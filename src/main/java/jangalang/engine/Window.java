package jangalang.engine;

import javax.swing.JFrame;
import javax.swing.JComponent;
import jangalang.util.GameProperties;

import java.awt.Cursor;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;

public class Window {
    public static final int WIDTH = GameProperties.getInt("window.width");
    public static final int HEIGHT = WIDTH / 12 * 9;
    private static JFrame gameWindow;
    private static JComponent renderer;
    private static KeyListener keyScanner;
    private static MouseScanner mouseScanner;

    public Window(JComponent _renderer, KeyListener _keyScanner, MouseScanner _mouseScanner) {
        renderer = _renderer;
        keyScanner = _keyScanner;
        mouseScanner = _mouseScanner;
        gameWindow = this.createWindow();
    }

    private JFrame createWindow() {
        JFrame result = new JFrame();
        // result.setSize(WIDTH, HEIGHT);
        result.setResizable(false); // Frick u, we pick the window size
        result.setLocationRelativeTo(null);
        result.setTitle(GameProperties.get("game.name"));
        result.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        result.setUndecorated(true);

        GraphicsDevice gd = GraphicsEnvironment
            .getLocalGraphicsEnvironment()
            .getDefaultScreenDevice();
        gd.setFullScreenWindow(result);

        result.setVisible(true);

        result.add(renderer);
        result.addKeyListener(keyScanner);
        result.addMouseListener(mouseScanner);
        result.addMouseMotionListener(mouseScanner);
        result.validate();
        try {
            mouseScanner.init(renderer);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (GameProperties.get("game.user.hidemouse").equals("true")) {
            Toolkit toolkit = Toolkit.getDefaultToolkit();
            BufferedImage cursorImg = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            Cursor blankCursor = toolkit.createCustomCursor(cursorImg, new Point(0, 0), "blank");
            result.setCursor(blankCursor);
        }

        return result;
    }

    public static JComponent getRenderer() {
        return renderer;
    }
}
