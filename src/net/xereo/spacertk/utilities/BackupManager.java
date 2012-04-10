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
package net.xereo.spacertk.utilities;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.RoundingMode;
import java.net.URL;
import java.text.DecimalFormat;
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
    }

    /**
     * Attempt to perform a backup.
     * @param folders The folders to backup.
     * @param outputFile The file to save the backup to.
     * @param ignoreImmediateFiles True if regular files in the backup directory root should be ignored, false otherwise.
     * @return true if the backup was started, false otherwise.
     */
    public synchronized boolean performBackup(boolean ignoreImmediateFiles, File outputFile, File... folders) {
        if(bThread == null || !bThread.running) {
            bThread = new BackupThread(folders, outputFile, ignoreImmediateFiles);
            bThread.start();
            return true;
        }
        return false;
    }

    /**
     * Get the size of the current, or last backup in bytes.
     * @return the size of the last or current backup in bytes.
     */
    public long getBackupSize() {
        if(bThread == null)
            return 0l;
        return bThread.backupSize;
    }

    /**
     * Get the total number of bytes that have been backed up.
     * @return the total number of bytes that have been backed up.
     */
    public long getDataBackedUp() {
        if(bThread == null)
            return 0l;
        return bThread.dataBackedUp;
    }

    /**
     * Get the current folder being backed up.
     * @return the folder currently being backed up.
     */
    public String getCurrentFolder() {
        if(bThread == null)
            return null;

        return bThread.currentFolder;
    }

    /**
     * Get the current file being backed up.
     * @return the file currently being backed up.
     */
    public String getCurrentFile() {
        if(bThread == null)
            return null;

        return bThread.currentFile;
    }

    /**
     * Get the backup progress, from 0 to 100 percent.
     * @return The total backup progress.
     */
    public String getBackupProgress() {
        if(bThread == null || bThread.backupSize == 0)
            return "0.00";
        float progress = (float)bThread.dataBackedUp / (float)bThread.backupSize;

        return formatter.format(progress * 100f);
    }

    /**
     * Get the status of the last or current backup.
     * @return the status of the last or current backup.
     */
    public String getBackupStatus() {
        if(bThread == null)
            return null;
        return bThread.status;
    }

    /**
     * Get the last error reported by the current or last backup.
     * @return the last error reported by the current or last backup.
     */
    public String getLastError() {
        if(bThread == null)
            return null;
        return bThread.error;
    }

    /**
     * Check if a backup is in progress.
     * @return true if a backup is in progress, false otherwise.
     */
    public boolean isBackupRunning() {
        if(bThread == null)
            return false;
        return bThread.running;
    }

    private class BackupThread extends Thread {
        private File[] folders;
        private File outputFile;
        private boolean ignoreImmediateFiles;
        long backupSize = 0l;
        long dataBackedUp = 0l;
        String currentFolder;
        String currentFile;
        String status = "Initializing";
        String error;
        boolean running = true;

        BackupThread(File[] folders, File outputFile, boolean ignoreImmediateFiles) {
            this.folders = folders;
            this.outputFile = outputFile;
            this.ignoreImmediateFiles = ignoreImmediateFiles;
        }

        public void run() {
            status = "Calculating backup size";
            ZipOutputStream zip = null;
            backupSize = 0l;
            try {
                for(File folder : folders) {
                    File[] files = folder.listFiles();
                    if(!ignoreImmediateFiles) {
                        backupSize += calculateBackupSize(folder);
                    } else {
                        for(File f : files) {
                            if(f.isDirectory())
                                backupSize += calculateBackupSize(f);
                        }
                    }
                }

                zip = new ZipOutputStream(new FileOutputStream(outputFile));
                for(File folder : folders) { //Perform the backup
                    File[] files = folder.listFiles();
                    if(!ignoreImmediateFiles) {
                        addDirectoryToZipStream(folder.getName(), folder, zip);
                    } else {
                        for(File f : files) {
                            if(f.isDirectory()) addDirectoryToZipStream(f.getName(), f, zip);
                        }
                    }
                }

                status = "Done";
            } catch (IOException e) {
                e.printStackTrace();
                for(StackTraceElement el : e.getStackTrace())
                    error += el + "\n";

                status = "Error";
                return;
            } finally {
                try {
                    if(zip != null) zip.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                running = false;
            }
        }

        private void addDirectoryToZipStream(String entryPath, File dir, ZipOutputStream zip) throws IOException {
            File[] files = dir.listFiles();
            status = "Backing up " +entryPath + File.separator + dir.getName();
            currentFolder = entryPath + File.separator + dir.getName();
            for(File f : files) {
                if(f.isDirectory())
                    addDirectoryToZipStream(entryPath + File.separator + f.getName(), f, zip);
                else
                    addFileToZipStream(entryPath, f, zip);
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
                while ((len = in.read(buf)) > 0)
                    zip.write(buf, 0, len);
            } catch (IOException e) {
                throw e;
            } finally {
                in.close();
                dataBackedUp += getFileSize(file);
            }
        }

        /**
         * Calculate the size of a backup
         */
        private long calculateBackupSize(File folder) throws IOException {
            long size = 0l;
            File[] files = folder.listFiles();
            for(File f : files) {
                if(f.isDirectory())
                    size += calculateBackupSize(f);
                else
                    size += getFileSize(f);
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
                stream.close();
            }
        }

    }
}
