package jangalang.client.scanners;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import jangalang.client.ClientGame;

public class KeyScanner extends KeyAdapter {
    private final ClientGame game;
    public KeyScanner(ClientGame game) {
        this.game = game;
        System,out,println("KeyScanner initialized");
    }

    @Override
    public void keyPressed(KeyEvent e) {
        game.keyPressed(e);
    }

    @Override
    public void keyReleased(KeyEvent e) {
        game.keyReleased(e);
    }
}
