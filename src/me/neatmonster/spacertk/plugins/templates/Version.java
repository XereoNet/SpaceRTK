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
 * Holds BukGet information about a version of a plugin
 */
public class Version {

    /**
     * Builds in this version
     */
    public List<String> builds           = new ArrayList<String>();
    /**
     * Date at which the version was released
     */
    public long         date             = 0L;
    /**
     * Filename of the version
     */
    public String       filename         = "";
    /**
     * A list of hard dependencies this version requires
     */
    public List<String> hardDependencies = new ArrayList<String>();
    /**
     * A link to the download of this version
     */
    public String       link             = "";
    /**
     * The MD5 of this version
     */
    public String       md5              = "";
    /**
     * The name of this version
     */
    public String       name             = "";
    /**
     * A list of soft dependencies this version requires
     */
    public List<String> softDependencies = new ArrayList<String>();

    /**
     * Creates a new version
     * @param version JSONObject containing the raw information from BukGet
     */
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
