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
package me.neatmonster.spacertk.utilities.backup;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IsolatedBackupLauncher {

    private Map<String, String> settings = new HashMap<String, String>();


    public static void main(String[] args) {
        new IsolatedBackupLauncher(args).start();
    }

    private IsolatedBackupLauncher(String[] args) {
        for(String s : args) {
            String[] split = s.split(":=");
            if(split.length == 1)
                settings.put(split[0], "");
            else if(split.length > 1)
                settings.put(split[0], split[1]);
        }
    }

    private void start() {
        String backupName = settings.get("backupName");
        String uid = settings.get("uid");
        File base = new File(settings.get("baseDir"));
        URI userDir = null;
        try {
            userDir = new URI(settings.get("userDir"));
        } catch (URISyntaxException e) {
            e.printStackTrace();
            System.exit(0);
        }
        List<URI> ignoreList = new ArrayList<URI>();
        boolean clearDst = Boolean.parseBoolean(settings.get("clearDst"));
        File destRoot = new File(settings.get("destRoot"));
        File sourceRoot = new File(settings.get("sourceRoot"));
        List<File> additionalSrcList = new ArrayList<File>();
        File[] additionalSources;

        try {
            for(String s : settings.get("ignoreList").split(","))
                ignoreList.add(new URI(s));
        } catch(URISyntaxException e) {
            e.printStackTrace();
            System.exit(0);
        }

        for(String s : settings.get("additionalSources").split(","))
            additionalSrcList.add(new File(s));

        additionalSources = new File[additionalSrcList.size()];
        for(int i = 0; i < additionalSources.length; i++)
            additionalSources[i] = additionalSrcList.get(i);


        BackupThread bThread = new BackupThread(true, false, backupName, uid, base, userDir, ignoreList,
                clearDst, true, destRoot, sourceRoot, additionalSources);

        bThread.start();

        try {
            bThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
