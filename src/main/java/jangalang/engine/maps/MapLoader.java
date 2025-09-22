package jangalang.engine.maps;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

public class MapLoader {
    public static Map parseMap(String file) {
        Map result = new Map();
        JSONParser parser = new JSONParser();

        try (
             InputStream in = MapLoader.class.getResourceAsStream(file);
             Reader reader = new InputStreamReader(in)
            ) {
            JSONObject jsonObject = (JSONObject) parser.parse(reader);

            // Parse the "spawn" array
            JSONArray spawnArray = (JSONArray) jsonObject.get("spawn");
            for (Object spawnObj : spawnArray) {
                JSONObject spawn = (JSONObject) spawnObj;
                double x = ((Number) spawn.get("x")).doubleValue();
                double y = ((Number) spawn.get("y")).doubleValue();
                result.addSpawn(x, y);
            }

            // Parse the "walls" array
            JSONArray wallsArray = (JSONArray) jsonObject.get("walls");
            for (Object wallObj : wallsArray) {
                JSONObject wall = (JSONObject) wallObj;

                double x1 = ((Number) wall.get("x1")).doubleValue();
                double y1 = ((Number) wall.get("y1")).doubleValue();
                double x2 = ((Number) wall.get("x2")).doubleValue();
                double y2 = ((Number) wall.get("y2")).doubleValue();

                result.addWall(new Wall(x1, y1, x2, y2));
            }
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }

        return result;
    }
}
