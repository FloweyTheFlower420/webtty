package com.floweytf.tty;

import com.floweytf.betterlogger.BetterLogger;
import com.floweytf.betterlogger.ConsoleTransport;
import com.floweytf.betterlogger.FileTransport;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import org.eclipse.jetty.util.log.Log;
import org.apache.commons.cli.*;

import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static spark.Spark.*;



public class Main {
    // http error codes
    public static final int INVALID_REQUEST = 400;
    public static final int INTERNAL_SERVER_ERROR = 500;

    public static final int OK = 200;

    public static BetterLogger logger;
    public static SessionManager sessions;
    public static Gson gson = new Gson();
    public static CommandLine cmd;
    public static Configuration config;

    public static void main(String[] args) throws SessionException {
        cmd = CLIParser.parse(args);
        logger = new BetterLogger("ttyserver")
                .addTransport(new ConsoleTransport(cmd.hasOption('d') ? BetterLogger.DEBUG : BetterLogger.INFO))
                .addTransport(new FileTransport("logs/" + DateTimeFormatter.ofPattern("MM-dd-yyyy_hh-mm-ss").format(ZonedDateTime.now()) + ".log", BetterLogger.DEBUG));

        Log.setLog(new BetterLogger(logger, Log.class));
        logger.info("setting up server...");
        sessions = new SessionManager();

        // load config
        logger.info("running at path {}", Paths.get(".").toAbsolutePath().normalize().toString());
        if(cmd.hasOption("config"))
            config.getConfig(cmd.getOptionValue("config"));
        else
            config.getConfig("tty.json");

        threadPool(16);

        if (cmd.hasOption("spark-threads"))
            threadPool(Integer.parseInt(cmd.getOptionValue("spark-threads")));

        port(16383);

        webSocket("/device/heartbeat", SocketServer.class);

        staticFiles.location("/public");

        get("/api", (req, res) -> {
            res.redirect("/public/home/api.html");

            return res;
        });

        exception(Exception.class, (e, req, res) -> {
            logger.error("internal server error!", e);
            res.status(500);
            res.body("Internal server error");
        });

        awaitInitialization();

        String s = Utils.getIpv4();
        if(s != null)
            logger.info("server is online, at: " + s);
    }
}
