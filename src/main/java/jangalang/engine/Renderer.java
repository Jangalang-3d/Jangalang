package jangalang.engine;

import javax.swing.JPanel;
import java.awt.Graphics;

public class Renderer extends JPanel {
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Game.getMode().render(this, g);
    }
}
