package jangalang.engine.gamemode;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;

import jangalang.engine.GameState;
import jangalang.engine.Game;

public class MainMenuState implements GameMode {
    private static final int splashScreenIndx = (int)(Math.random() * 10) % 3;
    @Override
    public void update() {
    }

    @Override
    public void render(JPanel window, Graphics g) {
        window.setBackground(Color.BLACK); // Fallback
        BufferedImage splashScreen = Game.splashScreens[splashScreenIndx];
        g.drawImage(splashScreen, 0, 0, window.getWidth(), window.getHeight(), null);

        String[] options = {
            "1. Start Game",
            "2. Controls/Info",
            "3. Quit"
        };

        FontMetrics fm = g.getFontMetrics();
        g.setFont(new Font("Arial", Font.BOLD, 24));
        g.setColor(Color.WHITE);
        fm = g.getFontMetrics();
        int lineHeight = fm.getHeight();
        int startY = (int)(window.getHeight() * 0.7);

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
            case KeyEvent.VK_2 -> Game.setGameState(GameState.HELP);
            case KeyEvent.VK_3 -> System.exit(0);
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {}

    @Override
    public void mouseMoved(int e) {}

    @Override
    public void mouseClicked(MouseEvent e) {}

}
