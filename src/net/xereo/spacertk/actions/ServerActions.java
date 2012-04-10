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
package net.xereo.spacertk.actions;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

import me.hwei.mctool.MapAutoTrim;
import net.xereo.spacemodule.SpaceModule;
import net.xereo.spacemodule.api.Action;
import net.xereo.spacertk.RemoteToolkit;
import net.xereo.spacertk.SpaceRTK;
import net.xereo.spacertk.utilities.BackupManager;
import net.xereo.spacertk.utilities.Utilities;
import net.xereo.spacertk.utilities.WorldFileFilter;

import net.xereo.spacertk.utilities.ZIP;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.json.simple.JSONArray;
import org.json.simple.JSONValue;

import chunkster.Chunkster;

public class ServerActions {

    @Action(
            aliases = {"backup", "backupDirectory", "backupDir"})
    public boolean backup(final String directory) {
        BackupManager bManager = SpaceRTK.getInstance().getBackupManager();
        if(bManager.isBackupRunning())
            return false;

        DateFormat dateFormat = new SimpleDateFormat("MM_dd_yyyy_HH_mm");
        Date date = new Date();
        File backupDir = new File(SpaceRTK.getInstance().worldContainer.getPath() + File.separator + "backups");
        if(!backupDir.exists())
            backupDir.mkdirs();

        if(directory.equals("*")) {
            File oldDirectory = new File(System.getProperty("user.dir"));
            File zipFile = new File(backupDir + File.separator + "full_" + dateFormat.format(date)+ ".zip");

            if (!SpaceRTK.getInstance().worldContainer.equals(".")) {
                return bManager.performBackup(true, zipFile, oldDirectory, SpaceRTK.getInstance().worldContainer);
            } else {
                return bManager.performBackup(true, zipFile, oldDirectory);
            }

        } else {
            File oldDirectory = new File(SpaceRTK.getInstance().worldContainer.getPath() + File.separator + directory);
            File zipFile = new File(backupDir + File.separator + directory + "_" + dateFormat.format(date)+ ".zip");

            return bManager.performBackup(true, zipFile, oldDirectory);
        }

    }

    @Action(
            aliases = {"backupStatus", "getBackupStatus"})
    public String getBackupStatus() {
        return SpaceRTK.getInstance().getBackupManager().getBackupStatus();
    }

    @Action(
            aliases = {"backupSize", "getBackupSize"})
    public long getBackupSize() {
        return SpaceRTK.getInstance().getBackupManager().getBackupSize();
    }

    @Action(
            aliases = {"dataBackedUp", "getDataBackedUp", "bytesBackedUp", "getBytesBackedUp"})
    public long getDataBackedUp() {
        return SpaceRTK.getInstance().getBackupManager().getDataBackedUp();
    }

    @Action(
            aliases = {"currentBackupFile", "getCurrentBackupFile"})
    public String getCurrentBackupFile() {
        return SpaceRTK.getInstance().getBackupManager().getCurrentFile();
    }

    @Action(
            aliases = {"currentBackupFolder", "getCurrentBackupFolder"})
    public String getCurrentBackupFolder() {
        return SpaceRTK.getInstance().getBackupManager().getCurrentFolder();
    }

    @Action(
            aliases = {"currentBackupProgress", "getCurrentBackupProgress"})
    public String getBackupProgress() {
        return SpaceRTK.getInstance().getBackupManager().getBackupProgress();
    }

    @Action(
            aliases = {"lastBackupError", "getLastBackupError"})
    public String getLastBackupError() {
        return SpaceRTK.getInstance().getBackupManager().getLastError();
    }

    @Action(
            aliases = {"isBackupRunning", "backupRunning"})
    public boolean isBackupRunning() {
        return SpaceRTK.getInstance().getBackupManager().isBackupRunning();
    }

    @Action(
            aliases = {"consoleCommand", "serverCommand", "command"})
    public boolean consoleCommand(final String command) {
        RemoteToolkit.consoleCommand(command);
        return true;
    }

    @Action(
            aliases = {"externalCommand"})
    public boolean externalCommand(final String command) {
        RemoteToolkit.externalCommand(command);
        return true;
    }

    @Action(
            aliases = {"forceRestart", "forceRestartServer"})
    public boolean forceRestart() {
        RemoteToolkit.forceRestart();
        return true;
    }

    @Action(
            aliases = {"forceStop", "forceStopServer"})
    public boolean forceStop() {
        RemoteToolkit.forceStop();
        return true;
    }

    @Action(
            aliases = {"getAllWorlds", "allWorlds"})
    public List<String> getAllWorlds() {
        return Arrays.asList(SpaceRTK.getInstance().worldContainer.list(WorldFileFilter.INSTANCE));
    }

    @Action(
            aliases = {"getBackups", "backups"})
    public LinkedHashMap<String, LinkedList<String>> getBackups() {
        final LinkedHashMap<String, LinkedList<String>> backups = new LinkedHashMap<String, LinkedList<String>>();
        File backupDirectory = new File(File.separator + "backups");
        if (backupDirectory.exists())
            for (final String backupName : backupDirectory.list(DirectoryFileFilter.INSTANCE)) {
                final LinkedList<String> directories = new LinkedList<String>();
                for (final String directoryName : new File(backupDirectory.getPath() + File.separator + backupName)
                        .list(DirectoryFileFilter.INSTANCE))
                    directories.add(directoryName);
                backups.put(backupName, directories);
            }
        if (!SpaceRTK.getInstance().worldContainer.equals(".")) {
            backupDirectory = new File(SpaceRTK.getInstance().worldContainer.getPath() + File.separator + "backups");
            if (backupDirectory.exists())
                for (final String backupName : backupDirectory.list(DirectoryFileFilter.INSTANCE)) {
                    final LinkedList<String> directories = new LinkedList<String>();
                    for (final String directoryName : new File(backupDirectory.getPath() + File.separator + backupName)
                            .list(DirectoryFileFilter.INSTANCE))
                        directories.add(directoryName);
                    if (backups.containsKey(backupName)) {
                        final LinkedList<String> directories_ = backups.get(backupName);
                        directories.addAll(directories_);
                    }
                    backups.put(backupName, directories);
                }
        }
        return backups;
    }

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

    @Action(
            aliases = {"hold", "stop", "stopServer"})
    public boolean hold() {
        RemoteToolkit.hold();
        return true;
    }

    @Action(
            aliases = {"rescheduleRestart", "rescheduleServerRestart"})
    public boolean rescheduleRestart(final String date) {
        RemoteToolkit.rescheduleRestart(date);
        return true;
    }

    @Action(
            aliases = {"restart", "restartServer"})
    public boolean restart(final Boolean save) {
        RemoteToolkit.restart(save);
        return true;
    }

    @Action(
            aliases = {"restartIfEmpty", "restartServerIfEmpty"})
    public boolean restartIfEmpty(final Boolean save) {
        final JSONArray players = (JSONArray) JSONValue.parse(Utilities.sendMethod("getPlayers", "[]"));
        if (players == null || players.size() == 0)
            RemoteToolkit.restart(save);
        return true;
    }

    @Action(
            aliases = {"restore", "restoreDirectory", "restoreDir"})
    public boolean restore(final String date, final String directory) {
        if (directory.equals("*")) {
            boolean result = true;
            final boolean wasRunning = RemoteToolkit.running();
            if (wasRunning) {
                RemoteToolkit.hold();
                while (RemoteToolkit.running())
                    try {
                        Thread.sleep(1000);
                    } catch (final InterruptedException e) {
                        e.printStackTrace();
                    }
            }
            File backupDirectory = new File("backups" + File.separator + date);
            for (final String directoryName_ : Arrays.asList(backupDirectory.list(DirectoryFileFilter.INSTANCE))) {
                final File oldDirectory = new File(backupDirectory.getPath() + File.separator + directoryName_);
                final File newDirectory = new File(directoryName_);
                if (newDirectory.exists()) {
                    newDirectory.delete();
                    newDirectory.mkdir();
                }
                try {
                    FileUtils.copyDirectory(oldDirectory, newDirectory);
                } catch (final IOException e) {
                    e.printStackTrace();
                    result = false;
                }
            }
            if (!SpaceRTK.getInstance().worldContainer.equals(".")) {
                backupDirectory = new File(SpaceRTK.getInstance().worldContainer.getPath() + File.separator + "backups"
                        + File.separator + date);
                for (final String directoryName_ : Arrays.asList(backupDirectory.list(DirectoryFileFilter.INSTANCE))) {
                    final File oldDirectory = new File(backupDirectory.getPath() + File.separator + directoryName_);
                    final File newDirectory = new File(SpaceRTK.getInstance().worldContainer.getPath() + File.separator
                            + directoryName_);
                    if (newDirectory.exists()) {
                        newDirectory.delete();
                        newDirectory.mkdir();
                    }
                    try {
                        FileUtils.copyDirectory(oldDirectory, newDirectory);
                    } catch (final IOException e) {
                        e.printStackTrace();
                        result = false;
                    }
                }
            }
            if (wasRunning)
                RemoteToolkit.unhold();
            return result;
        } else {
            boolean result = true;
            final boolean wasRunning = RemoteToolkit.running();
            if (wasRunning) {
                RemoteToolkit.hold();
                while (RemoteToolkit.running())
                    try {
                        Thread.sleep(1000);
                    } catch (final InterruptedException e) {
                        e.printStackTrace();
                    }
            }
            File backupDirectory = new File("backups" + File.separator + date);
            File oldDirectory = new File(backupDirectory.getPath() + File.separator + directory);
            File newDirectory = new File(directory);
            if (oldDirectory.exists()) {
                if (newDirectory.exists()) {
                    newDirectory.delete();
                    newDirectory.mkdir();
                }
                try {
                    FileUtils.copyDirectory(oldDirectory, newDirectory);
                } catch (final IOException e) {
                    e.printStackTrace();
                    result = false;
                }
            }
            if (!SpaceRTK.getInstance().worldContainer.equals(".")) {
                backupDirectory = new File(SpaceRTK.getInstance().worldContainer + File.separator + "backups"
                        + File.separator + date);
                oldDirectory = new File(backupDirectory.getPath() + File.separator + directory);
                newDirectory = new File(SpaceRTK.getInstance().worldContainer + File.separator + directory);
                if (oldDirectory.exists()) {
                    if (newDirectory.exists()) {
                        newDirectory.delete();
                        newDirectory.mkdir();
                    }
                    try {
                        FileUtils.copyDirectory(oldDirectory, newDirectory);
                    } catch (final IOException e) {
                        e.printStackTrace();
                        result = false;
                    }
                }
            }
            if (wasRunning)
                RemoteToolkit.unhold();
            return result;
        }
    }

    @Action(
            aliases = {"rollOverLog", "rollOver"})
    public boolean rollOver() {
        final File oldLog = new File("server.log");
        final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd_HH-mm");
        final File newLog = new File("server_" + format.format(new Date()) + ".log");
        try {
            if (!newLog.exists())
                newLog.createNewFile();
            FileUtils.writeStringToFile(newLog, FileUtils.readFileToString(oldLog));
            FileUtils.write(oldLog, "");
        } catch (final IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    @Action(
            aliases = {"runChunkster", "chunkster"})
    public boolean runChunkster(final String worldName) {
        final boolean wasRunning = running();
        if (wasRunning)
            hold();
        while (running())
            try {
                Thread.sleep(1000);
            } catch (final InterruptedException e) {
                e.printStackTrace();
            };
        final String[] chunksterArgs = new String[2];
        chunksterArgs[1] = worldName;
        Chunkster.main(chunksterArgs);
        if (wasRunning)
            unhold();
        return true;
    }

    @Action(
            aliases = {"runMapAutoTrim", "mapAutoTrim"})
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

    @Action(
            aliases = {"isRunning", "isServerRunning", "running"})
    public boolean running() {
        return RemoteToolkit.running();
    }

    @Action(
            aliases = {"save", "saveServer"})
    public boolean save() {
        RemoteToolkit.save();
        return true;
    }

    @Action(
            aliases = {"unhold", "start", "startServer"})
    public boolean unhold() {
        RemoteToolkit.unhold();
        return true;
    }
}
