/*
* This file is part of SpaceRTK (http://spacebukkit.xereo.net/).
*
* SpaceRTK is free software: you can redistribute it and/or modify it under the terms of the
* Attribution-NonCommercial-ShareAlike Unported (CC BY-NC-SA) license as published by the Creative Common organization,
* either version 3.0 of the license, or (at your option) any later version.
*
* SpaceRTK is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
* warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the Attribution-NonCommercial-ShareAlike
* Unported (CC BY-NC-SA) license for more details.
*
* You should have received a copy of the Attribution-NonCommercial-ShareAlike Unported (CC BY-NC-SA) license along with
* this program. If not, see <http://creativecommons.org/licenses/by-nc-sa/3.0/>.
*/
package me.neatmonster.spacertk.utilities;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import de.schlichtherle.truezip.file.TFile;
import de.schlichtherle.truezip.file.TFileInputStream;
import de.schlichtherle.truezip.file.TFileOutputStream;
import de.schlichtherle.truezip.file.TFileReader;
import de.schlichtherle.truezip.file.TVFS;
import de.schlichtherle.truezip.fs.FsSyncException;
import de.schlichtherle.truezip.fs.FsSyncOption;
import de.schlichtherle.truezip.fs.FsSyncOptions;
import me.neatmonster.spacemodule.SpaceModule;
import me.neatmonster.spacertk.SpaceRTK;
import me.neatmonster.spacertk.event.BackupEvent;

import com.drdanick.rtoolkit.EventDispatcher;
import com.drdanick.rtoolkit.event.ToolkitEventListener;
import com.drdanick.rtoolkit.event.ToolkitEventPriority;

/**
* Manages backups.
*
* @author drdanick
*/
public class BackupManager {

    private static BackupManager instance;
    private Map<String, Backup> backups;
    private long backupsLastLoaded;
    private static final long BACKUP_REFRESH_THRESHOLD = 60000; //1 minute
    private Map<String, BackupThread> backupThreadRegistry = new HashMap<String, BackupThread>();
    private Queue<BackupThread> operationQueue = new LinkedBlockingQueue<BackupThread>();
    private BackupThread currentOperation;
    private DecimalFormat formatter;

    private BackupManager() {
        backups = loadBackups();
        formatter = new DecimalFormat("##0.00");
        formatter.setRoundingMode(RoundingMode.HALF_EVEN);
        EventDispatcher edt = SpaceModule.getInstance().getEdt();

        ToolkitEventListener backupListener = new ToolkitEventListener() {
            @SuppressWarnings("unused")
            public void onBackupEvent(BackupEvent e) {
                if(e.isCanceled())
                    return;
                if(e.getEndTime() == -1 && operationQueue.peek().uid.equals(e.getUid())) {
                    currentOperation = operationQueue.remove();
                    currentOperation.start();
                } else if(e.getEndTime() != -1) {
                    if(hasOperationsQueued()) {
                        BackupThread next = operationQueue.peek();
                        BackupEvent b = new BackupEvent(next.startTime, next.endTime, next.offline, next.backupName, next.uid);
                        SpaceModule.getInstance().getEdt().fireToolkitEvent(b);
                    } else
                        currentOperation = null;
                }
            }
        };

        edt.registerListener(backupListener, SpaceModule.getInstance().getEventHandler(), ToolkitEventPriority.MONITOR, BackupEvent.class);
    }

    private Map<String, Backup> loadBackups() {
        Map<String, Backup> backups = new HashMap<String, Backup>();
        TFile backupDir = new TFile(SpaceRTK.baseDir, SpaceRTK.getInstance().backupDirName);
        if(!backupDir.exists())
            return backups;

        for(TFile f : backupDir.listFiles()) {
            if(f.isArchive()) {
                Backup b = getBackup(f);
                registerBackup(b);
            }
        }
        backupsLastLoaded = System.currentTimeMillis();

        return backups;
    }

    private synchronized void registerBackup(Backup b) {
        if(b != null)
            backups.put(b.uid, b);
        //TODO: print a warning if the backup is null
    }

    /**
     * Returns a Backup representation of an archive file.
     */
    private Backup getBackup(TFile f) {
        Map<String, String> meta = new HashMap<String, String>();
        TFile metaFile = new TFile(f,"backup.info");

        if(!metaFile.exists())
            return null;

        BufferedReader fIn = null;
        try {
            fIn = new BufferedReader(new TFileReader(metaFile));
            String read = fIn.readLine();

            while(read != null) {
                String[] split = read.split(":");
                meta.put(split[0], split[1]);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                if(fIn != null)
                    fIn.close();
            } catch(IOException e) {
                e.printStackTrace();
                return null;
            }
            //TODO: unmount the TFile correctly.
        }

        return new Backup(meta.get("uid"), meta.get("name"), Long.parseLong(meta.get("date")),
                Long.parseLong(meta.get("size")), f.toNonArchiveFile());
    }

    private void refreshBackups() {
        if(System.currentTimeMillis() > backupsLastLoaded + BACKUP_REFRESH_THRESHOLD)
            backups = loadBackups();
    }

    public static BackupManager getInstance() {
        if(instance == null)
            instance = new BackupManager();
        return instance;
    }

    /**
     * Attempt to queue a backup operation.
     *
     * @param offline true if the server should be held prior to performing the backup, false otherwise.
     * @param ignoreImmediateFiles True if regular files in the backup directory
     * root should be ignored, false otherwise.
     * @param backupName the name of the backup.
     * @param outputFile The file or folder to save the backup to.
     * @param ignoredFolders Folders to ignore given by their paths relative to the base directory.
     * @param folder The folder to backup.
     * @param folders Additional folders to backup.
     * @return the uid of the backup.
     */
    public synchronized String performBackup(boolean offline, boolean ignoreImmediateFiles,
            String backupName, File outputFile, String[] ignoredFolders, File folder, File... folders) {
        String uid = null;
        LinkedList<String> ignoreList = new LinkedList<String>(Arrays.asList(ignoredFolders));

        if(ignoreImmediateFiles && folder.isDirectory())
            for(File f : folder.listFiles())
                if(!f.isDirectory())
                    ignoreList.add(SpaceRTK.baseDir.toURI().relativize(f.toURI()).getPath());

        do {
            uid = Utilities.generateRandomString(8,"US-ASCII");
        } while(backups.containsKey(uid));

        BackupThread bThread = new BackupThread(backupName, uid, SpaceRTK.baseDir,
                ignoreList, false, offline, outputFile, folder, folders);

        backupThreadRegistry.put(bThread.uid, bThread);
        queueOperation(bThread);

        if(currentOperation == null) {
            BackupEvent e = new BackupEvent(-1, -1, offline, backupName, uid);
            SpaceModule.getInstance().getEdt().fireToolkitEvent(e);
        }

        return uid;
    }

    /**
     * Attempt to queue a restore operation.
     *
     * @param offline true if the server should be held prior to performing the restore, false otherwise.
     * @param clearDest true if the destination should be wiped prior to restoring the backup, false otherwise.
     * @param uid the UID of the backup to restore.
     * @param path the path of the folder to restore to relative to the base directory.
     * @returns true if the backup was queued, false otherwise.
     */
    public synchronized boolean performRestore(boolean offline, boolean clearDest, String uid, String path) {
        File dest = new File(SpaceRTK.baseDir, path);
        Backup backup = backups.get(uid);
        if(backup == null)
            return false;

        BackupThread bThread = new BackupThread(backup.name, backup.uid, SpaceRTK.baseDir, Arrays.asList(new String[]{}),
                clearDest, offline, dest, backup.backupFile);

        backupThreadRegistry.put(bThread.uid, bThread);
        queueOperation(bThread);

        if(currentOperation == null) {
            BackupEvent e = new BackupEvent(-1, -1, offline, backup.name, uid);
            SpaceModule.getInstance().getEdt().fireToolkitEvent(e);
        }

        return true;
    }

    /**
     * Retrieve a list of backup UIDs.
     * @return a list of loaded backup UIDs.
     */
    public synchronized List<String> listBackupIds() {
        return new ArrayList<String>(backups.keySet());
    }

    /**
     * List metadata of all backups.
     * @return a list of each backup's metadata.
     */
    public synchronized List<String[]> listBackupInfo() {
        refreshBackups();
        List<String[]> backupList = new ArrayList<String[]>(backups.size());

        for(Backup b : backups.values()) {
            String[] meta = new String[4];
            meta[0] = b.uid;
            meta[1] = b.name;
            meta[2] = ""+b.date;
            meta[3] = ""+b.size;
        }
        Collections.sort(backupList, new MetadataComparator());
        return backupList;
    }

    /**
     * Retrieve the metadata of a single backup given its UID.
     * @param uid the UID of the backup.
     * @returns an array of metadata entries.
     */
    public synchronized String[] getBackupInfo(String uid) {
        Backup b = backups.get(uid);
        String[] meta = new String[4];
        meta[0] = b.uid;
        meta[1] = b.name;
        meta[2] = ""+b.date;
        meta[3] = ""+b.size;

        return meta;
    }

    /**
     * Get the name of a previous or current backup/restore operation given its UID.
     * @param uid the UID of the backup/restore operation.
     * @return the name of the specified operation, null if it does not exist.
     */
    public synchronized String getOperationName(String uid) {
        BackupThread bThread = backupThreadRegistry.get(uid);

        if(bThread == null) {
            Backup b = backups.get(uid);

            if(b == null)
                return null;

            return b.name;
        }

        return bThread.backupName;
    }

    /**
     * Get the name of the file a backup was saved to, or the folder a restore was made to.
     * @param uid the UID of the backup/restore operation.
     * @return the destination of a backup/restore operation.
     */
    public synchronized String getOperationFileName(String uid) {
        BackupThread bThread = backupThreadRegistry.get(uid);

        if(bThread == null) {
            Backup b = backups.get(uid);

            if(b == null)
                return null;

            return b.backupFile.getName();
        }

        return bThread.destRoot.getName();
    }

    /**
     * Get the time (in milliseconds) a backup/restore was started.
     * @param uid the UID of the backup/restore operation.
     * @return The time (in milliseconds) a backup/restore was started, -1 if the operation hasn't yet been started.
     */
    public synchronized long getOperationStartTime(String uid) {
        BackupThread bThread = backupThreadRegistry.get(uid);

        if(bThread == null) {
            Backup b = backups.get(uid);

            if(b == null)
                return 0L;

            return b.date;
        }

        return bThread.startTime;
    }

    /**
     * Get the size of a backup/restore operation, measured in bytes.
     * @param uid the UID of the backup/restore operation.
     * @return the size of the last or current backup in bytes, -1 if the operation hasn't yet been started.
     */
    public synchronized long getOperationSize(String uid) {
        BackupThread bThread = backupThreadRegistry.get(uid);

        if (bThread == null) {
            Backup b = backups.get(uid);

            if(b == null)
                return 0l;

            return b.size;
        }

        return bThread.dataSize;
    }

    /**
     * Get the total number of bytes that have been copied in an operation.
     * @param uid the UID of the operation.
     * @return the total number of bytes that have been copied mesured in bytes.
     */
    public synchronized long getOperationDataCopied(String uid) {
        BackupThread bThread = backupThreadRegistry.get(uid);

        if (bThread == null)
            return 0l;

        return bThread.dataCopied;
    }

    /**
     * Get the current file being copied in a backup/restore operation.
     * @param uid the UID of the operation.
     * @return the file currently being backed up.
     */
    public synchronized String getOperationFile(String uid) {
        BackupThread bThread = backupThreadRegistry.get(uid);

        if (bThread == null) {
            return null;
        }

        return bThread.currentFile;
    }

    /**
     * Get the progress of a backup/restore operation, from 0 to 100 percent.
     * @param uid the UID of the operation.
     * @return The progress of the operation.
     */
    public synchronized String getOperationProgress(String uid) {
        BackupThread bThread = backupThreadRegistry.get(uid);

        if (bThread == null || bThread.dataSize == 0)
            return "0.00";

        float progress = bThread.progress;

        return formatter.format(progress * 100f);
    }

    /**
     * Get the status of a backup/restore operation.
     * @param uid the UID of the operation.
     * @return the status of a backup/restore operation.
     */
    public synchronized String getOperationStatus(String uid) {
        BackupThread bThread = backupThreadRegistry.get(uid);

        if (bThread == null)
            return null;

        return bThread.status;
    }

    /**
     * Get the error (if any) reported by a backup/restore operation.
     * @param uid the UID of the operation.
     * @return the error reported by a backup/restore operation.
     */
    public synchronized String getError(String uid) {
        BackupThread bThread = backupThreadRegistry.get(uid);

        if (bThread == null)
            return null;

        return bThread.error;
    }

    /**
     * Check if an operation is running given its uid.
     * @param uid the UID of the operation.
     * @return true if the operation is in progress, false otherwise.
     */
    public synchronized boolean isOperationRunning(String uid) {
        BackupThread bThread = backupThreadRegistry.get(uid);

        if (bThread == null)
            return false;

        return bThread.running;
    }

    /**
     * Get the UID of a currently running operation.
     * @returns the UID of the currently running operation, or null if there is no operation.
     */
    public synchronized String getOperationRunning() {
        if(currentOperation == null)
            return null;
        return currentOperation.uid;
    }

    public synchronized boolean hasOperationsQueued() {
        return !operationQueue.isEmpty();
    }

    private synchronized void queueOperation(BackupThread bThread) {
        operationQueue.add(bThread);
    }

    private class BackupThread extends Thread {
        private FSTree sourceTree;
        private File[] additionalSources;
        private File sourceRoot;
        private File destRoot;
        private File base;
        private boolean clearDst;
        private List<String> ignoreList;
        boolean offline;
        long startTime = -1L;
        long endTime = -1L;
        String status = "Idle";
        String error = "";
        String backupName;
        String uid;
        String currentFile;
        long dataSize = 0L;
        long dataCopied = 0L;
        float progress = 0.0f;
        boolean running = false;

        public BackupThread(String backupName, String uid, File base, List<String> ignoreList,
                boolean clearDst, boolean offline, File destRoot, File sourceRoot, File... additionalSources) {
            this.base = base;
            this.backupName = backupName;
            this.uid = uid;
            this.clearDst = clearDst;
            this.ignoreList = ignoreList;
            this.offline = offline;
            this.destRoot = destRoot;
            this.sourceRoot = sourceRoot;
            this.additionalSources = additionalSources;
        }

        public void run() {
            running  = true;
            startTime = System.currentTimeMillis();
            status = "Initializing";
            ObjectInputStream oi = null;
            ObjectOutputStream oo = null;
            PrintWriter fOut = null;
            try {
                TFile sourceFile = new TFile(sourceRoot);
                TFile backupIndex = new TFile(sourceRoot, "backup.index");
                TFile backupMeta = new TFile(sourceRoot, "backup.info");

                //Initialise the file index.
                if(backupIndex.exists()) {
                    oi = new ObjectInputStream(new TFileInputStream(backupIndex));
                    sourceTree = (FSTree)oi.readObject();
                    base = sourceRoot;
                } else {
                    sourceTree = Utilities.buildFSTree(new TFile(sourceRoot), base);
                    if(additionalSources != null) {
                        for(File f : additionalSources) {
                            FSTree tree = Utilities.buildFSTree(new TFile(f), base);
                            sourceTree.merge(tree);
                        }
                    }
                }

                //Remove ignored files from the index
                for(String s : ignoreList)
                    sourceTree.remove(s);
                sourceTree.remove(base.getName()+"/"+"backup.index");
                sourceTree.remove(base.getName()+"/"+"backup.info");

                if(sourceFile.isArchive())
                    dataSize = Utilities.getFileSize(sourceFile.toNonArchiveFile());
                else
                    dataSize = sourceTree.getSize();

                if(clearDst) {
                    status = "Wiping out destination directories";
                    if(new TFile(destRoot).isArchive())
                        destRoot.delete();
                    else {
                        for(String s : sourceTree) {
                            File file = new File(destRoot, s);
                            if(file.isDirectory())
                                for(File f : file.listFiles())
                                    if(!f.isDirectory())
                                        f.delete();
                        }
                    }
                }

                if(!backupIndex.exists()) {
                    backupIndex = new TFile(destRoot, "backup.index");
                    if(!destRoot.exists())
                        new TFile(destRoot).mkdirs();
                    oo = new ObjectOutputStream(new TFileOutputStream(backupIndex));
                    oo.writeObject(sourceTree);
                }

                if(!backupMeta.exists()) {
                    backupMeta = new TFile(destRoot, "backup.info");
                    if(!destRoot.exists())
                        new TFile(destRoot).mkdirs();
                    fOut = new PrintWriter(new TFileOutputStream(backupMeta));
                    fOut.println("name:"+backupName);
                    fOut.println("uid:"+ uid);
                    fOut.println("created:"+startTime);
                    fOut.println("size:"+dataSize);

                    Backup backup = new Backup(uid, backupName, startTime, dataSize, destRoot);
                    registerBackup(backup);
                }

                for(String path : sourceTree) {
                    TFile src = new TFile(base, path);
                    TFile dst = new TFile(destRoot, path);
                    status = "Processing "+src.getName();
                    currentFile = src.getName();
                    if(!dst.isDirectory())
                        dst.getParentFile().mkdirs();
                    else
                        dst.mkdirs();

                    try {
                        if(!src.isArchive())
                            src.cp_p(dst);
                        else
                            src.toNonArchiveFile().cp_p(dst.toNonArchiveFile());
                    } catch(IOException e) {
                        e.printStackTrace();
                    }

                    if(sourceFile.isArchive())
                        dataCopied += src.length();
                    else
                        dataCopied += Utilities.getFileSize(src);

                    progress = dataCopied / (dataSize + (0.15f * dataSize));
                }

                progress = 0.85f; //Temporary

            } catch (IOException e) {
                e.printStackTrace();
                for (StackTraceElement el : e.getStackTrace()) {
                    error += el + "\n";
                }
                status = "Error";
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
                for (StackTraceElement el : e.getStackTrace()) {
                    error += el + "\n";
                }
                status = "Error";
            } finally {
                try {
                    if(oi != null)
                        oi.close();
                    if(oo != null)
                        oo.close();
                    if(fOut != null)
                        fOut.close();
                } catch(IOException e){
                    for (StackTraceElement el : e.getStackTrace()) {
                        error += el + "\n";
                    }
                    status = "Error";
                }

                if(error.isEmpty())
                    status = "Finalizing";
                try {
                    TVFS.umount(new TFile(sourceRoot));
                } catch(FsSyncException e){
                    e.printStackTrace();
                }

                try {
                    TVFS.umount(new TFile(destRoot));
                } catch(FsSyncException e){
                    e.printStackTrace();
                }
                if(error.isEmpty())
                    status = "Done";

                endTime = System.currentTimeMillis();
                running = false;
                progress = 1.0f;

                BackupEvent e = new BackupEvent(startTime, endTime, offline, backupName, uid);
                SpaceModule.getInstance().getEdt().fireToolkitEvent(e);
            }
        }
    }

    /**
     * Represents a single backup archive
     */
    private class Backup {
        String name;
        String uid;
        long date;
        long size;
        File backupFile;

        public Backup(String uid, String name, long date, long size, File backupFile) {
            this.uid = uid;
            this.name = name;
            this.date = date;
            this.size = size;
            this.backupFile = backupFile;
        }
    }

    private class MetadataComparator implements Comparator<String[]> {

        @Override
        public int compare(String[] o1, String[] o2) {
            try {
                return Integer.parseInt(o1[2]) - Integer.parseInt(o2[2]);
            } catch(NumberFormatException e) {
                e.printStackTrace();
                return 0;
            }
        }
    }
}
