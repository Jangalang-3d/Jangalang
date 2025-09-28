package jangalang.engine;

import java.util.concurrent.*;
import jangalang.common.ApplicationProperties;

public class GameLoop {
    private static final long tickRate = 1000 / ApplicationProperties.getInt("game.tps");
    private static final long frameRate = 1000 / ApplicationProperties.getInt("game.fps");

    private final static ScheduledExecutorService executor =
        Executors.newScheduledThreadPool(2);

    private volatile static boolean running = false;
    public static void run() {
        if (running) return;
        running = true;

        executor.scheduleAtFixedRate(() -> {
            try {
                Game.getMode().update();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 0, tickRate, TimeUnit.MILLISECONDS);

        executor.scheduleAtFixedRate(() -> {
            try {
                Window.getRenderer().repaint();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 0, frameRate, TimeUnit.MILLISECONDS);
    }

    public static void stop() {
        running = false;
        executor.shutdownNow();
    }
}
