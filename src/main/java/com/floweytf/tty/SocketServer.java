package com.floweytf.tty;

import com.google.gson.JsonSyntaxException;
import org.eclipse.jetty.websocket.api.annotations.*;
import org.eclipse.jetty.websocket.api.Session;

import java.util.UUID;

@WebSocket
public class SocketServer {
    public static class ConnectionHeartbeat {
        String id;
        String buffer;
    }

    @OnWebSocketConnect
    public void onConnect(Session user) throws Exception {
        Main.logger.debug("Websocket connected!");
    }

    @OnWebSocketClose
    public void onClose(Session user, int statusCode, String reason) {
        Main.logger.debug("Websocket disconnected!");
    }

    @OnWebSocketMessage
    public void onMessage(Session user, String message) {
        Main.logger.debug("Recevied websock connection: " + message);
        
        try {
            ConnectionHeartbeat heartbeat = Main.gson.fromJson(message, ConnectionHeartbeat.class);
            user.getRemote().sendString(Main.sessions.heartbeat(UUID.fromString(heartbeat.id), heartbeat.buffer));
        }
        catch (SessionException | JsonSyntaxException e) {
            user.getRemote().sendString(e.getMessage());
        } finally {
            return;
        }
    }

}
