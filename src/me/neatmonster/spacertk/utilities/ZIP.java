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
package me.neatmonster.spacertk.utilities;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * The Class ZIP. This class is used to zip/unzip files.
 */
public class ZIP {

    /**
     * Adds the file to zip.
     *
     * @param path
     *            the path
     * @param srcFile
     *            the src file
     * @param zip
     *            the zip
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    private static void addFileToZip(final String path, final String srcFile, final ZipOutputStream zip)
            throws IOException {
        final File folder = new File(srcFile);
        if (folder.isDirectory())
            addFolderToZip(path, srcFile, zip);
        else {
            final byte[] buf = new byte[1024];
            int len;
            final FileInputStream in = new FileInputStream(srcFile);
            zip.putNextEntry(new ZipEntry(path + "/" + folder.getName()));
            while ((len = in.read(buf)) > 0)
                zip.write(buf, 0, len);
        }
    }

    /**
     * Adds the folder to zip.
     *
     * @param path
     *            the path
     * @param srcFolder
     *            the src folder
     * @param zip
     *            the zip
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    private static void addFolderToZip(final String path, final String srcFolder, final ZipOutputStream zip)
            throws IOException {
        final File folder = new File(srcFolder);
        for (final String fileName : folder.list())
            if (path.equals(""))
                addFileToZip(folder.getName(), srcFolder + "/" + fileName, zip);
            else
                addFileToZip(path + "/" + folder.getName(), srcFolder + "/" + fileName, zip);
    }

    /**
     * Unzips a ZIP
     *
     * @param archive
     *            the archive
     * @param folder
     *            the folder
     * @param jarOnly
     *            the jar only
     * @param filter
     *            the filter used in extraction, null if none
     * @throws FileNotFoundException
     *             the file not found exception
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    public static void unzip(final File archive, final File folder, final boolean jarOnly, final FileFilter filter)
            throws FileNotFoundException, IOException {
        final ZipInputStream zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(
                archive.getCanonicalFile())));
        ZipEntry ze = null;
        try {
            while ((ze = zis.getNextEntry()) != null)
                if (!jarOnly || ze.getName().toLowerCase().replace(" ", "-").endsWith(".jar")) {
                    final File f = new File(folder.getCanonicalPath(), ze.getName());
                    if (f.isDirectory()) {
                        f.mkdirs();
                        continue;
                    }
                    if (filter == null || !filter.accept(f)) {
                        f.getParentFile().mkdirs();
                        final OutputStream fos = new BufferedOutputStream(new FileOutputStream(f));
                        try {
                            try {
                                final byte[] buf = new byte[8192];
                                int bytesRead;
                                while (-1 != (bytesRead = zis.read(buf)))
                                    fos.write(buf, 0, bytesRead);
                            } finally {
                                fos.close();
                            }
                        } catch (final IOException ioe) {
                            f.delete();
                            throw ioe;
                        }
                    }
                }
            } finally {
                zis.close();
            }
    }

    /**
     * Zips a File
     *
     * @param archive
     *            the archive
     * @param folder
     *            the folder
     */
    public static void zip(final File archive, final File folder) {
        if (archive.exists())
            archive.delete();
        try {
            ZipOutputStream zip = null;
            FileOutputStream fileWriter = null;
            fileWriter = new FileOutputStream(archive);
            zip = new ZipOutputStream(fileWriter);
            addFolderToZip("", folder.getPath(), zip);
            zip.flush();
            zip.close();
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }
}
