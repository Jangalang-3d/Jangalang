package jangalang.client.ui;

import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;

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
        final int w = ApplicationProperties.getInt("window.width");
        final int h = w * 9 / 12;
        frame.setSize(w,h);
        frame.setLocationRelativeTo(null);
        frame.setUndecorated(true);
        frame.setResizable(false);

        GraphicsDevice gd = GraphicsEnvironment
            .getLocalGraphicsEnvironment()
            .getDefaultScreenDevice();
        gd.setFullScreenWindow(frame);

        renderer = new RendererPanel(game);
        frame.getContentPane().add(renderer);
        frame.setVisible(true);
        installInputs();
        // repaint loop
        new Timer(1000 / ApplicationProperties.getInt("game.fps"), ev -> {
            renderer.repaint();
            game.update();
        }).start();

        if (ApplicationProperties.get("game.user.hidemouse").equals("true")) {
            Toolkit toolkit = Toolkit.getDefaultToolkit();
            BufferedImage cursorImg = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            Cursor blankCursor = toolkit.createCustomCursor(cursorImg, new Point(0, 0), "blank");
            frame.setCursor(blankCursor);
        }
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
