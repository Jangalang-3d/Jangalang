package jangalang.engine;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import javax.swing.JComponent;
import java.awt.Robot;
import java.awt.Point;
import java.awt.AWTException;
import java.awt.Dimension;

public class MouseScanner implements MouseListener, MouseMotionListener {
    private JComponent window;
    private Robot robot;
    private Point center;
    private boolean mouseReset = false;
    private boolean offsetToggle = false;

    public void init(JComponent window) throws AWTException {
        this.window = window;
        this.robot = new Robot();
        this.robot.setAutoDelay(0);
        Dimension size = window.getSize();
        Point loc = window.getLocationOnScreen();
        this.center = new Point(
            loc.x + size.width / 2,
            loc.y + size.height / 2
        );
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        if (mouseReset) { // Don't read fake mouse movements
            mouseReset = false;
            return;
        }

        // Get delta mouse movement
        int dx = e.getXOnScreen() + (offsetToggle ? 1 : 0) - center.y;
        if (dx != 0) { // Don't send non-movements
            Game.getMode().mouseMoved(dx);
        }

        // TODO: Maybe find a better way to do this?
        // Offset is used to prevent values from being swallowed
        offsetToggle = !offsetToggle;

        mouseReset = true;
        robot.mouseMove(center.x, center.y);
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (mouseReset) { // Don't read fake mouse movements
            mouseReset = false;
            return;
        }

        // Get delta mouse movement
        int dx = e.getXOnScreen() + (offsetToggle ? 1 : 0) - center.x;
        if (dx != 0) { // Don't send non-movements
            Game.getMode().mouseMoved(dx);
        }

        // TODO: Maybe find a better way to do this?
        // Offset is used to prevent values from being swallowed
        offsetToggle = !offsetToggle;

        mouseReset = true;
        robot.mouseMove(center.x, center.y);
    }

    @Override
    public void mousePressed(MouseEvent e) {
        Game.getMode().mouseClicked(e);
    }

    @Override
    public void mouseReleased(MouseEvent e) {}

    @Override
    public void mouseEntered(MouseEvent e) {}

    @Override
    public void mouseExited(MouseEvent e) {}

    @Override
    public void mouseClicked(MouseEvent e) {
    }
}
