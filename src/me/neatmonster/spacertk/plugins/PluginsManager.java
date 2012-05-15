/*
 * This file is part of SpaceRTK (http://spacebukkit.xereo.net/).
 *
 * SpaceRTK is free software: you can redistribute it and/or modify it under the terms of the
 * Attribution-NonCommercial-ShareAlike Unported (CC BY-NC-SA) license as published by the Creative Common organization,
 * either version 3.0 of the license, or (at your option) any later version.
 *
 * SpaceRTK is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the Attribution-NonCommercial-ShareAlike
 * Unported (CC BY-NC-SA) license for more details.
 *
 * You should have received a copy of the Attribution-NonCommercial-ShareAlike Unported (CC BY-NC-SA) license along with
 * this program. If not, see <http://creativecommons.org/licenses/by-nc-sa/3.0/>.
 */
package me.neatmonster.spacertk.plugins;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.neatmonster.spacemodule.SpaceModule;
import me.neatmonster.spacertk.plugins.templates.SBPlugin;

import org.bukkit.configuration.file.YamlConfiguration;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

public class PluginsManager {
    private final File jarsFile = new File(SpaceModule.MAIN_DIRECTORY.getPath() + File.separator + "SpaceBukkit",
            "jars.yml");
    public static List<String>        pluginsNames = new ArrayList<String>();

    private final Map<String, SBPlugin> plugins      = new HashMap<String, SBPlugin>();

    public PluginsManager() {
        new Thread(new PluginsRequester()).start();
    }

    private SBPlugin addPlugin(final String pluginName) {
        try {
            final URLConnection connection = new URL("http://bukget.org/api/plugin/" + pluginName).openConnection();
            final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            final StringBuffer stringBuffer = new StringBuffer();
            String line;
            while ((line = bufferedReader.readLine()) != null)
                stringBuffer.append(line);
            bufferedReader.close();
            final String result = stringBuffer.toString();
            if (result == null || result == "")
                return null;
            return new SBPlugin((JSONObject) JSONValue.parse(result));
        } catch (final Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private boolean contains(String pluginName) {
        pluginName = pluginName.toLowerCase();
        if (pluginsNames.contains(pluginName))
            return true;
        if (pluginsNames.contains(pluginName.replace(" ", "")))
            return true;
        if (pluginsNames.contains(pluginName.replace(" ", "_")))
            return true;
        if (pluginsNames.contains(pluginName.replace(" ", "-")))
            return true;
        return false;
    }

    public SBPlugin getPlugin(String pluginName) {
        pluginName = pluginName.toLowerCase();
        if (plugins.containsKey(pluginName))
            return plugins.get(pluginName);
        if (plugins.containsKey(pluginName.replace(" ", "")))
            return plugins.get(pluginName.replace(" ", ""));
        if (plugins.containsKey(pluginName.replace(" ", "_")))
            return plugins.get(pluginName.replace(" ", "_"));
        if (plugins.containsKey(pluginName.replace(" ", "-")))
            return plugins.get(pluginName.replace(" ", "-"));
        if (contains(pluginName)) {
            final SBPlugin plugin1 = addPlugin(pluginName);
            if (plugin1 == null) {
                final SBPlugin plugin2 = addPlugin(pluginName.replace(" ", ""));
                if (plugin2 == null) {
                    final SBPlugin plugin3 = addPlugin(pluginName.replace(" ", "_"));
                    if (plugin3 == null) {
                        final SBPlugin plugin4 = addPlugin(pluginName.replace(" ", "-"));
                        plugins.put(pluginName, plugin4);
                        return plugin4;
                    } else {
                        plugins.put(pluginName, plugin3);
                        return plugin3;
                    }
                } else {
                    plugins.put(pluginName, plugin2);
                    return plugin2;
                }
            } else {
                plugins.put(pluginName, plugin1);
                return plugin1;
            }
        } else
            return null;
    }

    public File getPluginFile(String pluginName) {
        pluginName = pluginName.toLowerCase().replace(" ", "");
        final YamlConfiguration configuration = YamlConfiguration.loadConfiguration(jarsFile);
        final String fileName = configuration.getString(pluginName);
        try {
            configuration.save(jarsFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (fileName == null || fileName == "")
            return null;
        else
            return new File("plugins", fileName);
    }
}
