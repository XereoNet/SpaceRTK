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
import java.util.LinkedHashMap;

import me.neatmonster.spacemodule.api.Action;
import me.neatmonster.spacertk.RemoteToolkit;
import me.neatmonster.spacertk.SpaceRTK;
import me.neatmonster.spacertk.plugins.PluginsManager;
import me.neatmonster.spacertk.plugins.templates.SBPlugin;
import me.neatmonster.spacertk.plugins.templates.Version;
import me.neatmonster.spacertk.utilities.Utilities;
import me.neatmonster.spacertk.utilities.ZIP;

import org.apache.commons.io.FileUtils;

public class PluginActions {
    private static PluginsManager pluginsManager = SpaceRTK.getInstance().pluginsManager;

    @Action(
            aliases = {"checkForUpdates", "pluginCheckUpdates"})
    public String checkForUpdates(final String pluginName) {
        try {
            final SBPlugin plugin = pluginsManager.getPlugin(pluginName);
            if (plugin == null)
                return "NOTONBUKKITDEV";
            final File pluginFile = pluginsManager.getPluginFile(plugin.name);
            if (pluginFile != null)
                if (Utilities.getMD5(pluginFile).equalsIgnoreCase(plugin.getLatestVersion().md5))
                    return "UPTODATE,VERSION=" + plugin.getLatestVersion().name;
                else
                    for (final Version version : plugin.versions) {
                        if (Utilities.getMD5(pluginFile).equalsIgnoreCase(version.md5))
                            return "OUTDATED,OLDVERSION=" + version.name + ",NEWVERSION="
                                    + plugin.getLatestVersion().name;
                        return "UNKNOWN,NEWVERSION=" + plugin.getLatestVersion().name;
                    }
            else
                return "FILENOTFOUND";
        } catch (final IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Action(
            aliases = {"disable", "pluginDisable"})
    public boolean disable(final String pluginName) {
        final File pluginFile = pluginsManager.getPluginFile(pluginName);
        if (pluginFile == null)
            return false;
        final boolean wasRunning = RemoteToolkit.isRunning();
        if (wasRunning)
            RemoteToolkit.hold();
        while (RemoteToolkit.isRunning())
            try {
                Thread.sleep(1000);
            } catch (final InterruptedException e) {
                e.printStackTrace();
            }
        final boolean result = pluginFile.renameTo(new File(pluginFile.getPath() + ".DISABLED"));
        if (wasRunning)
            RemoteToolkit.unhold();
        return result;
    }

    @Action(
            aliases = {"enable", "pluginEnable"})
    public boolean enable(final String pluginName) {
        final File pluginFile = pluginsManager.getPluginFile(pluginName);
        if (pluginFile == null)
            return false;
        final boolean result = new File(pluginFile.getPath() + ".DISABLED").renameTo(pluginFile);
        if (RemoteToolkit.isRunning())
            Utilities.sendMethod("reload", "[]");
        return result;
    }

    @Action(
            aliases = {"informations", "pluginInformations"})
    public LinkedHashMap<String, Object> informations(final String pluginName) {
        final SBPlugin plugin = pluginsManager.getPlugin(pluginName);
        if (plugin != null) {
            final LinkedHashMap<String, Object> pluginInformations = new LinkedHashMap<String, Object>();
            pluginInformations.put("Name", plugin.name);
            pluginInformations.put("Status", plugin.status);
            pluginInformations.put("Link", plugin.link);
            pluginInformations.put("Authors", plugin.authors);
            pluginInformations.put("Categories", plugin.categories);
            pluginInformations.put("LatestVersionDate", plugin.getLatestVersion().date);
            pluginInformations.put("LatestVersionName", plugin.getLatestVersion().name);
            pluginInformations.put("LatestVersionFilename", plugin.getLatestVersion().filename);
            pluginInformations.put("LatestVersionMD5", plugin.getLatestVersion().md5);
            pluginInformations.put("LatestVersionLink", plugin.getLatestVersion().link);
            pluginInformations.put("LatestVersionBuilds", plugin.getLatestVersion().builds);
            pluginInformations.put("LatestVersionSoftDependencies", plugin.getLatestVersion().softDependencies);
            pluginInformations.put("LatestVersionHardDependencies", plugin.getLatestVersion().hardDependencies);
            return pluginInformations;
        }
        return new LinkedHashMap<String, Object>();
    }

    @Action(
            aliases = {"install", "pluginInstall"})
    public String install(final String pluginName) {
        final SBPlugin plugin = pluginsManager.getPlugin(pluginName);
        if (plugin == null)
            return "NOTONBUKKITDEV";
        final File pluginFile = pluginsManager.getPluginFile(plugin.name);
        if (pluginFile != null && pluginFile.exists())
            return "ALREADYINSTALLED";
        new FileActions().sendFile(plugin.getLatestVersion().link,
                "plugins" + File.separator + plugin.getLatestVersion().filename);
        final File file = new File("plugins", plugin.getLatestVersion().filename);
        if (file.getName().endsWith(".zip")) {
            try {
                ZIP.unzip(file, new File("plugins"), false);
            } catch (final Exception e) {
                e.printStackTrace();
            }
            file.delete();
        }
        if (RemoteToolkit.isRunning())
            Utilities.sendMethod("reload", "[]");
        return "SUCCESS";
    }

    @Action(
            aliases = {"installByUrl", "pluginInstallByUrl"})
    public String installByUrl(final String url, final String file) {
        new FileActions().sendFile(url, "plugins" + File.separator + file);
        final File file_ = new File("plugins", file);
        if (file.endsWith(".zip")) {
            try {
                ZIP.unzip(file_, new File("plugins"), false);
            } catch (final Exception e) {
                e.printStackTrace();
            }
            file_.delete();
        }
        if (RemoteToolkit.isRunning())
            Utilities.sendMethod("reload", "[]");
        return "SUCCESS";
    }

    @Action(
            aliases = {"remove", "pluginRemove"})
    public String remove(final String pluginName, final Boolean removeDirectory) {
        final File pluginFile = pluginsManager.getPluginFile(pluginName);
        if (pluginFile == null)
            return "FILENOTFOUND";
        final boolean wasRunning = RemoteToolkit.isRunning();
        if (wasRunning) {
            RemoteToolkit.hold();
            while (RemoteToolkit.isRunning())
                try {
                    Thread.sleep(1000);
                } catch (final InterruptedException e) {
                    e.printStackTrace();
                }
        }
        pluginFile.delete();
        final File pluginDirectory = new File("plugins" + File.separator + pluginName);
        if (removeDirectory && pluginDirectory.exists())
            try {
                FileUtils.deleteDirectory(pluginDirectory);
            } catch (final IOException e) {
                e.printStackTrace();
            }
        if (wasRunning)
            RemoteToolkit.unhold();
        return "SUCCESS";
    }

    @Action(
            aliases = {"update", "pluginUpdate"})
    public String update(final String pluginName, final Boolean override) {
        final String result = checkForUpdates(pluginName);
        if (!result.startsWith("OUTDATED"))
            return result;
        final SBPlugin plugin = pluginsManager.getPlugin(pluginName);
        final File pluginFile = pluginsManager.getPluginFile(plugin.name);
        final boolean wasRunning = RemoteToolkit.isRunning();
        if (wasRunning) {
            RemoteToolkit.hold();
            while (RemoteToolkit.isRunning())
                try {
                    Thread.sleep(1000);
                } catch (final InterruptedException e) {
                    e.printStackTrace();
                }
        }
        pluginFile.delete();
        new FileActions().sendFile(plugin.getLatestVersion().link,
                "plugins" + File.separator + plugin.getLatestVersion().filename);
        final File file = new File("plugins", plugin.getLatestVersion().filename);
        if (file.getName().endsWith(".zip")) {
            try {
                ZIP.unzip(file, new File("plugins"), override);
            } catch (final Exception e) {
                e.printStackTrace();
            }
            file.delete();
        }
        if (wasRunning)
            RemoteToolkit.unhold();
        return "SUCCESS";
    }
}
