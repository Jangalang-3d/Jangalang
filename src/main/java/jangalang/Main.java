package jangalang;

import jangalang.engine.Window;
import jangalang.engine.Renderer;
import jangalang.engine.GameLoop;
import jangalang.engine.KeyScanner;
import jangalang.engine.MouseScanner;

import java.awt.image.BufferedImage;
import java.util.ArrayList;

import javax.imageio.ImageIO;

import jangalang.engine.Game;

class Main {
    private static Renderer renderer;
    private static KeyScanner keyScanner;
    private static MouseScanner mouseScanner;

    public static void main(String[] args) {
        try {
            Game.floorTexture = ImageIO.read(Main.class.getResourceAsStream("/sprites/walls/wall1.png"));
            Game.wallTexture = ImageIO.read(Main.class.getResourceAsStream("/sprites/ground/Brick_03.png"));

            ArrayList<BufferedImage> weaponSpriteList = new ArrayList<>();
            weaponSpriteList.add(ImageIO.read(Main.class.getResourceAsStream("/sprites/guns/pistol/2PISA0.png")));
            weaponSpriteList.add(ImageIO.read(Main.class.getResourceAsStream("/sprites/guns/pistol/2PISB0.png")));
            weaponSpriteList.add(ImageIO.read(Main.class.getResourceAsStream("/sprites/guns/pistol/2PISD0.png")));
            weaponSpriteList.add(ImageIO.read(Main.class.getResourceAsStream("/sprites/guns/pistol/2PISF0.png")));
            Game.weaponSprite = weaponSpriteList.toArray(new BufferedImage[0]);
        } catch (Exception e) {
            e.printStackTrace();
        }

        renderer = new Renderer();
        keyScanner = new KeyScanner();
        mouseScanner = new MouseScanner();

        new Window(renderer, keyScanner, mouseScanner);
        GameLoop.run();
    }
}
