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
package me.neatmonster.spacertk.scheduler;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

import me.neatmonster.spacemodule.api.UnhandledActionException;

import org.bukkit.configuration.file.YamlConfiguration;
import org.json.simple.JSONValue;

public class Scheduler {
    public static final File JOBS_FILE = new File("SpaceModule", "jobs.yml");
    
    private static LinkedHashMap<String, Job> jobs = new LinkedHashMap<String, Job>();

    public static void addJob(final String jobName, final Job job) {
        if (!jobs.containsKey(jobName)) {
            jobs.put(jobName, job);
            saveJobs();
        }
    }

    public static LinkedHashMap<String, Job> getJobs() {
        return jobs;
    }

    public static void loadJobs() {
        if (!JOBS_FILE.exists())
            try {
                JOBS_FILE.createNewFile();
            } catch (final IOException e) {
                e.printStackTrace();
            }
        final YamlConfiguration configuration = YamlConfiguration.loadConfiguration(JOBS_FILE);
        final LinkedList<String> jobsNames = new LinkedList<String>();
        for (final String key : configuration.getValues(false).keySet())
            if (key.contains("."))
                jobsNames.add(key.split("\\.")[0]);
        for (final String jobName : jobsNames)
            if (!jobs.containsKey(jobName)) {
                final String timeType = configuration.getString(jobName + ".TimeType");
                final String timeArgument = configuration.getString(jobName + ".TimeArgument");
                final String actionName = configuration.getString(jobName + ".ActionName");
                @SuppressWarnings("unchecked")
                final Object[] actionArguments = ((List<Object>) JSONValue.parse((String) configuration
                        .get(jobName + ".ActionArguments"))).toArray();
                try {
                    final Job job = new Job(actionName, actionArguments, timeType, timeArgument, true);
                    jobs.put(jobName, job);
                } catch (final UnschedulableException e) {
                    e.printStackTrace();
                } catch (final UnhandledActionException e) {
                    e.printStackTrace();
                }
            }
        try {
            configuration.save(JOBS_FILE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void removeJob(final String jobName) {
        jobs.get(jobName).abort();
        jobs.remove(jobName);
        saveJobs();
    }

    public static void saveJobs() {
        final File file = new File("SpaceModule", "jobs.yml");
        if (!file.exists())
            try {
                file.createNewFile();
            } catch (final IOException e) {
                e.printStackTrace();
            }
        final YamlConfiguration configuration = YamlConfiguration.loadConfiguration(file);
        for (final String jobName : jobs.keySet()) {
            final Job job = jobs.get(jobName);
            final String actionName = job.actionName;
            final List<Object> actionArguments = Arrays.asList(job.actionArguments);
            final String timeType = job.timeType;
            final String timeArgument = job.timeArgument;
            configuration.set(jobName + ".TimeType", timeType);
            configuration.set(jobName + ".TimeArgument", timeArgument);
            configuration.set(jobName + ".ActionName", actionName);
            configuration.set(jobName + ".ActionArguments",
                    JSONValue.toJSONString(actionArguments).replace("[[", "[").replace("],[", ",").replace("]]", "]"));
        }
        try {
            configuration.save(JOBS_FILE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
