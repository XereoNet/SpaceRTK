/*
 * This file is part of SpaceRTK (http://spacebukkit.xereo.net/).
 *
 * SpaceRTK is free software: you can redistribute it and/or modify it under the terms of the
 * Attribution-NonCommercial-ShareAlike Unported (CC BY-NC-SA) license as published by the Creative
 * Common organization, either version 3.0 of the license, or (at your option) any later version.
 *
 * SpaceRTK is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Attribution-NonCommercial-ShareAlike Unported (CC BY-NC-SA) license for more details.
 *
 * You should have received a copy of the Attribution-NonCommercial-ShareAlike Unported (CC BY-NC-SA)
 * license along with this program. If not, see <http://creativecommons.org/licenses/by-nc-sa/3.0/>.
 */
package me.neatmonster.spacertk.event;


import com.drdanick.rtoolkit.event.ToolkitEvent;

//import java.io.File;

/**
 * Defines an event that is fired when a backup either starts, or ends.
 */
public class BackupEvent extends ToolkitEvent {
    private boolean offlineBackup;
    //private boolean ignoreImmediateFiles;
    private String backupName;
    /*private File outputFile;
    private String[] ignoredFolders;
    private File[] files;*/
    private long startTime;
    private long endTime;

    /**
     * Creates a new BackupEvent
     * @param startTime Start time of the backup
     * @param endTime End time of the backup
     * @param offlineBackup If the backup was offline
     * @param backupName What the backup is called
     */
    public BackupEvent(long startTime, long endTime, boolean offlineBackup, /*boolean ignoreImmediateFiles, */String backupName/*,
            File outputFile, String[] ignoredFolders, File... folders*/) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.offlineBackup = offlineBackup;
        //this.ignoreImmediateFiles = ignoreImmediateFiles;
        this.backupName = backupName;
        //this.outputFile = outputFile;
        //this.ignoredFolders = new String[ignoredFolders.length];
        //this.files = new File[files.length];

        //System.arraycopy(ignoredFolders, 0, this.ignoredFolders, 0, ignoredFolders.length);
        //System.arraycopy(files, 0, this.files, 0, files.length);
    }

    /**
     * Check if immeidate regular files in the backup root should be ignored.
     * @return True if regular files in the backup directory
     *         root should be ignored, false otherwise.
     */
    //public boolean ignoreImmediateFiles() {
    //    return ignoreImmediateFiles;
    //}

    /**
     * Get the name of the backup.
     * @return the name of the backup.
     */
    public String getBackupName() {
        return backupName;
    }

    /**
     * Get the file the backup is being saved to.
     * @return The file the backup is being saved to.
     */
    //public File getOutputFile() {
    //    return outputFile;
    //}

    /**
     * Get the time the backup started.
     * @return the time the backup started or -1 if the backup hasn't yet started.
     */
    public long getStartTime() {
        return startTime;
    }

    /**
     * Get the time the backup ended.
     * @return the time the backup ended or -1 if the backup is still running.
     */
    public long getEndTime() {
        return endTime;
    }

    /**
     * Check if this is an offline backup.
     * @return true if the server should be held while the backup is being performed, false otherwise.
     */
    public boolean isOfflineBackup() {
        return offlineBackup;
    }

}
