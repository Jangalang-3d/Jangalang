package jangalang.engine;

import java.awt.image.BufferedImage;

import jangalang.engine.gamemode.GameMode;
import jangalang.engine.gamemode.HelpState;
import jangalang.engine.gamemode.MainMenuState;
import jangalang.engine.gamemode.PauseState;
import jangalang.engine.gamemode.PlayingState;
import jangalang.game.Player;
import jangalang.common.maps.MapData;
import jangalang.common.maps.MapLoader;
import jangalang.common.types.Pair;

public class Game {
    private static GameMode gameMode = new MainMenuState();
    private static MapData gameMap = MapLoader.parseMap("/maps/example.map");
    private static final Pair<Double, Double> spawn = gameMap.getSpawns().getFirst();
    private static Player player = new Player(spawn.getKey(), spawn.getValue());

    public static BufferedImage floorTexture;
    public static BufferedImage wallTexture;
    public static BufferedImage[] weaponSprite;
    public static BufferedImage[] splashScreens;

    public static void setGameState(GameState state) {
        switch (state) {
            case MAIN_MENU -> gameMode = new MainMenuState();
            case PLAYING -> gameMode = new PlayingState();
            case PAUSED -> gameMode = new PauseState();
            case HELP -> gameMode = new HelpState();
        }
    }

    public static GameMode getMode() {
        return gameMode;
    }

    public static Player getPlayer() {
        return player;
    }

    public static MapData getMap() {
        return gameMap;
    }
}
