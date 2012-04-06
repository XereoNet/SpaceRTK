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
package net.xereo.spacertk.plugins.templates;

import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class Plugin {
    public List<String>  authors     = new ArrayList<String>();
    public List<String>  categories  = new ArrayList<String>();
    public String        description = "";
    public String        link        = "";
    public String        name        = "";
    public String        status      = "";
    public List<Version> versions    = new ArrayList<Version>();

    public Plugin(final JSONObject plugin) {
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
