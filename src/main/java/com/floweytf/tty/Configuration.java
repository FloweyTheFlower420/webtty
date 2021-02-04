package com.floweytf.tty;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.HashMap;

public class Configuration {
    private HashMap<String, HashMap<String, String>> tty;


    public String getTTY(String s) {
        String[] path = s.split("\\.", 2);
        return tty.get(path[0]).get(path[1]);
    }

    public void getConfig(String path) {
        try {
            HashMap<String, HashMap<String, String>> tty = new HashMap<>();
            JsonArray arr = new JsonParser().parse(new InputStreamReader(new FileInputStream(path))).getAsJsonArray();

            for (JsonElement e : arr) {
                String name = e.getAsJsonObject().get("name").getAsString();
                HashMap<String, String> ttys = new HashMap<>();
                for(JsonElement el : e.getAsJsonObject().get("ports").getAsJsonArray()) {
                    ttys.put(el.getAsJsonObject().get("name").getAsString(), el.getAsJsonObject().get("tty").getAsString());
                }

                tty.put(name, ttys);
            }

            this.tty = tty;
        } catch (Exception e) {
            Main.logger.fatal(-1, "Failed to load/parse config", e);
        }
    }
}
