package jangalang.engine.gamemode;

import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import javax.swing.JPanel;

public interface GameMode {
    void update();
    void render(JPanel window, Graphics g);
    void keyPressed(KeyEvent e);
    void keyReleased(KeyEvent e);
    void mouseClicked(MouseEvent e);
    void mouseMoved(int e);
}
