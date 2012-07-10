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

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.logging.Handler;
import java.util.logging.Logger;

import com.drdanick.rtoolkit.EventDispatcher;
import com.drdanick.rtoolkit.event.ToolkitEventPriority;
import me.neatmonster.spacemodule.SpaceModule;
import me.neatmonster.spacemodule.api.ActionsManager;
import me.neatmonster.spacertk.actions.FileActions;
import me.neatmonster.spacertk.actions.PluginActions;
import me.neatmonster.spacertk.actions.SchedulerActions;
import me.neatmonster.spacertk.actions.ServerActions;
import me.neatmonster.spacertk.event.BackupEvent;
import me.neatmonster.spacertk.plugins.PluginsManager;
import me.neatmonster.spacertk.scheduler.Scheduler;
import me.neatmonster.spacertk.utilities.BackupManager;
import me.neatmonster.spacertk.utilities.Format;

import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Main class of SpaceRTK
 */
public class SpaceRTK {
    private static SpaceRTK spaceRTK;

    /**
     * Gets the RTK Instance
     * @return RTK Instance
     */
    public static SpaceRTK getInstance() {
        return spaceRTK;
    }

    public ActionsManager actionsManager;
    public PanelListener  panelListener;
    public PluginsManager pluginsManager;

    public String         type = null;
    public int            port;
    public int            rPort;
    public String         salt;
    public File           worldContainer;
    public String         backupDirName;
    public boolean        backupLogs;

    private BackupManager backupManager;
    private PingListener pingListener;

    public static final File baseDir = new File(System.getProperty("user.dir"));

    /**
     * Creates a new RTK
     */
    public SpaceRTK() {
        try {
            final Logger rootlog = Logger.getLogger("");
            for (final Handler h : rootlog.getHandlers())
                h.setFormatter(new Format());
            EventDispatcher edt = SpaceModule.getInstance().getEdt();
            edt.registerListener(new BackupListener(), SpaceModule.getInstance().getEventHandler(), ToolkitEventPriority.SYSTEM, BackupEvent.class);
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Called when the RTK is disabled
     */
    public void onDisable() {
        try {
            panelListener.stopServer();
            pingListener.shutdown();
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Called when the RTK is enabled
     */
    public void onEnable() {
        spaceRTK = this;
        final YamlConfiguration configuration = YamlConfiguration.loadConfiguration(SpaceModule.CONFIGURATION);
        type = configuration.getString("SpaceModule.type", "Bukkit");
        configuration.set("SpaceModule.type", type = "Bukkit");
        salt = configuration.getString("General.salt", "<default>");
        if (salt.equals("<default>")) {
            salt = UUID.randomUUID().toString().replaceAll("-", "").toUpperCase();
            configuration.set("General.salt", salt);
        }
        worldContainer = new File(configuration.getString("General.worldContainer", "."));
        if (type.equals("Bukkit"))
            port = configuration.getInt("SpaceBukkit.port", 2011);
        rPort = configuration.getInt("SpaceRTK.port", 2012);
        backupDirName = configuration.getString("General.backupDirectory", "Backups");
        backupLogs = configuration.getBoolean("General.backupLogs", true);
        try {
            configuration.save(SpaceModule.CONFIGURATION);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if(backupManager == null)
            backupManager = BackupManager.getInstance();

        pingListener = new PingListener();
        pingListener.startup();

        File backupDir = new File(SpaceRTK.getInstance().worldContainer.getPath() + File.separator + SpaceRTK.getInstance().backupDirName);
        for(File f : baseDir.listFiles()) {
            if(f.isDirectory()) {
                if(f.getName().equalsIgnoreCase(backupDirName) && !f.getName().equals(backupDirName)) {
                    f.renameTo(backupDir);
                }
            }
        }

        pluginsManager = new PluginsManager();
        actionsManager = new ActionsManager();
        actionsManager.register(FileActions.class);
        actionsManager.register(PluginActions.class);
        actionsManager.register(SchedulerActions.class);
        actionsManager.register(ServerActions.class);
        panelListener = new PanelListener();
        Scheduler.loadJobs();

    }

    /**
     * Gets the Backup Manager
     * @return Backup Manager
     */
    public BackupManager getBackupManager() {
        return backupManager;
    }
}
