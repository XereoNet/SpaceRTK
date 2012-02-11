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

public class Version {

    public List<String> builds           = new ArrayList<String>();
    public long         date             = 0L;
    public String       filename         = "";
    public List<String> hardDependencies = new ArrayList<String>();
    public String       link             = "";
    public String       md5              = "";
    public String       name             = "";
    public List<String> softDependencies = new ArrayList<String>();

    public Version(final JSONObject version) {
        date = (Long) version.get("date");
        name = (String) version.get("name");
        filename = (String) version.get("filename");
        md5 = (String) version.get("md5");
        link = (String) version.get("dl_link");
        final JSONArray buildsJSONArray = (JSONArray) version.get("game_builds");
        @SuppressWarnings("unchecked")
        final List<Object> buildsListObject = buildsJSONArray.subList(0, buildsJSONArray.size());
        for (final Object object : buildsListObject)
            builds.add((String) object);
        final JSONArray softDependenciesJSONArray = (JSONArray) version.get("soft_dependencies");
        @SuppressWarnings("unchecked")
        final List<Object> softDependenciesListObject = softDependenciesJSONArray.subList(0,
                softDependenciesJSONArray.size());
        for (final Object object : softDependenciesListObject)
            softDependencies.add((String) object);
        final JSONArray hardDependenciesJSONArray = (JSONArray) version.get("hard_dependencies");
        @SuppressWarnings("unchecked")
        final List<Object> hardDependenciesListObject = hardDependenciesJSONArray.subList(0,
                hardDependenciesJSONArray.size());
        for (final Object object : hardDependenciesListObject)
            hardDependencies.add((String) object);
    }
}
