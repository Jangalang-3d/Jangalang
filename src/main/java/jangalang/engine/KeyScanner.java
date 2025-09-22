package jangalang.engine;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class KeyScanner implements KeyListener {

    @Override
    public void keyPressed(KeyEvent e) {
        Game.getMode().keyPressed(e);
    }

    @Override
    public void keyReleased(KeyEvent e) {
        Game.getMode().keyReleased(e);
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }
}
