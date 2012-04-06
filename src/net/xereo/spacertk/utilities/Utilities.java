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
package net.xereo.spacertk.utilities;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.URL;
import java.net.URLConnection;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Logger;

import net.xereo.spacertk.SpaceRTK;

public class Utilities {
    public static Logger logger = Logger.getLogger("Minecraft");

    public static String addHeader(final String string) throws UnsupportedEncodingException {
        String finishedString = "";
        String byteLengthOfFinishedString = "";
        final String newLine = "\r\n";
        if (string != null) {
            byteLengthOfFinishedString = Integer.toString(string.getBytes("UTF8").length);
            finishedString = finishedString + "HTTP/1.1 200 OK" + newLine;
            finishedString = finishedString + "Content-Language:en" + newLine;
            finishedString = finishedString + "Content-Length:" + byteLengthOfFinishedString + newLine;
            finishedString = finishedString + "Content-Type:text/plain; charset=utf-8" + newLine;
            finishedString = finishedString + newLine;
            finishedString = finishedString + string;
        } else {
            finishedString = finishedString + "HTTP/1.1 500 Internal Server Error" + newLine;
            finishedString = finishedString + "Content-Language:en" + newLine;
            finishedString = finishedString + "Content-Length:0" + newLine;
            finishedString = finishedString + "Content-Type:text/html; charset=utf-8" + newLine;
            finishedString = finishedString + newLine;
        }
        return finishedString;
    }

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
}
