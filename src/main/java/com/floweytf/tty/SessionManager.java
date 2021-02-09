package com.floweytf.tty;

import org.eclipse.jetty.websocket.api.Session;

import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class SessionManager {
    private final ConcurrentHashMap<Session, TTYSession> sessions = new ConcurrentHashMap<>();
    private final HashSet<String> ttyInUse = new HashSet<>();
    private final HashMap<Session, String> crashed = new HashMap<>();
    private final AtomicBoolean cleanupStatus = new AtomicBoolean(true);

    public SessionManager() {

    }

    public void newSession(String tty, Session s) throws SessionException {
        // starts a new session
        if(ttyInUse.contains(tty))
            throw new SessionException("TTY is already in use!");

        sessions.put(s, new TTYSession(tty, s));

        ttyInUse.add(tty);
    }

    public void write(Session s, String data) throws SessionException {
        if(crashed.containsKey(s))
            throw new SessionException(crashed.get(s));
        if(!sessions.containsKey(s))
            throw new SessionException("Session has been lost/invalid session ID!");
        sessions.get(s).write(data);
    }

    public void closeSession(Session s) {
        sessions.get(s).close();
        sessions.remove(s);
    }

    public void finalize() {
        close();
    }

    public void close() {
        cleanupStatus.set(false);
        sessions.forEach((key, value) -> value.close());
    }
}
