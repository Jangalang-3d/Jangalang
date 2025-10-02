package jangalang.server;

import jangalang.common.maps.MapData;
import jangalang.common.maps.Wall;
import jangalang.common.net.messages.InputPacket;

public class ServerPlayer {
    public final int id;
    public double xCoord;
    public double yCoord;
    public double velX;
    public double velY;
    public double viewAngle = 0.0;
    public long lastProcessedClientTick = 0;
    private static final double ACCEL = 0.01;
    private static final double MAX_SPEED = 0.3;
    private static final double FRICTION = 0.9;

    public ServerPlayer(int id, double xCoord, double yCoord) {
        this.id = id;
        this.xCoord = xCoord;
        this.yCoord = yCoord;
        this.velX = 0;
        this.velY = 0;
    }

    public void applyInput(InputPacket in) {
        double dirX = 0;
        double dirY = 0;
        double fx = Math.cos(in.viewAngle);
        double fy = Math.sin(in.viewAngle);
        if (in.forward) {
            dirX += fx;
            dirY += fy;
        }
        if (in.backward) {
            dirX -= fx;
            dirY -= fy;
        }
        if (in.left) {
            dirX -= fy;
            dirY += fx;
        }
        if (in.right) {
            dirX += fy;
            dirY -= fx;
        }
        boolean accelerating = (dirX != 0 || dirY != 0);
        if (accelerating) {
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
        viewAngle = in.viewAngle;
    }

    public void resolveCollisions(MapData map) {
        double newX = this.xCoord;
        double newY = this.yCoord;
        double radius = 0.5;
        for (Wall w : map.getWalls()) {
            if (w.playerIntersect(newX, newY, radius)) {
                double[] n = w.getNormal();
                double nx = n[0];
                double ny = n[1];
                double dot = velX * nx + velY * ny;
                velX = velX - dot * nx;
                velY = velY - dot * ny;

                newX = this.xCoord + velX;
                newY = this.yCoord + velY;
            }
        }
        this.xCoord = newX;
        this.yCoord = newY;
    }
}
