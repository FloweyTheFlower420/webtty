package com.floweytf.tty;

import com.floweytf.betterlogger.BetterLogger;
import com.floweytf.betterlogger.ConsoleTransport;
import com.floweytf.betterlogger.FileTransport;
import com.google.gson.Gson;
import org.eclipse.jetty.util.log.Log;

import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import static spark.Spark.*;

class ConnectionInit {
    String tty;
    String device;
}

public class Main {
    static BetterLogger logger;
    static SessionManager sessions;
    static Gson gson = new Gson();

    public static void main(String[] args) throws SessionException {
        logger = new BetterLogger("ttyserver")
                .addTransport(new ConsoleTransport(BetterLogger.INFO))
                .addTransport(new FileTransport("logs/" + DateTimeFormatter.ofPattern("MM-dd-yyyy_hh-mm-ss").format(ZonedDateTime.now()) + ".log", BetterLogger.DEBUG));

        Log.setLog(new BetterLogger(logger, Log.class));
        logger.info("setting up server...");
        sessions = new SessionManager();

        // load config
        logger.info("running at path {}", Paths.get(".").toAbsolutePath().normalize().toString());
        Configuration config = new Configuration();
        config.getConfig("tty.json");

        port(16383);

        webSocket("/device/heartbeat", SocketServer.class);

        get("/", (request, response) -> {
            return "A";
        });

        post("/device/create", (req, res) -> {
            // attempt to open a tty
            logger.info("creating new session...");
            ConnectionInit packet = gson.fromJson(req.body(), ConnectionInit.class);
            UUID uid = sessions.newSession(config.getTTY(packet.device + '.' + packet.tty));
            res.body("{\"id\": \"" + uid.toString() + "\"}");
            res.header("Content-Type", "application/json");
            logger.info("created session (" + uid.toString() + ") with dev " + packet.device + ":" + packet.tty);
            return res;
        });

        post("/device/close", (req, res) -> {
            logger.info("closing session: " + req.body());
            sessions.closeSession(UUID.fromString(req.body()));
            return res;
        });

        exception(Exception.class, (e, req, res) -> {
            logger.error("internal server error!", e);
            res.status(500);
            res.body("Internal server error");
        });
    }
}
