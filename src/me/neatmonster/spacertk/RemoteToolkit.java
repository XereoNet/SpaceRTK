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

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import com.drdanick.McRKit.Scheduler;
import com.drdanick.McRKit.ToolkitAction;
import com.drdanick.McRKit.Wrapper;

public class RemoteToolkit {

    public static void consoleCommand(String command) {
        try {
            final Field field = Wrapper.getInstance().getClass().getDeclaredField("console");
            field.setAccessible(true);
            final OutputStream console = (OutputStream) field.get(Wrapper.getInstance());
            if (!command.endsWith("\n"))
                command = command + "\n";
            console.write(command.getBytes());
            console.flush();
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    public static void externalCommand(final String command) {
        try {
            Runtime.getRuntime().exec(command);
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    public static void forceRestart() {
        Wrapper.getInterface().performAction(ToolkitAction.FORCERESTART, null);
    }

    public static void forceStop() {
        try {
            final Field restarting = Wrapper.getInstance().getClass().getDeclaredField("restarting");
            final Field serverRunning = Wrapper.getInstance().getClass().getDeclaredField("serverRunning");
            final Field pauseMode = Wrapper.getInstance().getClass().getDeclaredField("pauseMode");
            final Field saveOnServerStop = Wrapper.getInstance().getClass().getDeclaredField("saveOnServerStop");
            restarting.setAccessible(true);
            serverRunning.setAccessible(true);
            pauseMode.setAccessible(true);
            saveOnServerStop.setAccessible(true);
            restarting.setBoolean(Wrapper.getInstance(), false);

            if(serverRunning.getBoolean(Wrapper.getInstance())) {
                Method cancelTasks = Wrapper.getInstance().getClass().getDeclaredMethod("cancelTasks", Scheduler.Type.class);
                Method saveAndWait = Wrapper.getInstance().getClass().getDeclaredMethod("saveAndWait");
                cancelTasks.setAccessible(true);
                saveAndWait.setAccessible(true);

                cancelTasks.invoke(Wrapper.getInstance(), Scheduler.Type.SAVE);

                if(saveOnServerStop.getBoolean(Wrapper.getInstance()))
                    saveAndWait.invoke(Wrapper.getInstance());
                consoleCommand("kickallstop");
                Thread.sleep(1000);
                consoleCommand("stop");
            } else {
                pauseMode.setBoolean(Wrapper.getInstance(), false);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void hold() {
        try {
            final Field restarting = Wrapper.getInstance().getClass().getDeclaredField("restarting");
            final Field serverRunning = Wrapper.getInstance().getClass().getDeclaredField("serverRunning");
            final Field pauseMode = Wrapper.getInstance().getClass().getDeclaredField("pauseMode");
            final Field saveOnServerStop = Wrapper.getInstance().getClass().getDeclaredField("saveOnServerStop");
            restarting.setAccessible(true);
            serverRunning.setAccessible(true);
            pauseMode.setAccessible(true);
            saveOnServerStop.setAccessible(true);

            if (serverRunning.getBoolean(Wrapper.getInstance())) {
                Method cancelTasks = Wrapper.getInstance().getClass().getDeclaredMethod("cancelTasks", Scheduler.Type.class);
                Method saveAndWait = Wrapper.getInstance().getClass().getDeclaredMethod("saveAndWait");
                cancelTasks.setAccessible(true);
                saveAndWait.setAccessible(true);

                restarting.setBoolean(Wrapper.getInstance(), true);
                pauseMode.setBoolean(Wrapper.getInstance(), true);

                cancelTasks.invoke(Wrapper.getInstance(), Scheduler.Type.SAVE);

                if(saveOnServerStop.getBoolean(Wrapper.getInstance()))
                    saveAndWait.invoke(Wrapper.getInstance());

                consoleCommand("kickallhold");
                Thread.sleep(1000);
                consoleCommand("stop");

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void rescheduleRestart(final String date) {
        Wrapper.getInstance().performAction(ToolkitAction.RESCHEDULE, date);
    }

    public static void restart(final boolean save) {
        if (save)
            save();
        Wrapper.getInterface().performAction(ToolkitAction.RESTART, null);
    }

    public static boolean isRunning() {
        try {
            final Field field = Wrapper.getInstance().getClass().getDeclaredField("serverRunning");
            field.setAccessible(true);
            return (Boolean) field.get(Wrapper.getInstance());
        } catch (final Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void save() {
        consoleCommand("save-all");
    }

    public static void unhold() {
        Wrapper.getInterface().performAction(ToolkitAction.UNHOLD, null);
    }
}
