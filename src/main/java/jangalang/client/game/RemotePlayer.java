package jangalang.client.game;

import jangalang.common.PlayerState;

public class RemotePlayer {
    private final int id;
    private volatile double xCoord;
    private volatile double yCoord;
    private volatile double velX;
    private volatile double velY;
    private volatile double viewAngle;

    // interpolation target
    private volatile double targetXCoord;
    private volatile double targetYCoord;
    private volatile double targetVelX;
    private volatile double targetVelY;
    private volatile double targetViewAngle;

    private final Object lock = new Object();

    public RemotePlayer(int id) {
        this.id = id;
        this.xCoord = this.yCoord = 0;
        this.velX = this.velY = 0;
    }

    public void receiveServerState(PlayerState ps) {
        synchronized (lock) {
            // set target to server values
            targetXCoord = ps.xCoord;
            targetYCoord = ps.yCoord;
            targetVelX = ps.velX;
            targetVelY = ps.velY;
            targetViewAngle = ps.viewAngle;
        }
    }

    public void simulate(double dt) {
        synchronized (lock) {
            // simple linear interpolation towards target (smooth correction)
            double blend = Math.min(1.0, dt * 10.0); // correction speed
            xCoord += (targetXCoord - xCoord) * blend;
            yCoord += (targetYCoord - yCoord) * blend;
            velX = targetVelX;
            velY = targetVelY;
            viewAngle += (targetViewAngle - viewAngle) * blend;
        }
    }

    public int getId() {
        return id;
    }

    public double getXCoord() {
        return xCoord;
    }

    public void setXCoord(double xCoord) {
        this.xCoord = xCoord;
    }

    public double getYCoord() {
        return yCoord;
    }

    public void setYCoord(double yCoord) {
        this.yCoord = yCoord;
    }

    public double getVelX() {
        return velX;
    }

    public void setVelX(double velX) {
        this.velX = velX;
    }

    public double getVelY() {
        return velY;
    }

    public void setVelY(double velY) {
        this.velY = velY;
    }

    public double getViewAngle() {
        return viewAngle;
    }

    public void setViewAngle(double viewAngle) {
        this.viewAngle = viewAngle;
    }

    public double getTargetXCoord() {
        return targetXCoord;
    }

    public void setTargetXCoord(double targetXCoord) {
        this.targetXCoord = targetXCoord;
    }

    public double getTargetYCoord() {
        return targetYCoord;
    }

    public void setTargetYCoord(double targetYCoord) {
        this.targetYCoord = targetYCoord;
    }

    public double getTargetVelX() {
        return targetVelX;
    }

    public void setTargetVelX(double targetVelX) {
        this.targetVelX = targetVelX;
    }

    public double getTargetVelY() {
        return targetVelY;
    }

    public void setTargetVelY(double targetVelY) {
        this.targetVelY = targetVelY;
    }

    public double getTargetViewAngle() {
        return targetViewAngle;
    }

    public void setTargetViewAngle(double targetViewAngle) {
        this.targetViewAngle = targetViewAngle;
    }

    public Object getLock() {
        return lock;
    }
}
