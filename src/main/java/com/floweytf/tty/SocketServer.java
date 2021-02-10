package com.floweytf.tty;

import com.floweytf.betterlogger.BetterLogger;
import com.google.gson.JsonSyntaxException;
import org.eclipse.jetty.websocket.api.annotations.*;
import org.eclipse.jetty.websocket.api.Session;

import java.io.IOException;
import java.util.UUID;

import static spark.Spark.halt;

@WebSocket
public class SocketServer {
    BetterLogger logger = new BetterLogger(Main.logger) {{
        loggerName = "socket-server";
    }};
    public static class ConnectionInit {
        String tty;
        String device;
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
            if (Main.sessions.contains(user)) {
                // treat as raw buffer
                Main.sessions.write(user, message);
            }
            else {
                ConnectionInit heartbeat = Main.gson.fromJson(message, ConnectionInit.class);
                String s = Main.config.getTTY(heartbeat.device + '.' + heartbeat.tty);

                // make sure that the tty exists in config
                if (s == null)
                    user.close(400, "Invalid device/tty!");

                // get UUID
                Main.sessions.newSession(s, user);

                logger.info("created session (" + user + ") with dev " + heartbeat.device + ":" + heartbeat.tty);
                user.getRemote().sendString(Main.gson.toJson(new ConnectionOpenResponse() {{
                    tty = s;
                }}, ConnectionOpenResponse.class));
            }
        }
        catch (SessionException e ) {
            user.close(500, e.getMessage());
        }
        catch (JsonSyntaxException e) {
            user.close(400, e.getMessage());
        }
        catch (IOException e) {
            // wtf
        }
    }

}
