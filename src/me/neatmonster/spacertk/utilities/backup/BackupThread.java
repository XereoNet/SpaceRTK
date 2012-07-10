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

import de.schlichtherle.truezip.file.TFile;
import de.schlichtherle.truezip.file.TFileInputStream;
import de.schlichtherle.truezip.file.TFileOutputStream;
import de.schlichtherle.truezip.file.TVFS;
import de.schlichtherle.truezip.fs.FsSyncException;
import me.neatmonster.spacemodule.SpaceModule;
import me.neatmonster.spacertk.event.BackupEvent;
import me.neatmonster.spacertk.utilities.FSTree;
import me.neatmonster.spacertk.utilities.Utilities;
import me.neatmonster.spacertk.utilities.backup.BackupManager.Backup;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.URI;
import java.util.List;

class BackupThread extends Thread {
    private boolean isolated = false;
    private boolean sendEvent;
    private File[] additionalSources;
    private TFile sourceRoot;
    private TFile destRoot;
    private TFile base;
    private URI userDir;
    private boolean clearDst;
    private List<URI> ignoreList;
    private boolean printData;
    Backup backup;
    boolean offline;
    long startTime = -1L;
    long endTime = -1L;
    String status = "Idle";
    String error = "";
    String backupName;
    String uid;
    String currentFile;
    long dataSize = 0L;
    long dataCopied = 0L;
    float progress = 0.0f;
    boolean running = false;

    public BackupThread(boolean printData, boolean sendEvent, String backupName, String uid, File base, URI userDir, List<URI> ignoreList,
            boolean clearDst, boolean offline, File destRoot, File sourceRoot, File... additionalSources) {
        this.printData = printData;
        this.sendEvent = sendEvent;
        this.base = new TFile(base);
        this.userDir = userDir;
        this.backupName = backupName;
        this.uid = uid;
        this.clearDst = clearDst;
        this.ignoreList = ignoreList;
        this.offline = offline;
        this.destRoot = new TFile(destRoot);
        this.sourceRoot = new TFile(sourceRoot);
        this.additionalSources = additionalSources;

        setName("BackupThread-"+uid);
    }

    protected BackupThread() {
        isolated = true;
    }
    //XXX: This needs a serious cleanup
    public void run() {
        if(isolated) //Do not run if class was constructed from an IsolatedBackupThread.
            return;

        FSTree sourceTree;
        ObjectInputStream oi = null;
        ObjectOutputStream oo = null;
        PrintWriter fOut = null;
        running  = true;
        startTime = System.currentTimeMillis();
        status = "Initializing";

        if(printData) {
            printData("startTime:="+startTime);
            printData("status:="+status);
        }

        try {
            TFile sourceFile = new TFile(sourceRoot);
            TFile backupIndex = new TFile(sourceRoot, "backup.index");
            TFile backupMeta = new TFile(sourceRoot, "backup.info");

            //Initialise the file index.
            if(backupIndex.exists()) {
                oi = new ObjectInputStream(new TFileInputStream(backupIndex));
                sourceTree = (FSTree)oi.readObject();
                base = sourceRoot;
                oi.close();
            } else {
                sourceTree = Utilities.buildFSTree(new TFile(sourceRoot), base);
                if(additionalSources != null) {
                    for(File f : additionalSources) {
                        FSTree tree = Utilities.buildFSTree(new TFile(f), base);
                        sourceTree.merge(tree);
                    }
                }
            }
            //Remove ignored files from the index
            for(URI u : ignoreList)
                sourceTree.remove(userDir.relativize(u).getPath());
            sourceTree.remove(base.getName()+"/"+"backup.index");
            sourceTree.remove(base.getName()+"/"+"backup.info");

            if(sourceFile.isArchive())
                dataSize = Utilities.getFileSize(sourceFile.toNonArchiveFile());
            else
                dataSize = sourceTree.getSize();

            if(printData) {
                printData("dataSize:="+dataSize);
            }

            if(clearDst) {
                status = "Wiping out destination directories";
                if(printData) {
                    printData("status:="+status);
                }

                if(destRoot.isArchive())
                    destRoot.toNonArchiveFile().rm();
                else {
                    for(String s : sourceTree) {
                        File file = new File(destRoot, s);
                        if(file.isDirectory())
                            for(File f : file.listFiles())
                                if(!f.isDirectory())
                                    f.delete();
                    }
                }
            }

            if(!backupIndex.exists()) {
                backupIndex = new TFile(destRoot, "backup.index");
                if(!destRoot.exists())
                    new TFile(destRoot).mkdirs();
                oo = new ObjectOutputStream(new TFileOutputStream(backupIndex));
                oo.writeObject(sourceTree);
                oo.close();
            }

            if(!backupMeta.exists()) {
                backupMeta = new TFile(destRoot, "backup.info");
                if(!destRoot.exists())
                    new TFile(destRoot).mkdirs();
                fOut = new PrintWriter(new TFileOutputStream(backupMeta));
                fOut.println("name:"+backupName);
                fOut.println("uid:"+ uid);
                fOut.println("date:"+startTime);
                fOut.println("size:"+dataSize);
                fOut.close();

                Backup backup = new Backup(uid, backupName, startTime, dataSize, destRoot);
                this.backup = backup;
            }

            List<String> sourceFiles = sourceTree.enumerateLeaves(null);
            sourceTree.removeAll();

            for(String path : sourceFiles) {
                TFile src = new TFile(base, path);
                TFile dst = new TFile(destRoot, path);
                status = "Processing "+src.getName();
                currentFile = src.getName();

                if(printData) {
                    printData("status:="+status);
                    printData("currentFile:="+currentFile);
                }

                if(!dst.isDirectory())
                    dst.getParentFile().mkdirs();
                else
                    dst.mkdirs();

                try {
                    if(!src.isArchive())
                        src.cp_p(dst);
                    else
                        src.toNonArchiveFile().cp_p(dst.toNonArchiveFile());
                } catch(IOException e) {
                    e.printStackTrace();
                }

                if(sourceFile.isArchive())
                    dataCopied += src.length();
                else
                    dataCopied += Utilities.getFileSize(src);


                progress = dataCopied / (dataSize + (0.15f * dataSize));
                if(printData) {
                    printData("dataCopied:="+dataCopied);
                    printData("progress:="+progress);
                }

            }

            progress = 0.85f; //Temporary

            if(printData) {
                printData("progress:="+progress);
            }

        } catch (IOException e) {
            e.printStackTrace();
            for (StackTraceElement el : e.getStackTrace()) {
                error += el + "\n";
            }
            status = "Error";
            if(printData) {
                printData("error:="+error);
                printData("status:="+status);
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            for (StackTraceElement el : e.getStackTrace()) {
                error += el + "\n";
            }
            status = "Error";
            if(printData) {
                printData("error:="+error);
                printData("status:="+status);
            }
        } finally {
            try { //Attempt to close the streams again in the case that an exception was thrown before closing them.
                if(oi != null)
                    oi.close();
                if(oo != null)
                    oo.close();
                if(fOut != null)
                    fOut.close();
            } catch(IOException e){
                for (StackTraceElement el : e.getStackTrace()) {
                    error += el + "\n";
                }
                status = "Error";
                if(printData) {
                    printData("error:="+error);
                    printData("status:="+status);
                }
            }

            if(error.isEmpty()) {
                status = "Finalizing";
                if(printData) {
                    printData("status:="+status);
                }
            }
            try {
                TVFS.umount(sourceRoot);
            } catch(FsSyncException e){
                e.printStackTrace();
            }

            try {
                TVFS.umount(destRoot);
            } catch(FsSyncException e){
                e.printStackTrace();
            }
            if(error.isEmpty()) {
                status = "Done";

                if(printData) {
                    printData("status:="+status);
                }
            }

            endTime = System.currentTimeMillis();
            running = false;
            progress = 1.0f;

            if(printData) {
                printData("endTime:="+endTime);
                printData("progress:="+progress);
            }

            if(sendEvent) {
                BackupEvent e = new BackupEvent(startTime, endTime, offline, backupName, uid);
                SpaceModule.getInstance().getEdt().fireToolkitEvent(e);
            }
        }
    }

    TFile getDestRoot() {
        return destRoot;
    }

    private void printData(String s) {
        System.out.print(s + BackupManager.STDOUT_LINE_DELIMITER);
    }
}