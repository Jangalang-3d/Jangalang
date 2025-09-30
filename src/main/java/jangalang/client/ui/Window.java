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
        installInputs();
        frame.setVisible(true);
        // repaint loop
        new Timer(1000 / ApplicationProperties.getInt("game.fps"), ev -> {
            renderer.repaint();
            game.update();
        }).start();
    }

    private void installInputs() {
        renderer.setFocusable(true);
        renderer.addMouseMotionListener(new MouseAdapter() {
            @Override public void mouseMoved(MouseEvent e) {
                // compute dx relative to centre
                Point onScreen = renderer.getLocationOnScreen();
                int cx = onScreen.x + renderer.getWidth() / 2;
                int dx = e.getXOnScreen() - cx;
                game.mouseMoved(dx);
                try {
                    Robot r = new Robot();
                    r.mouseMove(cx, onScreen.y + renderer.getHeight()/2);
                } catch (Exception ignored) {}
            }
        });

        renderer.addKeyListener(new KeyAdapter() {
            // private final Set<Integer> pressed = new HashSet<>();
            @Override public void keyPressed(KeyEvent e) {
                // if (pressed.contains(e.getKeyCode())) return;
                // pressed.add(e.getKeyCode());
                // boolean fw = pressed.contains(KeyEvent.VK_W);
                // boolean bk = pressed.contains(KeyEvent.VK_S);
                // boolean lf = pressed.contains(KeyEvent.VK_A);
                // boolean rt = pressed.contains(KeyEvent.VK_D);

                // game.sendInput(fw, bk, lf, rt, 0.0);

                game.keyPressed(e);
            }
            @Override public void keyReleased(KeyEvent e) {
                // pressed.remove(e.getKeyCode());
                // boolean fw = pressed.contains(KeyEvent.VK_W);
                // boolean bk = pressed.contains(KeyEvent.VK_S);
                // boolean lf = pressed.contains(KeyEvent.VK_A);
                // boolean rt = pressed.contains(KeyEvent.VK_D);
                // game.sendInput(fw, bk, lf, rt, 0.0);

                game.keyReleased(e);
            }
        });
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
