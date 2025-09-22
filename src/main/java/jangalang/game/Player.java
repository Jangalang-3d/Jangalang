package jangalang.game;

import java.awt.image.BufferedImage;
import java.io.InputStream;

import javax.imageio.ImageIO;

import jangalang.engine.maps.Map;
import jangalang.engine.maps.Wall;
import jangalang.util.GameProperties;
import jangalang.util.types.Vector;

public class Player {
    private double xCoord;
    private double yCoord;
    private final int size = 1;

    private double velX = 0;
    private double velY = 0;

    private static final double ACCEL = 0.01;
    private static final double MAX_SPEED = 0.3;
    private static final double FRICTION = 0.9;

    public static double RAY_MAX_LENGTH = GameProperties.getDouble("game.user.viewdist");
    public static int RAY_COUNT = GameProperties.getInt("game.user.resolution");
    public static double FOV = Math.toRadians(GameProperties.getInt("game.user.fov"));
    private Vector[] rays = new Vector[RAY_COUNT];
    private double viewAngleOffset = 0;

    private BufferedImage weaponSprite;

    public Player (double xCoord, double yCoord) {
        this.xCoord = xCoord;
        this.yCoord = yCoord;

        for (int i = 0; i < rays.length; ++i) {
            final double angle = this.viewAngleOffset - FOV / 2 + (FOV * i / (RAY_COUNT - 1));
            rays[i] = new Vector(Math.cos(angle), Math.sin(angle));
        }

        try (InputStream in = Player.class.getResourceAsStream("/sprites/guns/pistol/2PISA0.png")) {
            this.weaponSprite = ImageIO.read(in);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    public void move(Map gameMap, double dirX, double dirY, boolean accelerating) {
        if (accelerating) {
            // Normalize direction
            double len = Math.sqrt(dirX * dirX + dirY * dirY);
            if (len > 0) {
                dirX /= len;
                dirY /= len;
            }

            velX += dirX * ACCEL;
            velY += dirY * ACCEL;

            double speed = Math.sqrt(velX * velX + velY * velY);
            if (speed > MAX_SPEED) {
                velX = (velX / speed) * MAX_SPEED;
                velY = (velY / speed) * MAX_SPEED;
            }
        } else {
            velX *= FRICTION;
            velY *= FRICTION;
        }

        double newX = this.xCoord + velX;
        double newY = this.yCoord + velY;
        double radius = this.size / 2.0;

        for (Wall wall : gameMap.getWalls()) {
            if (wall.playerIntersect(newX, newY, radius)) {
                // --- Sliding instead of stopping ---
                // 1. Get the wall's normal vector

                double[] n = wall.getNormal(); // must be unit length!
                double nx = n[0];
                double ny = n[1];

                // 2. Project velocity onto tangent = v - (v·n)n
                double dot = velX * nx + velY * ny;
                velX = velX - dot * nx;
                velY = velY - dot * ny;

                // 3. Recompute position using adjusted velocity
                newX = this.xCoord + velX;
                newY = this.yCoord + velY;

                break; // only need first blocking wall this frame
            }
        }
        this.xCoord = newX;
        this.yCoord = newY;
    }

    public double getXCoord() {
        return this.xCoord;
    }

    public double getYCoord() {
        return this.yCoord;
    }

    public int getSize() {
        return this.size;
    }

    public Vector getViewAngle() {
        return rays[RAY_COUNT / 2]; // center ray
    }

    public Vector[] getRays() {
        return this.rays;
    }

    public void updateRay(int index, Vector v) {
        this.rays[index] = v;
    }

    public void rotate(double angleDelta) {
        this.viewAngleOffset += angleDelta;

        for (int i = 0; i < rays.length; ++i) {
            final double angle = this.viewAngleOffset - FOV / 2.0 + (FOV * i / (RAY_COUNT - 1));
            this.rays[i].x = Math.cos(angle);
            this.rays[i].y = Math.sin(angle);
        }
    }

    public BufferedImage getWeaponSprite() {
        return this.weaponSprite;
    }
}
