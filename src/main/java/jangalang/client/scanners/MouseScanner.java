package jangalang.client.scanners;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.AWTException;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Robot;
import javax.swing.JComponent;

import jangalang.client.ClientGame;

public class MouseScanner extends MouseAdapter {
    private final ClientGame game;
    private Robot robot;
    private Point center;
    private boolean mouseReset = false;
    private boolean offsetToggle = false;

    public MouseScanner(JComponent window, ClientGame game) {
        this.game = game;
        try {
            this.robot = new Robot();
        } catch (AWTException awte) {
            awte.printStackTrace();
        }
        this.robot.setAutoDelay(0);
        Dimension size = window.getSize();
        Point location = window.getLocationOnScreen();
        this.center = new Point(location.x + size.width / 2, location.y + size.height / 2);
        System.out.println("MouseScanner initialized");
    }

    private void onMouseMove(MouseEvent e) {
        if (mouseReset) { // Don't read fake mouse movements
            mouseReset = false;
            return;
        }

        // Get delta mouse movement
        int dx = e.getXOnScreen() + (offsetToggle ? 1 : 0) - center.x;
        if (dx != 0) { // Don't send non-movements
            game.mouseMoved(-dx);
        }

        offsetToggle = !offsetToggle;

        mouseReset = true;
        robot.mouseMove(center.x, center.y);
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        this.onMouseMove(e);
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        this.onMouseMove(e);
    }

    @Override
    public void mousePressed(MouseEvent e) {
        game.mouseClicked(e);
    }
}
