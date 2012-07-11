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
package me.neatmonster.spacertk.actions;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import me.hwei.mctool.MapAutoTrim;
import me.neatmonster.spacemodule.SpaceModule;
import me.neatmonster.spacemodule.api.Action;
import me.neatmonster.spacemodule.api.ActionHandler;
import me.neatmonster.spacertk.RemoteToolkit;
import me.neatmonster.spacertk.SpaceRTK;
import me.neatmonster.spacertk.utilities.backup.BackupManager;
import me.neatmonster.spacertk.utilities.Utilities;
import me.neatmonster.spacertk.utilities.WorldFileFilter;

import org.apache.commons.io.FileUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONValue;

/**
 * Actions handler for any Server-related actions
 */
public class ServerActions implements ActionHandler {

    /**
     * Backups a directory
     * @param name Name of the backup
     * @param directory Directory to backup
     * @param offlineBackup If the backup is offline
     * @return The UID of the operation
     */
    @Action(
            aliases = {"backup", "backupDirectory", "backupDir"})
    public String backup(String name, String directory, boolean offlineBackup) {
        BackupManager bManager = SpaceRTK.getInstance().getBackupManager();

        DateFormat dateFormat = new SimpleDateFormat("MM_dd_yyyy_HH_mm");
        Date date = new Date();
        File backupDir = new File(SpaceRTK.getInstance().worldContainer.getPath() + File.separator + SpaceRTK.getInstance().backupDirName);

        if(!backupDir.exists())
            backupDir.mkdirs();
        try {
            if(directory.equals("*")) {
                File oldDirectory = new File(System.getProperty("user.dir"));
                File zipFile = new File(backupDir + File.separator + name + "_" + dateFormat.format(date)+ ".zip");

                if (!SpaceRTK.getInstance().worldContainer.equals(new File("."))) {
                    return bManager.performBackup(offlineBackup, false, name, zipFile, new URI[]{backupDir.toURI()}, oldDirectory,SpaceRTK.getInstance().worldContainer);
                } else {
                    return bManager.performBackup(offlineBackup, false, name, zipFile, new URI[]{backupDir.toURI()}, oldDirectory);
                }

            } else {
                File oldDirectory = new File(SpaceRTK.getInstance().worldContainer.getPath() + File.separator + directory);
                String dirPath = oldDirectory.getCanonicalPath();

                if(!dirPath.startsWith(SpaceRTK.baseDir.getCanonicalPath()))
                    return null;

                File zipFile = new File(backupDir + File.separator + name + "_" + dateFormat.format(date)+ ".zip");

                return bManager.performBackup(offlineBackup, false, name, zipFile, new URI[]{backupDir.toURI()}, oldDirectory);
            }
        } catch(IOException e) {
            e.printStackTrace();
            return null;
        }

    }

    /**
     * Lists metadata of all backups.
     * @return A list of backup metadata.
     */
    @Action(
            aliases = {"getBackups", "listBackups", "listBackupInfo"})
    public List<List<String>> getBackups() {
        return SpaceRTK.getInstance().getBackupManager().listBackupInfo();
    }

    /**
     * Lists metadata of current and older operations.
     * @return A list of operation metadata.
     */
    @Action(
            aliases = {"getOperations", "listOperations", "listOperationInfo"})
    public List<List<String>> getOperations() {
        return SpaceRTK.getInstance().getBackupManager().listOperationInfo();
    }

    /**
     * Gets information about an operation
     * @return Information about an operation
     */
    @Action(
            aliases = {"getOperationInfo", "operationInfo"})
    public List<String> getOperationInfo(String uid) {
        BackupManager bManager = SpaceRTK.getInstance().getBackupManager();

        List<String> info = new ArrayList<String>(9);
        info.add(bManager.getOperationName(uid));
        info.add(bManager.getOperationFileName(uid));
        info.add(""+bManager.getOperationStartTime(uid));
        info.add(""+bManager.getOperationProgress(uid).replaceAll(",", "."));
        info.add(bManager.getOperationStatus(uid));
        info.add(bManager.getOperationFile(uid));
        info.add(""+bManager.getOperationDataCopied(uid));
        info.add(""+bManager.getOperationSize(uid));

        return info;
    }

    /**
     * Attempt to restore a backup.
     *
     * @param uid the UID of the backup to restore.
     * @param clearDest true if the destination should be wiped prior to restoring the backup, false otherwise.
     * @param offline true if the server should be held prior to performing the restore, false otherwise.
     */
    @Action(
            aliases = {"restore", "restoreBackup"})
    public void restore(String uid, boolean clearDest, boolean offline) {
        SpaceRTK.getInstance().getBackupManager().performRestore(offline, clearDest, uid, ".");
    }

    /**
     * Gets the last backup error
     * @return Last backup error
     */
    @Action(
            aliases = {"operationError", "getOperationError"})
    public String getOperationError(String uid) {
        return SpaceRTK.getInstance().getBackupManager().getError(uid);
    }

    /**
     * Checks if a backup is currently running
     * @return Backup is running
     */
    @Action(
            aliases = {"isOperationRunning", "operationRunning"})
    public boolean isOperationRunning(String uid) {
        return SpaceRTK.getInstance().getBackupManager().isOperationRunning(uid);
    }

    /**
     * Gets the UID of the currently running backup.
     * @return The UID of the currently running backup, null if none are running.
     */
    @Action(
            aliases = {"getRunningOperation", "runningOperation"})
    public String getRunningOperation() {
        return SpaceRTK.getInstance().getBackupManager().getOperationRunning();
    }

    /**
     * Get information about the currently running backup.
     * @return information about the currently running backup.
     */
    @Action(
            aliases = {"getRunningOperationInfo", "runningOperationInfo"})
    public List<String> getRunningOperationInfo() {
        BackupManager bManager = SpaceRTK.getInstance().getBackupManager();
        return getOperationInfo(bManager.getOperationRunning());
    }



    /**
     * Preforms a console command
     * @param command Command to preform
     * @return If successful
     */
    @Action(
            aliases = {"consoleCommand", "serverCommand", "command"})
    public boolean consoleCommand(final String command) {
        RemoteToolkit.consoleCommand(command);
        return true;
    }

    /**
     * Preforms a java command
     * @param command Command to preform
     * @return If successful
     */
    @Action(
            aliases = {"externalCommand"})
    public boolean externalCommand(final String command) {
        RemoteToolkit.externalCommand(command);
        return true;
    }

    /**
     * Forces the server to restart
     * @return If successful
     */
    @Action(
            aliases = {"forceRestart", "forceRestartServer"})
    public boolean forceRestart() {
        RemoteToolkit.forceRestart();
        return true;
    }

    /**
     * Forces the server to stop
     * @return If successful
     */
    @Action(
            aliases = {"forceStop", "forceStopServer"})
    public boolean forceStop() {
        RemoteToolkit.forceStop();
        return true;
    }

    /**
     * Gets all the worlds on the server
     * @return All worlds
     */
    @Action(
            aliases = {"getAllWorlds", "allWorlds"})
    public List<String> getAllWorlds() {
        return Arrays.asList(SpaceRTK.getInstance().worldContainer.list(WorldFileFilter.INSTANCE));
    }

    /**
     * Gets the SpaceModule version
     * @return SpaceModule version
     */
    @Action(
            aliases = {"getSpaceModuleVersion", "SpaceModuleVersion", "SpaceModule"})
    public String getSpaceModuleVersion() {
        try {
            SpaceModule.class.getMethod("getVersion");
            return SpaceModule.getInstance().getVersion();
        } catch (final Exception e) {
            System.out.println("This version of SpaceModule doesn't support this feature!");
            return "<unknown>";
        }
    }

    /**
     * Gets the SpaceRTK version
     * @return SpaceRTK version
     */
    @Action(
            aliases = {"getVersion", "version"})
    public String getSpaceRTKVersion() {
        try {
            SpaceModule.class.getMethod("getModuleVersion");
            return SpaceModule.getInstance().getModuleVersion();
        } catch (final Exception e) {
            System.out.println("This version of SpaceModule doesn't support this feature!");
            return "<unknown>";
        }
    }

    /**
     * Holds the server
     * @return If successful
     */
    @Action(
            aliases = {"hold", "stop", "stopServer"})
    public boolean hold() {
        RemoteToolkit.hold();
        return true;
    }

    /**
     * Schedules a restart at a date
     * @param date Date to restart
     * @return If successful
     */
    @Action(
            aliases = {"rescheduleRestart", "rescheduleServerRestart"})
    public boolean rescheduleRestart(final String date) {
        RemoteToolkit.rescheduleRestart(date);
        return true;
    }

    /**
     * Restarts the server
     * @param save If the server should be saved before restarting
     * @return If successful
     */
    @Action(
            aliases = {"restart", "restartServer"})
    public boolean restart(final Boolean save) {
        RemoteToolkit.restart(save);
        return true;
    }

    /**
     * Restarts the server if it's emtpy
     * @param save If the server should be saved before restarting
     * @return If successful
     */
    @Action(
            aliases = {"restartIfEmpty", "restartServerIfEmpty"})
    public boolean restartIfEmpty(final Boolean save) {
        final JSONArray players = (JSONArray) JSONValue.parse(Utilities.sendMethod("getPlayers", "[]"));
        if (players == null || players.size() == 0)
            RemoteToolkit.restart(save);
        return true;
    }

    /**
     * Rollover's the log
     * @return If successful
     */
    @Action(
            aliases = {"rollOverLog", "rollOver"})
    public boolean rollOver() {
        File oldLog = null;
        for (File file : new File("").listFiles()) {
            if (file.getName().contains("server") && file.getName().endsWith(".log")) {
               oldLog = file; 
            }
        }
        if (oldLog == null) {
            return false;
        }
        final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd_HH-mm");
        final File newLog = new File("server_" + format.format(new Date()) + ".log");
        try {
            if (!newLog.exists())
                newLog.createNewFile();
            FileUtils.writeStringToFile(newLog, FileUtils.readFileToString(oldLog));
            FileUtils.write(oldLog, "");
            
            if (SpaceRTK.getInstance().backupLogs) {
                FileUtils.copyFileToDirectory(oldLog, new File(SpaceRTK.getInstance().backupDirName));
            }
            oldLog.delete();
        } catch (final IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    /**
     * Runs MapTrimmer
     * @param worldName World to run MapTrimmer on
     * @param dilation Dilation to run MapTrimmer with
     * @param preservedBlocks Any preserved blocks for MapTrimmer
     * @return If successful
     */
    @Action(
            aliases = {"runMapTrimmer", "mapTrimmer"})
    public boolean runMapAutoTrim(final String worldName, final String dilation, final String preservedBlocks) {
        final boolean wasRunning = running();
        if (wasRunning)
            hold();
        while (running())
            try {
                Thread.sleep(1000);
            } catch (final InterruptedException e) {
                e.printStackTrace();
            };
        int size;
        if (dilation.equals("") && preservedBlocks.equals(""))
            size = 2;
        else if (preservedBlocks.equals(""))
            size = 4;
        else
            size = 6;
        final String[] matArgs = new String[size];
        matArgs[0] = "-w";
        matArgs[1] = worldName;
        if (size >= 4) {
            matArgs[2] = "-d";
            matArgs[3] = dilation;
        }
        if (size >= 6) {
            matArgs[4] = "-p";
            matArgs[5] = preservedBlocks;
        }
        MapAutoTrim.main(matArgs);
        if (wasRunning)
            unhold();
        return true;
    }

    /**
     * Gets if the server is running
     * @return Server is running
     */
    @Action(
            aliases = {"isRunning", "isServerRunning", "running"})
    public boolean running() {
        return RemoteToolkit.isRunning();
    }

    /**
     * Saves the server
     * @return If successful
     */
    @Action(
            aliases = {"save", "saveServer"})
    public boolean save() {
        RemoteToolkit.save();
        return true;
    }

    /**
     * Unholds the server
     * @return If successful
     */
    @Action(
            aliases = {"unhold", "start", "startServer"})
    public boolean unhold() {
        RemoteToolkit.unhold();
        return true;
    }
    
    /**
     * Gets the server's current Java version
     */
    @Action(aliases = {"getJavaVersion", "javaversion", "java"})
    public String getJavaVersion() {
        return System.getProperty("java.version");
    }
}
