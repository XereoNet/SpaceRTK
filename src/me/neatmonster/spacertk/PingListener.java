/*
 * This file is part of SpaceModule (http://spacebukkit.xereo.net/).
 *
 * SpaceModule is free software: you can redistribute it and/or modify it under the terms of the
 * Attribution-NonCommercial-ShareAlike Unported (CC BY-NC-SA) license as published by the Creative
 * Common organization, either version 3.0 of the license, or (at your option) any later version.
 *
 * SpaceBukkit is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Attribution-NonCommercial-ShareAlike Unported (CC BY-NC-SA) license for more details.
 *
 * You should have received a copy of the Attribution-NonCommercial-ShareAlike Unported (CC BY-NC-SA)
 * license along with this program. If not, see <http://creativecommons.org/licenses/by-nc-sa/3.0/>.
 */
package me.neatmonster.spacertk;

import me.neatmonster.spacertk.RemoteToolkit;
import me.neatmonster.spacertk.SpaceRTK;

import java.io.*;
import java.net.*;
import java.util.Timer;
import java.util.TimerTask;


/**
 * Periodically pings SpaceBukkit and prints a warning if there is no response.
 * @author Jamy
 * @author Drdanick
 */
public class PingListener {

    private static final int TIMEOUT = 1000;
    private static final int INITIAL_DELAY = 60 * 1000;
    private static final int PING_PERIOD = 30 * 1000;

    private Boolean running = false;
    private Timer timer;
    private TimerTask pingTask;

    private URL pingUrl;

    public PingListener() {
        InetAddress hostAddress = SpaceRTK.getInstance().spaceModule.bindAddress;
        try {
            if(hostAddress.isAnyLocalAddress())
                hostAddress = InetAddress.getLocalHost();

            pingUrl = new URL("http", hostAddress.getHostAddress(), SpaceRTK.getInstance().port, "/ping");

        } catch (UnknownHostException e) {
            System.err.println(
                    "Warning: Ping listener cannot find local host address.\n" +
                            "Disabling ping listener..."
            );
            return;

        } catch(MalformedURLException e) {
            System.err.println(
                    "Warning: Ping URL could not be resolved: "+e.getMessage()+"\n" +
                            "Disabling ping listener..."
            );
            return;
        }

        timer = new Timer("SpaceBukkit PingListener", true);

    }

    /**
     * Starts the ping task.
     */
    public void start() {
        if(!running) {
            running = true;
            pingTask = new PingTask();
            timer.scheduleAtFixedRate(pingTask, INITIAL_DELAY, PING_PERIOD);
        }
    }

    /**
     * Disables the ping task, but does not stop the underlying timer.
     */
    public void stop() {
        if(pingTask != null)
            pingTask.cancel();
        timer.purge();
        running = false;
    }

    /**
     * Permanently disables the underlying timer.
     * If the timer is currently running, stop() will be called before the timer is disabled.
     */
    public void shutdown() {
        if(running)
            stop();
        timer.cancel();
    }

    private class PingTask extends TimerTask {

        /**
         * Pings the SpaceBukkit plugin and prints a warning if it could not be reached.
         */
        public void run() {

            if (RemoteToolkit.isRunning()) {
                URLConnection      urlConn = null;
                BufferedReader br = null;
                try {
                    urlConn = pingUrl.openConnection();

                    urlConn.setConnectTimeout(TIMEOUT);
                    urlConn.setReadTimeout(TIMEOUT);
                    urlConn.setDoInput(true);
                    urlConn.setUseCaches(false);

                    urlConn.connect();
                    br = new BufferedReader(new InputStreamReader(urlConn.getInputStream()));

                    String read = br.readLine();

                    if(read == null || !read.trim().equals("Pong!"))
                        System.err.println("Warning: SpaceBukkit failed to respond to ping request. Is the plugin running?");

                } catch (MalformedURLException e) {
                    System.err.println("Unexpected exception: "+e);
                    e.printStackTrace();
                } catch(SocketTimeoutException e) {
                    System.err.println("Warning: SpaceBukkit failed to respond to ping request. Is the plugin running?");
                } catch (IOException e) {
                    System.err.println("Unexpected exception: "+e);
                    e.printStackTrace();
                }finally {
                    try {
                        if(br != null)
                            br.close();
                    } catch(IOException e) {}
                }
            }

        }

    }
}
