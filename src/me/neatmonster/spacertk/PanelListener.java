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
package me.neatmonster.spacertk;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URLDecoder;
import java.util.List;

import me.neatmonster.spacemodule.api.InvalidArgumentsException;
import me.neatmonster.spacemodule.api.UnhandledActionException;
import me.neatmonster.spacertk.utilities.Utilities;
import me.neatmonster.spacertk.utilities.ZIP;

import org.json.simple.JSONValue;
import sun.nio.ch.SocketOpts;

/**
 * Listens and accepts requests from the panel
 */
public class PanelListener extends Thread {

    /**
     * Interprets a raw command from the panel
     * @param string input from panel
     * @return result of the action
     * @throws InvalidArgumentsException Thrown when the wrong arguments are used by the panel
     * @throws UnhandledActionException Thrown when there is no handler for the action
     */
    @SuppressWarnings("unchecked")
    private static Object interpret(final String string) throws InvalidArgumentsException, UnhandledActionException {
        final int indexOfMethod = string.indexOf("?method=");
        final int indexOfArguments = string.indexOf("&args=");
        final int indexOfKey = string.indexOf("&key=");
        final String method = string.substring(indexOfMethod + 8, indexOfArguments);
        final String argumentsString = string.substring(indexOfArguments + 6, indexOfKey);
        final List<Object> arguments = (List<Object>) JSONValue.parse(argumentsString);
        try {
            if (SpaceRTK.getInstance().actionsManager.contains(method))
                return SpaceRTK.getInstance().actionsManager.execute(method, arguments.toArray());
        } catch (final InvalidArgumentsException e) {
            e.printStackTrace();
        } catch (final UnhandledActionException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static final int SO_BACKLOG = 128;
    private static final int SO_TIMEOUT = 30000; //30 seconds
    private final int    mode;
    private ServerSocket serverSocket = null;
    private Socket       socket;

    /**
     * Creates a new panel listener, listening for connections from a panel
     */
    public PanelListener() {
        mode = 0;
        start();
    }

    /**
     * Creates a new panel listener, listening for input from the socket
     * @param socket Socket to listen on
     */
    public PanelListener(final Socket socket) {
        mode = 1;
        this.socket = socket;
        start();
    }

    /**
     * Gets the mode the panel listener is in
     * 0 = Listening for opening connection from a panel
     * 1 = Listening for input from a socket
     * @return mode panel is in
     */
    public int getMode() {
        return mode;
    }

    @Override
    public void run() {
        if (mode == 0) {

            try {
                serverSocket = new ServerSocket(SpaceRTK.getInstance().rPort, SO_BACKLOG, InetAddress.getByName(SpaceRTK.getInstance().bindIp));
                serverSocket.setSoTimeout(SO_TIMEOUT);
            } catch(IOException e) {
                e.printStackTrace();
                return;
            }

            while (!serverSocket.isClosed()) {
                try {
                    final Socket clientSocket = serverSocket.accept();
                    new PanelListener(clientSocket);
                } catch(SocketTimeoutException e) {
                    // Do nothing.
                } catch (Exception e) {
                    if (!e.getMessage().contains("socket closed"))
                        e.printStackTrace();
                }
            }
        } else {
            try {
                final BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String string = input.readLine();
                if (string == null) {
                    return;
                }
                string = URLDecoder.decode(string, "UTF-8");
                string = string.substring(5, string.length() - 9);
                final PrintWriter output = new PrintWriter(socket.getOutputStream());
                if (string.startsWith("call") && string.contains("?method=") && string.contains("&args=")) {
                    final String method = string.substring(12, string.indexOf("&args="));
                    if (string.contains("&key=" + Utilities.crypt(method + SpaceRTK.getInstance().salt))) {
                        if (string.startsWith("call?method=DOWNLOAD_WORLD")) {
                            final boolean wasRunning = RemoteToolkit.isRunning();
                            if (wasRunning)
                                RemoteToolkit.hold();
                            final File file = new File(string.split("\"")[1] + ".zip");
                            ZIP.zip(file, new File(string.split("\"")[1]));
                            if (file.exists()) {
                                final FileInputStream fileInputStream = new FileInputStream(file);
                                final byte[] fileData = new byte[65536];
                                int length;
                                output.println("HTTP/1.1 200 OK");
                                output.println("Content-Type: application/force-download; name=" + file.getName());
                                output.println("Content-Transfer-Encoding: binary");
                                output.println("Content-Length:" + file.length());
                                output.println("Content-Disposition: attachment; filename=" + file.getName());
                                output.println("Expires: 0");
                                output.println("Cache-Control: no-cache, must-revalidate");
                                output.println("Pragma: no-cache");
                                while ((length = fileInputStream.read(fileData)) > 0)
                                    output.print(new String(fileData, 0, length));
                                fileInputStream.close();
                            } else
                                output.println(Utilities.addHeader(null));
                            if (wasRunning)
                                RemoteToolkit.unhold();
                        } else {
                            final Object result = interpret(string);
                            if (result != null)
                                output.println(Utilities.addHeader(JSONValue.toJSONString(result)));
                            else
                                output.println(Utilities.addHeader(null));
                        }
                    } else
                        output.println(Utilities.addHeader("Incorrect Salt supplied. Access denied!"));
                } else if (string.startsWith("ping"))
                    output.println(Utilities.addHeader("Pong!"));
                else
                    output.println(Utilities.addHeader(null));
                output.flush();
                input.close();
                output.close();
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Gracefully stops the listener
     * @throws IOException If the socket cannot be closed
     */
    public void stopServer() throws IOException {
        if (serverSocket != null)
            serverSocket.close();
    }
}
