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

/**
 * Defines an event that is fired when a backup either starts, or ends.
 */
public class BackupEvent extends ToolkitEvent {
    private boolean offlineBackup;
    private String backupName;
    private String uid;
    private long startTime;
    private long endTime;

    /**
     * Creates a new BackupEvent
     * @param startTime Start time of the backup
     * @param endTime End time of the backup
     * @param offlineBackup If the backup was offline
     * @param backupName What the backup is called
     */
    public BackupEvent(long startTime, long endTime, boolean offlineBackup, String backupName, String uid) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.offlineBackup = offlineBackup;
        this.backupName = backupName;
        this.uid = uid;
    }


    /**
     * Get the name of the backup.
     * @return the name of the backup.
     */
    public String getBackupName() {
        return backupName;
    }

    /**
     * Get the internal UID of the backup.
     * @return the UID of the backup.
     */
    public String getUid() {
        return uid;
    }

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
