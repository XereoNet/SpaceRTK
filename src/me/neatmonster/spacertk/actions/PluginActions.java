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
import java.io.FileFilter;
import java.io.IOException;
import java.util.LinkedHashMap;

import me.neatmonster.spacemodule.api.Action;
import me.neatmonster.spacemodule.api.ActionHandler;
import me.neatmonster.spacertk.RemoteToolkit;
import me.neatmonster.spacertk.SpaceRTK;
import me.neatmonster.spacertk.plugins.PluginsManager;
import me.neatmonster.spacertk.plugins.templates.SBPlugin;
import me.neatmonster.spacertk.plugins.templates.Version;
import me.neatmonster.spacertk.utilities.Utilities;
import me.neatmonster.spacertk.utilities.ZIP;

import org.apache.commons.io.FileUtils;

/**
 * Actions handler for any Plugin-related actions
 */
public class PluginActions implements ActionHandler {
    private static PluginsManager pluginsManager = SpaceRTK.getInstance().pluginsManager;
    
    private static final FileFilter filter = new FileFilter() {
        public boolean accept(File file) {
            String name = file.getName();
            if (name.contains(".jar") || name.contains(".yml") || file.isDirectory()) {
                return true;
            }
            return false;
        }
    };

    /**
     * Checks to see if a plugin needs updates
     * 
     * NOTONBUKKITDEV - Not on BukkitDev
     * UPTODATE,VERSION=*** - The plugin is up to date and at version ***
     * OUTDATED,OLDVERSION=***,NEWVERSION=^^^ - The plugin is not up to date, it is at version *** and the latest version is ^^^
     * UNKNOWN,NEWVERSION=*** - Unable to find the version, the plugin's newest version is ***
     * FILENOTFOUND - The file could not be found
     * @param pluginName Plugin to check
     * @return If a plugin needs updates
     */
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

    /**
     * Disables a plugin
     * @param pluginName Plugin to disable
     * @return If successful
     */
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

    /**
     * Enables a plugin
     * @param pluginName Plugin to enable
     * @return If successful
     */
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

    /**
     * Gets information about a plugin
     * @param pluginName Plugin to get information about
     * @return Information about a plugin
     */
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

    /**
     * Installs a plugin from BukGet
     * NOTONBUKKITDEV - Plugin is not on BukkitDev
     * ALREADYINSTALLED - The plugin is already installed
     * SUCCESS - The plugin was installed
     * @param pluginName Plugin to install
     * @return Result
     */
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
                ZIP.unzip(file, new File("plugins"), false, filter);
            } catch (final Exception e) {
                e.printStackTrace();
            }
            file.delete();
        }
        if (RemoteToolkit.isRunning())
            Utilities.sendMethod("reload", "[]");
        return "SUCCESS";
    }

    /**
     * Installs a plugin from a URL
     * SUCCESS - The plugin was successfully installed
     * @param url URL to install from
     * @param file File to install to
     * @return
     */
    @Action(
            aliases = {"installByUrl", "pluginInstallByUrl"})
    public String installByUrl(final String url, final String file) {
        new FileActions().sendFile(url, "plugins" + File.separator + file);
        final File file_ = new File("plugins", file);
        if (file.endsWith(".zip")) {
            try {
                ZIP.unzip(file_, new File("plugins"), false, filter);
            } catch (final Exception e) {
                e.printStackTrace();
            }
            file_.delete();
        }
        if (RemoteToolkit.isRunning())
            Utilities.sendMethod("reload", "[]");
        return "SUCCESS";
    }

    /**
     * Removes a plugin from the server
     * FILENOTFOUND - The file was not found
     * SUCCESS - The removal was successful
     * @param pluginName Plugin to remove
     * @param removeDirectory Directory to remove from
     * @return Result
     */
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

    /**
     * Updates a plugin
     * NOTONBUKKITDEV - Not on BukkitDev
     * UPTODATE,VERSION=*** - The plugin is up to date and at version ***
     * UNKNOWN,NEWVERSION=*** - Unable to find the version, the plugin's newest version is ***
     * FILENOTFOUND - The file could not be found
     * SUCCESS - The plugin was updated successfully
     * @param pluginName Plugin to update
     * @param override Only extract the JAR
     * @return Result
     */
    @Action(
            aliases = {"update", "pluginUpdate"})
    public String update(final String pluginName, final Boolean override) {
        final String result = checkForUpdates(pluginName);
        if (!result.startsWith("OUTDATED") && !result.startsWith("UNKNOWN"))
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
                ZIP.unzip(file, new File("plugins"), override, filter);
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
