package jangalang.client;

import java.util.ArrayList;
import java.util.HashMap;

import jangalang.common.maps.MapData;

import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;

public class ResourceLoader {
    public static ArrayList<BufferedImage> wallTextures;
    public static ArrayList<BufferedImage> floorTextures;
    public static ArrayList<BufferedImage> weaponSprites;
    public static ArrayList<BufferedImage> splashScreens;
    public static ArrayList<MapData> maps;

    public static void load() {
        try {
            String locManifest = new String(ResourceLoader.class.getResourceAsStream("/manifest.txt").readAllBytes());
            HashMap<String, String> locs = parseLocations(locManifest);

            wallTextures = loadImageResource(loadManifest(locs.get("walls")));
            floorTextures = loadImageResource(loadManifest(locs.get("floors")));
            weaponSprites = loadImageResource(loadManifest(locs.get("weapon")));
            splashScreens = loadImageResource(loadManifest(locs.get("splash")));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static ArrayList<BufferedImage> loadImageResource(String[] paths) throws IOException {
        ArrayList<BufferedImage> result = new ArrayList<>();
        for (String path : paths) {
            result.add(ImageIO.read(ResourceLoader.class.getResourceAsStream(path)));
        }
        return result;
    }

    private static String[] loadManifest(String path) throws IOException {
        String contents = new String(ResourceLoader.class.getResourceAsStream(path).readAllBytes());
        ArrayList<String> result = new ArrayList<>();

        for (String line : contents.split("\n")) {
            result.add(path.replace("manifest.txt", "") + line);
        }

        return (String[]) result.toArray(new String[0]);
    }

    private static HashMap<String, String> parseLocations(String str) throws Exception {
        Exception corruptMF = new Exception("Corrupted manifest");
        if (str.isBlank() || str.isEmpty()) throw corruptMF;

        String[] lines = str.split("\n");
        if (lines.length < 4) throw corruptMF;

        HashMap<String, String> result = new HashMap<>();
        for (String s : lines) {
            if (!s.contains(":")) throw corruptMF;

            String[] keyValue = s.split(":");
            result.put(keyValue[0], keyValue[1]);
        }

        return result;
    }
}
