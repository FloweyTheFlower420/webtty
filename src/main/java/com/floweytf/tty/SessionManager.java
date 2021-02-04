package com.floweytf.tty;

import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;

public class SessionManager {
    private final HashMap<UUID, Session> sessions = new HashMap<>();
    private final HashSet<String> ttyInUse = new HashSet<>();
    private final HashMap<UUID, String> crashed = new HashMap<>();

    public SessionManager() {

    }

    public UUID newSession(String tty) throws SessionException {
        // starts a new session
        if(ttyInUse.contains(tty))
            throw new SessionException("TTY is already in use!");

        UUID newId = UUID.randomUUID();
        sessions.put(newId, new Session(tty));
        ttyInUse.add(tty);
        return newId;
    }

    public String heartbeat(UUID uid, String data) throws SessionException {
        if(crashed.containsKey(uid))
            throw new SessionException(crashed.get(uid));
        if(!sessions.containsKey(uid))
            throw new SessionException("Session has been lost/invalid UID!");
        sessions.get(uid).write(data);
        return sessions.get(uid).getBuf();
    }

    public void finalize() {
        close();
    }

    public void close() {
        sessions.forEach((key, value) -> { value.close(); });
    }
}
