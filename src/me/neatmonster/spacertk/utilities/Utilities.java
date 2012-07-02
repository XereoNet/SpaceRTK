/*
 * This file is part of SpaceRTK (http://spacebukkit.xereo.net/).
 *
 * SpaceRTK is free software: you can redistribute it and/or modify it under the terms of the
 * Attribution-NonCommercial-ShareAlike Unported (CC BY-NC-SA) license as published by the Creative
 * Common organization, either version 3.0 of the license, or (at your option) any later version.
 *
 * SpaceRTK is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Attribution-NonCommercial-ShareAlike Unported (CC BY-NC-SA) license for more details.
 *
 * You should have received a copy of the Attribution-NonCommercial-ShareAlike Unported (CC BY-NC-SA)
 * license along with this program. If not, see <http://creativecommons.org/licenses/by-nc-sa/3.0/>.
 */
package me.neatmonster.spacertk.utilities;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Logger;

import de.schlichtherle.truezip.file.TFile;
import me.neatmonster.spacertk.SpaceRTK;

/**
 * Various Utility methods
 */
public class Utilities {
    /**
     * Logger that handles any output to the console
     */
    public static Logger logger = Logger.getLogger("Minecraft");

    /**
     * Adds the HTTP header to a string
     * @param string String to add to
     * @return String with the header
     * @throws UnsupportedEncodingException If the encoding is not UTF-8
     */
    public static String addHeader(final String string) throws UnsupportedEncodingException {
        String finishedString = "";
        String byteLengthOfFinishedString = "";
        final String newLine = "\r\n";
        if (string != null) {
            byteLengthOfFinishedString = Integer.toString(string.getBytes("UTF8").length);
            finishedString += "HTTP/1.1 200 OK" + newLine;
            finishedString += "Content-Language:en" + newLine;
            finishedString += "Content-Length:" + byteLengthOfFinishedString + newLine;
            finishedString += "Content-Type:text/plain; charset=utf-8" + newLine;
            finishedString += newLine;
            finishedString += string;
        } else {
            finishedString += "HTTP/1.1 500 Internal Server Error" + newLine;
            finishedString += "Content-Language:en" + newLine;
            finishedString += "Content-Length:0" + newLine;
            finishedString += "Content-Type:text/html; charset=utf-8" + newLine;
            finishedString += newLine;
        }
        return finishedString;
    }

    /**
     * Encrypts a string with SHA-256
     * @param string String to encrypt
     * @return String encrypted with hex
     * @throws NoSuchAlgorithmException If SHA-256 or UTF-8 is not supported
     */
    public static String crypt(final String string) throws NoSuchAlgorithmException {
        final MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.reset();
        byte[] input = null;
        try {
            input = digest.digest(string.getBytes("UTF-8"));
            final StringBuffer hexString = new StringBuffer();
            for (final byte element : input) {
                final String hex = Integer.toHexString(0xFF & element);
                if (hex.length() == 1)
                    hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (final UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return "UnsupportedEncodingException";
    }
    
    /**
     * Gets the MD5 of a file
     * @param file File to get the MD5 of
     * @return MD5 of the file
     * @throws FileNotFoundException If the file cannot be found
     */
    public static String getMD5(final File file) throws FileNotFoundException {
        try {
            final MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            InputStream inputStream = new FileInputStream(file);
            inputStream = new DigestInputStream(inputStream, messageDigest);
            inputStream.close();
            final byte[] digest = messageDigest.digest();
            return new String(digest);
        } catch (final Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Sends a method to the panel from the plugin
     * @param method Method to send
     * @param arguments Arguments to that method
     * @return Result of the method
     */
    public static String sendMethod(final String method, final String arguments) {
        try {
            final URL url = new URL("http://localhost:" + SpaceRTK.getInstance().port + "/call?method=" + method
                    + "&args=" + arguments + "&key=" + Utilities.crypt(method + SpaceRTK.getInstance().salt));
            final URLConnection connection = url.openConnection();
            final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            final StringBuffer stringBuffer = new StringBuffer();
            String line;
            while ((line = bufferedReader.readLine()) != null)
                stringBuffer.append(line);
            bufferedReader.close();
            return stringBuffer.toString();
        } catch (final ConnectException e) {
            logger.severe("----------------------------------------------------------");
            logger.severe("| SpaceBukkit cannot be reached, please make sure you    |");
            logger.severe("| have placed the awesome plugin to your plugins folder. |");
            logger.severe("| Otherwise report this issue on our issues tracker      |");
            logger.severe("| (http://bit.ly/spacebukkitissues).                     |");
            logger.severe("----------------------------------------------------------");
        } catch (final Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Initializes and builds an FSTree rooted at at the specified base directory
     * and traversed starting at the specified directory.
     * @param file Directory to begin the traversal from.
     * @param base directory to relativize paths against.
     * @return The initialized FSTRee instance.
     * @throws IOException If an error occurs in the underlying IO operations.
     */
    public static FSTree buildFSTree(File file, File base) throws IOException {
        file = file.getCanonicalFile();
        FSTree tree = new FSTree();
        URI baseURI = (base instanceof TFile) ? ((TFile) base).toNonArchiveFile().toURI() : base.toURI();
        if(file.isDirectory()) {
            if(file instanceof TFile)
                buildTFSTree((TFile)file, baseURI, tree);
            else
                buildFSTree(file, baseURI, tree);
        }
        return tree;
    }

    /**
     * Recursive helper method for buildFSTree (used for TrueZIP files).
     */
    private static void buildTFSTree(TFile file, URI base, FSTree tree) {
        for(TFile f : file.listFiles()) {
            if(!f.isDirectory() || f.isArchive()) {
                tree.add(base.relativize(f.toNonArchiveFile().toURI()).getPath(), getFileSize(f));
            } else {
                buildTFSTree(f, base, tree);
            }
        }
    }

    /**
     * Recursive helper method for buildFSTree (used for regular files).
     */
    private static void buildFSTree(File file, URI base, FSTree tree) {
        for(File f : file.listFiles()) {
            if(!f.isDirectory()) {
                tree.add(base.relativize(f.toURI()).getPath(), getFileSize(f));
            } else {
                buildFSTree(f, base, tree);
            }
        }
    }

    /**
     * Gets the size of a file, measured in bytes.
     * @param f File to get the size of.
     * @return Size of the file.
     */
    public static long getFileSize(File f) {
        try {
            InputStream stream = null;
            try {
                URL url = f.toURI().toURL();
                stream = url.openStream();

                return stream.available();
            } finally {
                if(stream != null)
                    stream.close();
            }
        } catch(IOException e) {
            return f.length();
        }
    }

    /**
     * Generate a random n-length alphanumeric string.
     * @param length The length of the string to generate.
     * @param charset The charset to use.
     * @return The randomly generated String.
     */
    public static String generateRandomString(int length, String charset) {
        String str;
        try {
            str = new String(new byte[] {}, charset);
        } catch (UnsupportedEncodingException ex) {
            System.err.println("ERROR: Unsupported encoding: " + charset);
            ex.printStackTrace();
            return null;
        }

        for (int i = 0; i < length; i++) {
            int rand = (int) (Math.random() * 62);
            if (rand > 9 && rand < 36) {
                rand += 7;
            } else if (rand > 35) {
                rand += 13;
            }
            str += (char) (rand + 48);
        }
        return str;
    }

}
