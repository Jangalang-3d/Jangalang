package jangalang.common;

import java.io.Serializable;

public class PlayerState implements Serializable {
    private static final long serialVersionUID = 1L;
    public final int id;
    public double xCoord;
    public double yCoord;
    public double velX;
    public double velY;
    public double viewAngle;

    public PlayerState(int id, double xCoord, double yCoord, double velX, double velY, double viewAngle) {
        this.id = id;
        this.xCoord = xCoord;
        this.yCoord = yCoord;
        this.velX = velX;
        this.velY = velY;
        this.viewAngle = viewAngle;
    }
}
