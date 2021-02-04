package com.floweytf.tty;

import com.floweytf.betterlogger.BetterLogger;
import com.floweytf.betterlogger.ConsoleTransport;
import com.floweytf.betterlogger.FileTransport;
import com.google.gson.Gson;
import org.eclipse.jetty.util.log.Log;
import org.yaml.snakeyaml.Yaml;

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

    public static void main(String[] args) throws SessionException {
        logger = new BetterLogger("ttyserver")
                .addTransport(new ConsoleTransport(BetterLogger.INFO))
                .addTransport(new FileTransport("logs/" + DateTimeFormatter.ofPattern("MM-dd-yyyy_hh-mm-ss").format(ZonedDateTime.now()) + ".log", BetterLogger.DEBUG));

        Log.setLog(new BetterLogger(logger, Log.class));
        logger.info("setting up server...");

        SessionManager sessions = new SessionManager();
        Gson gson = new Gson();
        Yaml yaml = new Yaml();

        // load config
        logger.info("running at path {}", Paths.get(".").toAbsolutePath().normalize().toString());
        Configuration config = new Configuration();
        config.getConfig("tty.json");

        port(16383);

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

        exception(Exception.class, (exception, request, response) -> {
            //exception.printStackTrace();
        });
    }
}
