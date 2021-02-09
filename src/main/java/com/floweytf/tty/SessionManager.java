package com.floweytf.tty;

import com.floweytf.betterlogger.BetterLogger;
import org.jetbrains.annotations.ApiStatus;

import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class SessionManager {
    public static class SessionMeta {
        public TTYSession session;
        public Long lastConnect;
    }
    public static final int HEARTBEAT_MAX_TIME = 1000;
    private final ConcurrentHashMap<UUID, SessionMeta> sessions = new ConcurrentHashMap<>();
    private final HashSet<String> ttyInUse = new HashSet<>();
    private final HashMap<UUID, String> crashed = new HashMap<>();
    Thread oldSessionCleanup = new Thread(this::cleanup);
    private final AtomicBoolean cleanupStatus = new AtomicBoolean(true);
    private final ExecutorService threadpool = Executors.newCachedThreadPool();

    public SessionManager() {
        oldSessionCleanup.setName("cleanup-" + oldSessionCleanup.getId());
        oldSessionCleanup.start();
    }

    public UUID newSession(String tty) throws SessionException {
        // starts a new session
        if(ttyInUse.contains(tty))
            throw new SessionException("TTY is already in use!");

        UUID newId = UUID.randomUUID();
        sessions.put(newId, new SessionMeta() {{
            lastConnect = System.currentTimeMillis();
            session = new TTYSession(tty);
        }});

        ttyInUse.add(tty);
        return newId;
    }

    public String heartbeat(UUID uid, String data) throws SessionException {
        if(crashed.containsKey(uid))
            throw new SessionException(crashed.get(uid));
        if(!sessions.containsKey(uid))
            throw new SessionException("Session has been lost/invalid session ID!");
        sessions.get(uid).session.write(data);
        return sessions.get(uid).session.getBuf();
    }

    @ApiStatus.Internal
    public void cleanup() {
        if(Main.cmd.hasOption("no-timeout"))
            return;

        final BetterLogger logger = new BetterLogger(Main.logger) {{
            loggerName = "SessionManger/" + Thread.currentThread().getName();
        }};

        while(cleanupStatus.get()) {
            try {
                sessions.forEach((key, value) -> {
                    if (System.currentTimeMillis() - value.lastConnect > HEARTBEAT_MAX_TIME) {
                        threadpool.submit(() -> sessions.remove(key));
                    }
                });

                Thread.sleep(10);
            }
            catch (Exception e) {
                logger.fatal(26, "cleanup thread has somehow errored!", e);
            }
        }
    }

    public void closeSession(UUID uid) {
        sessions.get(uid).session.close();
        sessions.remove(uid);
    }

    public void finalize() {
        close();
    }

    public void close() {
        cleanupStatus.set(false);
        sessions.forEach((key, value) -> value.session.close());
        while(oldSessionCleanup.isAlive());
    }
}
