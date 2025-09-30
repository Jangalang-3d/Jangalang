package jangalang.client.ui;

import java.awt.Point;
import java.awt.Robot;
import java.awt.Graphics;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.Timer;

import jangalang.client.ClientGame;
import jangalang.client.scanners.KeyScanner;
import jangalang.client.scanners.MouseScanner;
import jangalang.common.ApplicationProperties;

public class Window {
    private final JFrame frame;
    private final RendererPanel renderer;
    private final ClientGame game;

    public Window(ClientGame game) {
        this.game = game;
        frame = new JFrame("Jangalang (multiplayer)");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        int w = Integer.parseInt(System.getProperty("window.width", "1280"));
        int h = w * 9 / 12;
        frame.setSize(w,h);
        frame.setLocationRelativeTo(null);
        renderer = new RendererPanel(game);
        frame.getContentPane().add(renderer);
        frame.setVisible(true);
        installInputs();
        // repaint loop
        new Timer(1000 / ApplicationProperties.getInt("game.fps"), ev -> {
            renderer.repaint();
            game.update();
        }).start();
    }

    private void installInputs() {
        renderer.setFocusable(true);
        MouseScanner mScanner = new MouseScanner(renderer, game);
        renderer.addMouseListener(mScanner);
        renderer.addMouseMotionListener(mScanner);
        renderer.addKeyListener(new KeyScanner(game));
       }

    static class RendererPanel extends JPanel {
        private final ClientGame game;
        RendererPanel(ClientGame g) {
            this.game = g;
            setFocusable(true);
            requestFocusInWindow();
        }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            game.render(this, g);
        }
    }
}
