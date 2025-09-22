package jangalang.engine.gamemode;

import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import jangalang.engine.GameState;
import javax.swing.JPanel;
import java.awt.*;

import jangalang.engine.Game;

public class PauseState implements GameMode {
    @Override
    public void update() {}

    @Override
    public void render(JPanel window, Graphics g) {
        window.setBackground(Color.BLACK);

        String title = "Paused";
        String[] options = {
            "1. Resume Game",
            "2. Return to Main Menu"
        };

        g.setFont(new Font("Arial", Font.BOLD, 36));
        g.setColor(Color.WHITE);

        FontMetrics fm = g.getFontMetrics();
        int titleWidth = fm.stringWidth(title);
        int x = (window.getWidth() - titleWidth) / 2;
        int y = window.getHeight() / 4;
        g.drawString(title, x, y);

        g.setFont(new Font("Arial", Font.PLAIN, 24));
        fm = g.getFontMetrics();
        int lineHeight = fm.getHeight();
        int startY = window.getHeight() / 2;

        for (int i = 0; i < options.length; i++) {
            String option = options[i];
            int optionWidth = fm.stringWidth(option);
            int optionX = (window.getWidth() - optionWidth) / 2;
            int optionY = startY + (i * lineHeight * 2);
            g.drawString(option, optionX, optionY);
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_1 -> Game.setGameState(GameState.PLAYING);
            case KeyEvent.VK_2 -> Game.setGameState(GameState.MAIN_MENU);
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {}

    @Override
    public void mouseMoved(int e) {}

    @Override
    public void mouseClicked(MouseEvent e) {}

}
