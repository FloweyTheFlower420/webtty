package com.floweytf.tty;

import com.google.gson.JsonSyntaxException;
import org.eclipse.jetty.websocket.api.annotations.*;
import org.eclipse.jetty.websocket.api.Session;

import java.util.UUID;

@WebSocket
public class SocketServer {
    public static class ConnectionHeartbeat {
        String uuid;
        String buffer;
    }

    @OnWebSocketConnect
    public void onConnect(Session user) throws Exception {

    }

    @OnWebSocketClose
    public void onClose(Session user, int statusCode, String reason) {
    }

    @OnWebSocketMessage
    public void onMessage(Session user, String message) {
        try {
            ConnectionHeartbeat heartbeat = Main.gson.fromJson(message, ConnectionHeartbeat.class);
            user.getRemote().sendString(Main.sessions.heartbeat(UUID.fromString(heartbeat.uuid), heartbeat.buffer));
        }
        catch (SessionException | JsonSyntaxException e) {
            user.getRemote().sendString(e.getMessage());
        } finally {
            return;
        }
    }

}