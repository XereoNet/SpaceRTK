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
import java.util.UUID;
import java.util.logging.Handler;
import java.util.logging.Logger;

import me.neatmonster.spacemodule.api.ActionsManager;
import me.neatmonster.spacertk.actions.FileActions;
import me.neatmonster.spacertk.actions.PluginActions;
import me.neatmonster.spacertk.actions.SchedulerActions;
import me.neatmonster.spacertk.actions.ServerActions;
import me.neatmonster.spacertk.plugins.PluginsManager;
import me.neatmonster.spacertk.scheduler.Scheduler;
import me.neatmonster.spacertk.utilities.Format;

import org.bukkit.util.config.Configuration;

@SuppressWarnings("deprecation")
public class SpaceRTK {
    private static SpaceRTK spaceRTK;

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
    public String         worldContainer;

    public SpaceRTK() {
        try {
            final Logger rootlog = Logger.getLogger("");
            for (final Handler h : rootlog.getHandlers())
                h.setFormatter(new Format());
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    public File getWorldContainer() {
        File worldContainer_ = new File(worldContainer);
        if (!worldContainer_.isDirectory())
            worldContainer_ = worldContainer_.getParentFile();
        return worldContainer_;
    }

    public void onDisable() {
        try {
            panelListener.stopServer();
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    public void onEnable() {
        spaceRTK = this;
        final Configuration configuration = new Configuration(new File("SpaceModule", "configuration.yml"));
        configuration.load();
        type = configuration.getString("SpaceModule.Type", "Bukkit");
        configuration.setProperty("SpaceModule.Type", type = "Bukkit");
        salt = configuration.getString("General.Salt", "<default>");
        if (salt.equals("<default>")) {
            salt = UUID.randomUUID().toString().replaceAll("-", "").toUpperCase();
            configuration.setProperty("General.Salt", salt);
        }
        worldContainer = configuration.getString("General.WorldContainer", ".");
        if (type.equals("Bukkit"))
            port = configuration.getInt("SpaceBukkit.Port", 2011);
        rPort = configuration.getInt("SpaceRTK.Port", 2012);
        configuration.save();
        pluginsManager = new PluginsManager();
        actionsManager = new ActionsManager();
        actionsManager.register(FileActions.class);
        actionsManager.register(PluginActions.class);
        actionsManager.register(SchedulerActions.class);
        actionsManager.register(ServerActions.class);
        panelListener = new PanelListener();
        Scheduler.loadJobs();
    }
}
