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
package me.neatmonster.spacertk;

import com.drdanick.rtoolkit.event.ToolkitEventListener;
import me.neatmonster.spacertk.event.BackupEvent;

/**
 * Listens for BackupEvents and responds to them appropriately.
 */
public class BackupListener implements ToolkitEventListener {

    public void onBackupEvent(BackupEvent e) {
        if(e.isOfflineBackup()) {
            if(e.getEndTime() == -1) {
                if(RemoteToolkit.isRunning()) {
                    RemoteToolkit.hold();
                    try {
                        Thread.sleep(10000); // Sleep for 10 seconds
                    } catch (InterruptedException ex) {}
                }
            } else {
                RemoteToolkit.unhold();
                e.setCanceled(true);
            }
        }
    }

}
