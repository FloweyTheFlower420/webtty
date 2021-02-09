package com.floweytf.tty;

import com.floweytf.betterlogger.BetterLogger;
import com.google.gson.JsonSyntaxException;
import org.eclipse.jetty.websocket.api.annotations.*;
import org.eclipse.jetty.websocket.api.Session;

import java.util.UUID;

import static spark.Spark.halt;

@WebSocket
public class SocketServer {
    BetterLogger logger = new BetterLogger(Main.logger) {{
        loggerName = "socket-server";
    }};
    public static class SocketPayload {
        String buffer;

        String tty;
        String device;
        public int getType() {
            boolean t1 = buffer != null;
            boolean t2 = tty != null && device != null;

            if(t1 == t2)
                return -1;

            if(t1)
                return 1;
            return 2;
        }
    }

    static class ConnectionOpenResponse {
        String tty;
    }

    @OnWebSocketConnect
    public void onConnect(Session user) throws Exception {
        Main.logger.debug("Websocket connected!");
    }

    @OnWebSocketClose
    public void onClose(Session user, int statusCode, String reason) {
        Main.logger.debug("Websocket disconnected!");
        Main.sessions.closeSession(user);
    }

    @OnWebSocketMessage
    public void onMessage(Session user, String message) {
        Main.logger.debug("Recevied websock connection: " + message);
        
        try {
            SocketPayload heartbeat = Main.gson.fromJson(message, SocketPayload.class);
            switch (heartbeat.getType()) {
                case -1:
                    user.close(400, "Invalid payload!");
                    break;
                case 1:
                    Main.sessions.write(user, heartbeat.buffer);
                    break;
                case 2:
                    String s = Main.config.getTTY(heartbeat.device + '.' + heartbeat.tty);

                    // make sure that the tty exists in config
                    if(s == null)
                        user.close(400, "Invalid device/tty!");

                    // get UUID
                    Main.sessions.newSession(s, user);

                    logger.info("created session (" + user + ") with dev " + heartbeat.device + ":" + heartbeat.tty);
                    user.getRemote().sendString(Main.gson.toJson(new ConnectionOpenResponse() {{
                        tty = s;
                    }}, ConnectionOpenResponse.class));
                    break;
            }
        }
        catch (SessionException | JsonSyntaxException e) {
            user.getRemote().sendString(e.getMessage());
        } finally {
            return;
        }
    }

}
