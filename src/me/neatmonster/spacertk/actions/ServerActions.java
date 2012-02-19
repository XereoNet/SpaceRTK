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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

import me.hwei.mctool.MapAutoTrim;
import me.neatmonster.spacemodule.api.Action;
import me.neatmonster.spacertk.RemoteToolkit;
import me.neatmonster.spacertk.SpaceRTK;
import me.neatmonster.spacertk.utilities.Utilities;
import me.neatmonster.spacertk.utilities.WorldFileFilter;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.json.simple.JSONArray;
import org.json.simple.JSONValue;

import chunkster.Chunkster;

public class ServerActions {

    @Action(
            aliases = {"backup", "backupDirectory", "backupDir"})
    public boolean backup(final String directory) {
        boolean result = true;
        final DateFormat dateFormat = new SimpleDateFormat("MM_dd_yyyy_HH_mm");
        final Date date = new Date();
        if (directory.equals("*")) {
            List<String> directoriesNames_ = Arrays.asList(new File(".").list(DirectoryFileFilter.INSTANCE));
            for (final String directoryName : directoriesNames_) {
                final File oldDirectory = new File(directoryName);
                final File newDirectory = new File("backups" + File.separator + dateFormat.format(date)
                        + File.separator + directoryName);
                newDirectory.mkdir();
                try {
                    FileUtils.copyDirectory(oldDirectory, newDirectory);
                } catch (final IOException e) {
                    e.printStackTrace();
                    result = false;
                }
            }
            if (!SpaceRTK.getInstance().worldContainer.equals(".")) {
                directoriesNames_ = Arrays.asList(SpaceRTK.getInstance().worldContainer
                        .list(DirectoryFileFilter.INSTANCE));
                for (final String directoryName : directoriesNames_) {
                    final File oldDirectory = new File(SpaceRTK.getInstance().worldContainer.getPath() + File.separator
                            + directoryName);
                    final File newDirectory = new File(SpaceRTK.getInstance().worldContainer.getPath() + File.separator
                            + "backups" + File.separator + dateFormat.format(date) + File.separator + directoryName);
                    newDirectory.mkdir();
                    try {
                        FileUtils.copyDirectory(oldDirectory, newDirectory);
                    } catch (final IOException e) {
                        e.printStackTrace();
                        result = false;
                    }
                }
            }
        } else {
            final File oldDirectory = new File(SpaceRTK.getInstance().worldContainer.getPath() + File.separator
                    + directory);
            final File newDirectory = new File(SpaceRTK.getInstance().worldContainer.getPath() + File.separator
                    + "backups" + File.separator + dateFormat.format(date) + File.separator + directory);
            newDirectory.mkdir();
            try {
                FileUtils.copyDirectory(oldDirectory, newDirectory);
            } catch (final IOException e) {
                e.printStackTrace();
                result = false;
            }
        }
        return result;
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
