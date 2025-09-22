package jangalang.engine;

import java.util.Timer;
import java.util.TimerTask;
import jangalang.util.GameProperties;

public class GameLoop {
    private static Timer deltaTime;
    private static final long tickRate = 1000 / GameProperties.getInt("game.tps");

    private static Timer renderTime;
    private static final long frameRate = 1000 / GameProperties.getInt("game.fps");

    public static void run() {
        deltaTime = new Timer();
        deltaTime.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                Game.getMode().update();
            }
        }, 0, tickRate);

        renderTime = new Timer();
        renderTime.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Window.getRenderer().repaint();
            }
        }, 0, frameRate);
    }

}
