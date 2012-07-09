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

import com.drdanick.McRKit.Wrapper;
import com.drdanick.rtoolkit.event.ToolkitEventListener;
import me.neatmonster.spacertk.event.BackupEvent;
import me.neatmonster.spacertk.utilities.backup.BackupManager;

import java.lang.reflect.Field;

/**
 * Listens for BackupEvents and responds to them appropriately.
 */
public class BackupListener implements ToolkitEventListener {

    /**
     * Called when the server backs up
     * @param e Event details
     */
    public void onBackupEvent(BackupEvent e) {
        if(e.isOfflineBackup()) {
            if(e.getEndTime() == -1) {
                if(RemoteToolkit.isRunning()) {
                    RemoteToolkit.hold();
                    try {
                        final Field process = Wrapper.getInstance().getClass().getDeclaredField("mcProcess");
                        process.setAccessible(true);
                        Process p = (Process)process.get(Wrapper.getInstance());
                        Thread.sleep(5000); // Sleep for 5 seconds

                        p.waitFor(); //Make sure the process has terminated
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    } catch (NoSuchFieldException ex) {
                        ex.printStackTrace();
                    } catch (IllegalAccessException ex) {
                        ex.printStackTrace();
                    }
                }
            } else if(BackupManager.getInstance().nextOperationIsOffline()){
                RemoteToolkit.unhold();
                try {
                    Thread.sleep(5000); //Give the toolkit 5 seconds to bring the server back up
                } catch(InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

}
