package com.floweytf.tty;

import com.floweytf.betterlogger.BetterLogger;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import java.io.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

public class Session {
    private final Thread runner;
    private final ReentrantLock mutex = new ReentrantLock();
    private final InputStream reader;
    private final OutputStream writer;
    private final AtomicBoolean isEnable = new AtomicBoolean(true);
    private final AtomicBoolean isCrashed = new AtomicBoolean(false);
    private String readBuffer = "";
    private BetterLogger logger;

    public Session(String tty) throws SessionException {
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
                    mutex.lock();
                    readBuffer += (char)data;
                    mutex.unlock();
                }
            }
        }
        catch (IOException e) {
            mutex.lock();
            isCrashed.set(true);
            readBuffer = e.getMessage();
            mutex.unlock();
        }
    }

    public void write(@NotNull String s) throws SessionException {
        mutex.lock();
        try {
            if(isCrashed.get())
                throw new SessionException(readBuffer);
            writer.write(s.getBytes());
        }
        catch (IOException e) {
            mutex.unlock();
            throw new SessionException("Underlying IO failed!" + e.getMessage());
        }
        mutex.unlock();
    }

    public String getBuf() throws SessionException {
        mutex.lock();
        if(isCrashed.get())
            throw new SessionException(readBuffer);

        String ret = readBuffer;
        readBuffer = "";
        mutex.unlock();
        return ret;
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
