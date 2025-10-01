package jangalang.client.game;

import java.util.HashSet;

import jangalang.common.net.messages.InputPacket;

public class PredictedPlayer {
    private double xCoord = 0;
    private double yCoord = 0;
    private double velX = 0;
    private double velY = 0;
    private double viewAngle = 0;
    private static final double ACCEL = 0.01;
    private static final double MAX_SPEED = 0.3;
    private static final double FRICTION = 0.9;
    public HashSet<String> keySet = new HashSet<String>();

    public void applyInput(InputPacket ip) {
        double dirX = 0;
        double dirY = 0;
        double fx = Math.cos(ip.viewAngle);
        double fy = Math.sin(ip.viewAngle);
        if (ip.forward) {
            dirX += fx;
            dirY += fy;
        }
        if (ip.backward) {
            dirX -= fx;
            dirY -= fy;
        }
        if (ip.left) {
            dirX += fy;
            dirY -= fx;
        }
        if (ip.right) {
            dirX -= fy;
            dirY += fx;
        }
        boolean acc = (dirX != 0 || dirY != 0);
        if (acc) {
            double len = Math.hypot(dirX, dirY);
            dirX /= len;
            dirY /= len;
            velX += dirX * ACCEL;
            velY += dirY * ACCEL;

            double speed = Math.hypot(velX, velY);
            if (speed > MAX_SPEED) {
                velX = (velX / speed) * MAX_SPEED;
                velY = (velY / speed) * MAX_SPEED;
            }
        } else {
            velX *= FRICTION;
            velY *= FRICTION;
        }
        xCoord += velX;
        yCoord += velY;
        viewAngle = ip.viewAngle;
    }

    public double getXCoord() {
        return this.xCoord;
    }

    public double getYCoord() {
        return this.yCoord;
    }

    public double getVelX() {
        return this.velX;
    }

    public double getVelY() {
        return this.velY;
    }

    public double getViewAngle() {
        return this.viewAngle;
    }

    public void setXCoord(double xCoord) {
        this.xCoord = xCoord;
    }

    public void setYCoord(double yCoord) {
        this.yCoord = yCoord;
    }

    public void setVelX(double velX) {
        this.velX = velX;
    }

    public void setVelY(double velY) {
        this.velY = velY;
    }

    public void setViewAngle(double viewAngle) {
        this.viewAngle = viewAngle;
    }
}
