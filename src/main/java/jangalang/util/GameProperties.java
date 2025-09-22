package jangalang.util;

import java.util.Properties;
import java.io.IOException;
import java.io.InputStream;

public class GameProperties {
    private static final Properties gameProps = new Properties();

    static {
        try (InputStream in = GameProperties.class
                .getClassLoader()
                .getResourceAsStream("application.properties")) {

            if (in == null) {
                throw new IllegalStateException("application.properties not found on classpath");
            }

            gameProps.load(in);
            gameProps.setProperty("os.name", System.getProperty("os.name")); // set OS
        } catch (IOException e) {
            throw new ExceptionInInitializerError("Failed to load application.properties: " + e.getMessage());
        }
    }

    private GameProperties() {}

    public static String get(String key) {
        return gameProps.getProperty(key);
    }

    public static int getInt(String key) {
        return Integer.parseInt(gameProps.getProperty(key));
    }

    public static double getDouble(String key) {
        return Double.parseDouble(gameProps.getProperty(key));
    }

    public static String getOrDefault(String key, String defaultValue) {
        return gameProps.getProperty(key, defaultValue);
    }
}
