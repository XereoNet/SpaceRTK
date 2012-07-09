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


import com.drdanick.McRKit.PropertiesFile;
import de.schlichtherle.truezip.file.TFile;
import me.neatmonster.spacemodule.SpaceModule;
import me.neatmonster.spacertk.RemoteToolkit;
import me.neatmonster.spacertk.event.BackupEvent;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.URI;
import java.util.List;

public class IsolatedBackupThread extends BackupThread {

    private static final int STRING_BUFFER_SIZE = 128;

    private File[] additionalSources;
    private TFile sourceRoot;
    private TFile destRoot;
    private TFile base;
    private boolean clearDst;
    private List<URI> ignoreList;
    private ProcessBuilder pBuilder;

    public IsolatedBackupThread(String backupName, String uid, File base, List<URI> ignoreList,
            boolean clearDst, File destRoot, File sourceRoot, File... additionalSources) throws IOException{
        this.base = new TFile(base);
        this.backupName = backupName;
        this.uid = uid;
        this.clearDst = clearDst;
        this.ignoreList = ignoreList;
        this.destRoot = new TFile(destRoot);
        this.sourceRoot = new TFile(sourceRoot);
        this.additionalSources = additionalSources;
        offline = true;

        setName("BackupThread-"+uid);

        init();
    }

    private void init() throws IOException {
        PropertiesFile toolkitProperties = RemoteToolkit.PROPERTIES;
        String artifactPath = SpaceModule.getInstance().artifactPath;
        String maxMem = toolkitProperties.getString("maximum-heap-size");

        if(maxMem.isEmpty())
            maxMem = "-Xms32M";
        else
            maxMem = "-Xmx"+maxMem;

        String override = toolkitProperties.getString("overridden-process-arguments");
        String jvmCommand = "java";

        if(override != null && !override.isEmpty())
            jvmCommand = override.split(" ")[0]; //XXX This may not work in all cases. Consider revising.


        String ignore = "";
        for(URI u : ignoreList)
            ignore += ","+u.getRawPath();
        if(!ignore.isEmpty())
            ignore = ignore.substring(1);

        String additional = "";
        for(File f : additionalSources)
            additional += ","+f.getCanonicalPath();
        if(!additional.isEmpty())
            additional = additional.substring(1);



        pBuilder = new ProcessBuilder(jvmCommand, maxMem,"-cp", artifactPath, IsolatedBackupLauncher.class.getName(),
                "backupName:="+backupName, "uid:="+uid, "baseDir:="+base.getCanonicalPath(),
                "ignoreList:="+ignore, "clearDst:="+clearDst, "additionalSources:="+additional,
                "sourceRoot:="+sourceRoot.getCanonicalPath(), "destRoot:="+destRoot.getCanonicalPath());
    }

    public void run() {
        running = true;

        Thread shutdownHook = null;
        StandardStreamHandler stdOut = null;
        StandardStreamHandler stdErr = null;
        try {
            final Process p = pBuilder.start();

            stdOut = new StandardStreamHandler(p.getInputStream(), System.out, true, false, BackupManager.STDOUT_LINE_DELIMITER);
            stdErr = new StandardStreamHandler(p.getErrorStream(), System.err, false, true, (char)10);
            stdOut.start();
            stdErr.start();

            shutdownHook = new Thread(new Runnable(){
                public void run(){
                    p.destroy();
                }
            });
            Runtime.getRuntime().addShutdownHook(shutdownHook);

            p.waitFor();


        } catch(IOException e) {
            e.printStackTrace();
        } catch(InterruptedException e) {
            e.printStackTrace();
        } finally {
            if(shutdownHook != null)
                Runtime.getRuntime().removeShutdownHook(shutdownHook);

            running = false;
            backup = new BackupManager.Backup(uid, backupName, startTime, dataSize, destRoot);

            running = false;
            if(endTime == -1L)
                endTime = System.currentTimeMillis();

            BackupEvent e = new BackupEvent(startTime, endTime, offline, backupName, uid);
            SpaceModule.getInstance().getEdt().fireToolkitEvent(e);
        }
    }


    private class StandardStreamHandler extends Thread {
        InputStream stream;
        PrintStream out;
        char lineDelimiter;
        boolean  processOutput;
        boolean echoOutput;

        StandardStreamHandler(InputStream stream, PrintStream out, boolean processOutput, boolean echoOutput, char lineDelimiter) {
            this.stream = stream;
            this.out = out;
            this.processOutput = processOutput;
            this.echoOutput = echoOutput;
            this.lineDelimiter = lineDelimiter;
        }

        public void run() {
            running = true;
            BufferedReader br = new BufferedReader(new InputStreamReader(stream));
            try {
                StringBuilder builder = new StringBuilder(STRING_BUFFER_SIZE);
                int in;
                do {
                    in = br.read();

                    if(in == lineDelimiter) {
                        if(processOutput)
                            handleOutputString(builder.toString());
                        if(echoOutput)
                            out.println(builder.toString());

                        builder.setLength(0);

                    } else if(in != -1) {
                        builder.append((char)in);
                    }

                } while(in != -1);
            } catch(IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if(br != null)
                        br.close();
                } catch(IOException e) {}
            }
        }
    }

    private void handleOutputString(String str) {
        String[] split = str.split(":=");

        if(split[0].equals("startTime")) {
            startTime = Long.parseLong(split[1]);

        } else if(split[0].equals("endTime")) {
            endTime = Long.parseLong(split[1]);

        } else if(split[0].equals("status")) {
            status = split[1];

        } else if(split[0].equals("error")) {
            error = split[1];

        } else if(split[0].equals("currentFile")) {
            currentFile = split[1];

        } else if(split[0].equals("dataSize")) {
            dataSize = Long.parseLong(split[1]);

        } else if(split[0].equals("dataCopied")) {
            dataCopied = Long.parseLong(split[1]);

        } else if(split[0].equals("progress")) {
            progress = Float.parseFloat(split[1]);
        }

    }

    @Override
    TFile getDestRoot() {
        return destRoot;
    }

}
