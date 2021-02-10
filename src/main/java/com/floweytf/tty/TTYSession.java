package com.floweytf.tty;

import com.floweytf.betterlogger.BetterLogger;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.eclipse.jetty.websocket.api.Session;

import java.io.*;

public class TTYSession {
    private final Thread runner;
    private final InputStream reader;
    private final OutputStream writer;
    private BetterLogger logger;
    private final Session websock;
    private final String tty;

    public TTYSession(String tty, Session session) throws SessionException {
        runner = new Thread(this::poll);
        runner.setName("session-" + runner.getId());
        try {
            logger = new BetterLogger(Main.logger);
            logger.loggerName += "/" + runner.getName() + "-init";

            File file = new File(tty);
            logger.info("opening tty: "  + tty);
            reader = new FileInputStream(file);
            writer = new FileOutputStream(file, true);
            logger.info("tty has been opened successfully!");
            this.websock = session;
            this.tty = tty;
            runner.start();
        }
        catch (FileNotFoundException e) {
            logger.error("Failed to open tty!", (Throwable)e);
            throw new SessionException("Failed to open tty!");
        }

        logger.loggerName = Main.logger.loggerName + "/" + runner.getName();
    }

    @ApiStatus.Internal
    public void poll() {
        logger.info("starting poll loop");
        try {
            int data;
            while (true) {
                if(reader.available() > 0) {
                    data = reader.read();
                    if (data != -1) {
                        websock.getRemote().sendString(Character.toString((char) data));
                    }
                }
            }
        }
        catch (IOException e) {
            websock.close(500, e.getMessage());
        }
        catch (Exception e) {
            // we chill
        }
        logger.info("Poll loop ended");
    }

    public void write(@NotNull String s) throws SessionException {
        try {
            writer.write(s.getBytes());
        }
        catch (IOException e) {
            throw new SessionException("Underlying IO failed!" + e.getMessage());
        }
    }

    public String getTTY() {
        return tty;
    }

    public void finalize() {
        close();
    }

    public void close() {
        runner.interrupt();
        try {
            reader.close();
            writer.close();
        }
        catch (IOException e) {
            // ignore
        }

        // wait for it to actually close
        while(runner.isAlive());
    }
}
