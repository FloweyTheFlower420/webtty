package com.floweytf.tty;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Configuration {
    private HashMap<String, HashMap<String, String>> tty;


    public String getTTY(String s) {
        String[] path = s.split("\\.", 2);
        return tty.get(path[0]).get(path[1]);
    }

    public void getConfig(String path) {
        Runtime r = Runtime.getRuntime();
        List<Process> processList = new ArrayList<>();
        try {
            HashMap<String, HashMap<String, String>> tty = new HashMap<>();
            JsonArray arr = new JsonParser().parse(new InputStreamReader(new FileInputStream(path))).getAsJsonArray();

            for (JsonElement e : arr) {
                String name = e.getAsJsonObject().get("name").getAsString();
                HashMap<String, String> ttys = new HashMap<>();
                for(JsonElement el : e.getAsJsonObject().get("ports").getAsJsonArray()) {
                    ttys.put(el.getAsJsonObject().get("name").getAsString(), el.getAsJsonObject().get("tty").getAsString());
                    processList.add(r.exec("stty -F " + el.getAsJsonObject().get("tty").getAsString() + " " + el.getAsJsonObject().get("baud").getAsInt() +
                            "1>/dev/null 2>&1"));
                }

                tty.put(name, ttys);
            }

            for (Process p : processList) {
                if(p.waitFor() != 0) {
                    Main.logger.fatal(-1, "Unable to set baud rate");
                }
            }

            this.tty = tty;
        } catch (Exception e) {
            Main.logger.fatal(-1, "Failed to load/parse config", e);
        }
    }
}
