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
package me.neatmonster.spacertk.utilities.backup;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.math.RoundingMode;
import java.net.URI;
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
import de.schlichtherle.truezip.file.TFileReader;
import de.schlichtherle.truezip.file.TVFS;
import de.schlichtherle.truezip.fs.FsSyncException;
import me.neatmonster.spacemodule.SpaceModule;
import me.neatmonster.spacertk.SpaceRTK;
import me.neatmonster.spacertk.event.BackupEvent;

import com.drdanick.rtoolkit.EventDispatcher;
import com.drdanick.rtoolkit.event.ToolkitEventListener;
import com.drdanick.rtoolkit.event.ToolkitEventPriority;
import me.neatmonster.spacertk.utilities.Utilities;

/**
 * Manages backups.
 *
 * @author drdanick
 */
public class BackupManager {
    private static final long BACKUP_REFRESH_THRESHOLD = 60000; //1 minute
    static final char STDOUT_LINE_DELIMITER = 30;

    private static BackupManager instance;
    private Map<String, Backup> backups = new HashMap<String, Backup>();
    private long backupsLastLoaded = 0L;
    private Map<String, BackupThread> backupThreadRegistry = new HashMap<String, BackupThread>();
    private Queue<BackupThread> operationQueue = new LinkedBlockingQueue<BackupThread>();
    private BackupThread currentOperation;
    private DecimalFormat formatter;
    private Object lock;

    private BackupManager() {
        lock = this;
        loadBackups();

        formatter = new DecimalFormat("##0.00");
        formatter.setRoundingMode(RoundingMode.HALF_EVEN);
        EventDispatcher edt = SpaceModule.getInstance().getEdt();

        ToolkitEventListener backupListener = new ToolkitEventListener() {
            @SuppressWarnings("unused")
            public void onBackupEvent(BackupEvent e) {
                if(e.isCanceled())
                    return;
                synchronized(lock){
                    if(e.getEndTime() == -1 && !operationQueue.isEmpty() && operationQueue.peek().uid.equals(e.getUid())) {
                        currentOperation = operationQueue.remove();
                        currentOperation.start();
                    } else if(e.getEndTime() != -1) {
                        Backup lastBackup = currentOperation.backup;
                        if(lastBackup != null)
                            registerBackup(lastBackup);

                        if(hasOperationsQueued()) {
                            BackupThread next = operationQueue.peek();
                            BackupEvent b = new BackupEvent(next.startTime, next.endTime, next.offline, next.backupName, next.uid);
                            SpaceModule.getInstance().getEdt().fireToolkitEvent(b);
                        } else
                            currentOperation = null;
                    }
                }
            }
        };

        edt.registerListener(backupListener, SpaceModule.getInstance().getEventHandler(), ToolkitEventPriority.MONITOR, BackupEvent.class);
    }

    private void loadBackups() {
        TFile backupDir = new TFile(SpaceRTK.baseDir, SpaceRTK.getInstance().backupDirName);
        if(!backupDir.exists())
            return;

        for(TFile f : backupDir.listFiles()) {
            if(f.isArchive()) {
                Backup b = getBackup(f);
                registerBackup(b);

                try {
                    TVFS.umount(f);
                } catch (FsSyncException e) {
                    e.printStackTrace();
                }
            }
        }
        backupsLastLoaded = System.currentTimeMillis();
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
                read = fIn.readLine();
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
        }

        return new Backup(meta.get("uid"), meta.get("name"), Long.parseLong(meta.get("date")),
                Long.parseLong(meta.get("size")), f.toNonArchiveFile());
    }

    private void refreshBackups() {
        if(System.currentTimeMillis() > backupsLastLoaded + BACKUP_REFRESH_THRESHOLD)
            loadBackups();
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
            String backupName, File outputFile, URI[] ignoredFolders, File folder, File... folders) {
        String uid = null;
        List<URI> ignoreList = new LinkedList<URI>(Arrays.asList(ignoredFolders));

        if(ignoreImmediateFiles && folder.isDirectory())
            for(File f : folder.listFiles())
                if(!f.isDirectory())
                    ignoreList.add(f.toURI());

        do {
            uid = Utilities.generateRandomString(8, "US-ASCII");
        } while(backups.containsKey(uid));
        BackupThread bThread;

        try {
            if(offline)
                bThread = new IsolatedBackupThread(backupName, uid, SpaceRTK.baseDir,
                        SpaceRTK.baseDir.toURI(), ignoreList, false, outputFile, folder, folders);
            else
                bThread = new BackupThread(false, true, backupName, uid, SpaceRTK.baseDir,
                        SpaceRTK.baseDir.toURI(), ignoreList, false, offline, outputFile, folder, folders);

            backupThreadRegistry.put(bThread.uid, bThread);
            queueOperation(bThread);

            if(getOperationRunning() == null) {
                setCurrentOperation(bThread);
                BackupEvent e = new BackupEvent(-1, -1, offline, backupName, uid);
                SpaceModule.getInstance().getEdt().fireToolkitEvent(e);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return "null";
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
        BackupThread bThread = null;

        try {
            if(offline)
                bThread = new IsolatedBackupThread(backup.name, backup.uid, SpaceRTK.baseDir, SpaceRTK.baseDir.toURI(),
                        Arrays.asList(new URI[]{}), clearDest, dest, backup.backupFile);
            else
                bThread = new BackupThread(false, true, backup.name, backup.uid, SpaceRTK.baseDir, SpaceRTK.baseDir.toURI(),
                        Arrays.asList(new URI[]{}), clearDest, offline, dest, backup.backupFile);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        backupThreadRegistry.put(bThread.uid, bThread);
        queueOperation(bThread);

        if(getOperationRunning() == null) {
            setCurrentOperation(bThread);
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
    public synchronized List<List<String>> listBackupInfo() {
        refreshBackups();
        List<List<String>> backupList = new ArrayList<List<String>>(backups.size());

        for(Backup b : backups.values()) {
            String[] meta = new String[4];
            meta[0] = b.uid;
            meta[1] = b.name;
            meta[2] = ""+b.date;
            meta[3] = ""+b.size;

            backupList.add(Arrays.asList(meta));
        }
        if(!backupList.isEmpty())
            Collections.sort(backupList, new MetadataComparator());
        return backupList;
    }

    /**
     * Lists metadata of current and older operations.
     * @return A list of operation metadata.
     */
    public synchronized List<List<String>> listOperationInfo() {
        List<List<String>> operationList = new ArrayList<List<String>>(backups.size());

        for(BackupThread b : backupThreadRegistry.values()) {
            String[] meta = new String[4];
            meta[0] = b.uid;
            meta[1] = b.backupName;
            meta[2] = ""+b.startTime;
            meta[3] = ""+b.dataSize;

            operationList.add(Arrays.asList(meta));
        }
        if(!operationList.isEmpty())
            Collections.sort(operationList, new MetadataComparator());
        return operationList;
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

        return bThread.getDestRoot().getName();
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

    public synchronized boolean nextOperationIsOffline() {
        if(!operationQueue.isEmpty())
            return operationQueue.peek().offline;
        return false;
    }

    private synchronized void queueOperation(BackupThread bThread) {
        operationQueue.add(bThread);
    }

    private synchronized void setCurrentOperation(BackupThread operation) {
        this.currentOperation = operation;
    }



    /**
     * Represents a single backup archive
     */
    static class Backup {
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

    private class MetadataComparator implements Comparator<List<String>> {

        @Override
        public int compare(List<String> o1, List<String> o2) {
            try {
                if(Long.parseLong(o1.get(2)) == -1L) //Handle operations that have not been started.
                    return 1;
                else if(Long.parseLong(o2.get(2)) == -1L)
                    return -1;

                long l = Long.parseLong(o1.get(2)) - Long.parseLong(o2.get(2));
                //o1 - o2 is not sufficient here due to possible errors from casting down
                if(l < 0)
                    return -1;
                else if(l > 0)
                    return 1;

                return 0;
            } catch(NumberFormatException e) {
                e.printStackTrace();
                return 0;
            }
        }
    }
}
