package com.floweytf.tty;

import com.floweytf.betterlogger.BetterLogger;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.eclipse.jetty.websocket.api.Session;

import java.io.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

public class TTYSession {
    private final Thread runner;
    private final ReentrantLock mutex = new ReentrantLock();
    private final InputStream reader;
    private final OutputStream writer;
    private final AtomicBoolean isEnable = new AtomicBoolean(true);
    private final AtomicBoolean isCrashed = new AtomicBoolean(false);
    private String crashMessage = "";
    private BetterLogger logger;
    private Session websock;

    public TTYSession(String tty) throws SessionException {
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
        }
        catch (FileNotFoundException e) {
            logger.error("Failed to open tty!", (Throwable)e);
            throw new SessionException("Failed to open tty!");
        }

        logger.loggerName = Main.logger.loggerName + "/" + runner.getName();
    }

    public void start(Session session) {
        this.websock = session;
        runner.start();
    }

    @ApiStatus.Internal
    public void poll() {
        logger.info("starting poll loop");
        try {
            int data;
            while (isEnable.get()) {
                data = reader.read();
                if(data != -1) {
                    websock.getRemote().sendString(Character.toString((char) data));
                }
            }
        }
        catch (IOException e) {
            websock.close(500, e.getMessage());
        }
    }

    public void write(@NotNull String s) throws SessionException {
        try {
            writer.write(s.getBytes());
        }
        catch (IOException e) {
            throw new SessionException("Underlying IO failed!" + e.getMessage());
        }
    }

    public void finalize() {
        close();
    }

    public void close() {
        isEnable.set(false);
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
