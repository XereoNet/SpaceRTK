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

import com.drdanick.rtoolkit.EventDispatcher;
import com.drdanick.rtoolkit.event.ToolkitEventListener;
import com.drdanick.rtoolkit.event.ToolkitEventPriority;
import me.neatmonster.spacemodule.SpaceModule;
import me.neatmonster.spacertk.SpaceRTK;
import me.neatmonster.spacertk.event.BackupEvent;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.RoundingMode;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Manage backup threads
 *
 * @author drdanick
 */
public class BackupManager {

    private BackupThread bThread;
    private DecimalFormat formatter;

    public BackupManager() {
        formatter = new DecimalFormat("##0.00");
        formatter.setRoundingMode(RoundingMode.HALF_EVEN);
        EventDispatcher edt = SpaceModule.getInstance().getEdt();

        ToolkitEventListener backupListener = new ToolkitEventListener() {
            public void onBackupEvent(BackupEvent e) {
                System.out.println("DEBUG: Got backup event..."); //DEBUG
                if(!e.isCanceled() && e.getBackupName().equals(bThread.backupName))
                    bThread.start();
            }
        };

        edt.registerListener(backupListener, SpaceModule.getInstance().getEventHandler(),ToolkitEventPriority.MONITOR, BackupEvent.class);
    }

    /**
     * Attempt to perform a backup.
     *
     * @param ignoreImmediateFiles True if regular files in the backup directory
     * root should be ignored, false otherwise.
     * @param backupName the name of the backup.
     * @param outputFile The file to save the backup to.
     * @param ignoredFolders Folders to ignore in the root of the folder(s) being
     * backed up.
     * @param folders The folders to backup.
     * @return true if the backup was started, false otherwise.
     */
    public synchronized boolean performBackup(boolean offlineBackup, boolean ignoreImmediateFiles, String backupName, File outputFile, String[] ignoredFolders, File... folders) {
        if (bThread == null || !bThread.running) {
            bThread = new BackupThread(folders, ignoredFolders, outputFile, backupName, ignoreImmediateFiles, offlineBackup);
            BackupEvent e = new BackupEvent(-1, -1, offlineBackup, backupName);
            SpaceModule.getInstance().getEdt().fireToolkitEvent(e);
            return true;
        }
        return false;
    }

    /**
     * Get the name of the current or previous backup.
     * @return the name of the current or previous backup.
     */
    public String getBackupName() {
        if(bThread == null)
            return null;

        return bThread.backupName;
    }

    /**
     * Get the name of the file the last backup was saved to.
     * @return the name of the file the last backup was saved to.
     */
    public String getBackupFileName() {
        if(bThread == null)
            return null;

        return bThread.outputFile.getName();
    }

    /**
     * Get the time (in milliseconds) the last backup was started.
     * @return The time (in milliseconds) the last backup was started.
     */
    public long getStartTime() {
        if(bThread == null)
            return 0l;

        return bThread.startTime;
    }

    /**
     * Get the size of the current, or last backup in bytes.
     *
     * @return the size of the last or current backup in bytes.
     */
    public long getBackupSize() {
        if (bThread == null)
            return 0l;

        return bThread.backupSize;
    }

    /**
     * Get the total number of bytes that have been backed up.
     *
     * @return the total number of bytes that have been backed up.
     */
    public long getDataBackedUp() {
        if (bThread == null)
            return 0l;

        return bThread.dataBackedUp;
    }

    /**
     * Get the current folder being backed up.
     *
     * @return the folder currently being backed up.
     */
    public String getCurrentFolder() {
        if (bThread == null)
            return null;

        return bThread.currentFolder;
    }

    /**
     * Get the current file being backed up.
     *
     * @return the file currently being backed up.
     */
    public String getCurrentFile() {
        if (bThread == null) {
            return null;
        }

        return bThread.currentFile;
    }

    /**
     * Get the backup progress, from 0 to 100 percent.
     *
     * @return The total backup progress.
     */
    public String getBackupProgress() {
        if (bThread == null || bThread.backupSize == 0)
            return "0.00";

        float progress = (float) bThread.dataBackedUp / (float) bThread.backupSize;

        return formatter.format(progress * 100f);
    }

    /**
     * Get the status of the last or current backup.
     *
     * @return the status of the last or current backup.
     */
    public String getBackupStatus() {
        if (bThread == null)
            return null;

        return bThread.status;
    }

    /**
     * Get the last error reported by the current or last backup.
     *
     * @return the last error reported by the current or last backup.
     */
    public String getLastError() {
        if (bThread == null)
            return null;

        return bThread.error;
    }

    /**
     * Check if a backup is in progress.
     *
     * @return true if a backup is in progress, false otherwise.
     */
    public boolean isBackupRunning() {
        if (bThread == null)
            return false;

        return bThread.running;
    }

    private class BackupThread extends Thread {

        private File[] folders;
        private List<String> ignoredFolders;
        private boolean ignoreImmediateFiles;
        private boolean offlineBackup;
        File outputFile;
        long startTime;
        long backupSize = 0l;
        long dataBackedUp = 0l;
        String backupName;
        String currentFolder;
        String currentFile;
        String status = "Initializing";
        String error;
        boolean running = true;

        BackupThread(File[] folders, String[] ignoredFolders, File outputFile, String backupName, boolean ignoreImmediateFiles, boolean offlineBackup) {
            this.folders = folders;
            this.ignoredFolders = Arrays.asList(ignoredFolders);
            this.outputFile = outputFile;
            this.backupName = backupName;
            this.ignoreImmediateFiles = ignoreImmediateFiles;
            this.offlineBackup = offlineBackup;
        }

        public void run() {
            System.out.println("Starting backup..."); //DEBUG
            status = "Calculating backup size";
            startTime = System.currentTimeMillis();
            ZipOutputStream zip = null;
            backupSize = 0l;
            try {
                for (File folder : folders) {
                    File[] files = folder.listFiles();
                    if (!ignoreImmediateFiles) {
                        backupSize += calculateBackupSize(folder, ignoredFolders);
                    } else {
                        for (File f : files) {
                            if (f.isDirectory()) {
                                if (ignoredFolders != null && ignoredFolders.contains(f.getCanonicalPath()))
                                    continue; //Do not count the folder if it is in the ignore list
                                backupSize += calculateBackupSize(f, ignoredFolders);
                            }
                        }
                    }
                }

                zip = new ZipOutputStream(new FileOutputStream(outputFile));
                for (File folder : folders) { //Perform the backup
                    File[] files = folder.listFiles();
                    if (!ignoreImmediateFiles) {
                        addDirectoryToZipStream(folder.getName(), folder, ignoredFolders, zip);
                    } else {
                        for (File f : files) {
                            if (f.isDirectory()) {
                                if (ignoredFolders != null && ignoredFolders.contains(f.getCanonicalPath()))
                                    continue; //Do not back up the folder if it is in the ignore list
                                addDirectoryToZipStream(f.getName(), f, null, zip);
                            }
                        }
                    }
                }

                status = "Done";
            } catch (IOException e) {
                e.printStackTrace();
                for (StackTraceElement el : e.getStackTrace()) {
                    error += el + "\n";
                }
                status = "Error";

            } finally {
                try {
                    if (zip != null) {
                        zip.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                BackupEvent e = new BackupEvent(startTime, System.currentTimeMillis(), offlineBackup, backupName);
                SpaceModule.getInstance().getEdt().fireToolkitEvent(e);
                running = false;
            }
        }

        private void addDirectoryToZipStream(String entryPath, File dir, List<String> ignoredFolders, ZipOutputStream zip) throws IOException {
            File[] files = dir.listFiles();
            status = "Backing up " + entryPath;
            currentFolder = entryPath;
            for (File f : files) {
                if (f.isDirectory()) {
                    if (ignoredFolders != null && ignoredFolders.contains(f.getCanonicalPath())) {
                        continue; //Do not back up the folder if it is in the ignore list
                    }
                    addDirectoryToZipStream(entryPath + File.separator + f.getName(), f, null, zip);
                } else {
                    addFileToZipStream(entryPath, f, zip);
                }
            }
        }

        private void addFileToZipStream(String entryPath, File file, ZipOutputStream zip) throws IOException {
            final byte[] buf = new byte[1024];
            int len;
            FileInputStream in = null;
            currentFile = file.getName();
            try {
                in = new FileInputStream(file);
                zip.putNextEntry(new ZipEntry(entryPath + File.separator + file.getName()));
                while ((len = in.read(buf)) > 0) {
                    zip.write(buf, 0, len);
                }
            } catch (IOException e) {
                throw e;
            } finally {
                if(in != null)
                    in.close();
                dataBackedUp += getFileSize(file);
            }
        }

        /**
         * Calculate the size of a backup
         */
        private long calculateBackupSize(File folder, List<String> ignoredFolders) throws IOException {
            long size = 0l;
            File[] files = folder.listFiles();
            for (File f : files) {
                if (f.isDirectory()) {
                    if (ignoredFolders != null && ignoredFolders.contains(f.getCanonicalPath())) {
                        continue; //Do not back up the folder if it is in the ignore list
                    }
                    size += calculateBackupSize(f, null);
                } else {
                    size += getFileSize(f);
                }
            }

            return size;
        }

        private long getFileSize(File f) throws IOException {
            InputStream stream = null;
            try {
                URL url = f.toURI().toURL();
                stream = url.openStream();

                return stream.available();
            } finally {
                if(stream != null)
                    stream.close();
            }
        }
    }
}
