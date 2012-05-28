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
package me.neatmonster.spacertk.plugins.templates;

import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * Holds BukGet information about a plugin
 */
public class SBPlugin {
    /**
     * Authors that currently contribute the the plugin
     */
    public List<String>  authors     = new ArrayList<String>();
    /**
     * Categories the plugin is applicable under
     */
    public List<String>  categories  = new ArrayList<String>();
    /**
     * A description of the plugin
     */
    public String        description = "";
    /**
     * A link to the plugins website
     */
    public String        link        = "";
    /**
     * The name of the plugin
     */
    public String        name        = "";
    /**
     * The status of the plugin
     */
    public String        status      = "";
    /**
     * A list of versions of the plugin (On BukkitDev)
     */
    public List<Version> versions    = new ArrayList<Version>();

    /**
     * Creats a new Plugin
     * @param plugin JSONObject containing the raw information from BukGet
     */
    public SBPlugin(final JSONObject plugin) {
        name = (String) plugin.get("name");
        status = (String) plugin.get("status");
        link = (String) plugin.get("bukkitdev_link");
        description = (String) plugin.get("desc");
        final JSONArray authorsJSONArray = (JSONArray) plugin.get("authors");
        @SuppressWarnings("unchecked")
        final List<Object> authorsListObject = authorsJSONArray.subList(0, authorsJSONArray.size());
        for (final Object object : authorsListObject)
            authors.add((String) object);
        final JSONArray categoriesJSONArray = (JSONArray) plugin.get("categories");
        @SuppressWarnings("unchecked")
        final List<Object> categoriesListObject = categoriesJSONArray.subList(0, categoriesJSONArray.size());
        for (final Object object : categoriesListObject)
            categories.add((String) object);
        final JSONArray versionsJSONArray = (JSONArray) plugin.get("versions");
        @SuppressWarnings("unchecked")
        final List<Object> versionsListObject = versionsJSONArray.subList(0, versionsJSONArray.size());
        final List<JSONObject> versionsListJSONObjects = new ArrayList<JSONObject>();
        for (final Object object : versionsListObject)
            versionsListJSONObjects.add((JSONObject) object);
        for (final JSONObject object : versionsListJSONObjects)
            versions.add(new Version(object));
    }

    /**
     * Gets the latest version of a plugin
     * @return Latest version
     */
    public Version getLatestVersion() {
        int index = 0;
        long date = 0;
        for (int x = 0; x < versions.size(); x++)
            if (versions.get(x).date > date) {
                date = versions.get(x).date;
                index = x;
            }
        return versions.get(index);
    }
}
