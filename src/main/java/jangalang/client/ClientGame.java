package jangalang.client;

import jangalang.common.PlayerState;
import jangalang.common.maps.MapData;
import jangalang.common.ApplicationProperties;
import jangalang.common.net.messages.*;

import java.util.*;
import java.util.concurrent.*;

import javax.swing.JPanel;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.KeyEvent;

import jangalang.client.ui.GameMode;

/**
 * Client-side game logic: prediction for local and remote players, reconciliation.
 */
public class ClientGame implements GameMode {
    private final NetworkClient net;
    private final int clientId;
    private MapData map;
    private volatile boolean started = false;

    // predicted local player state
    private final PredictedPlayer local = new PredictedPlayer();

    // other players state map: id -> remote predicted
    private final ConcurrentMap<Integer, RemotePlayer> others = new ConcurrentHashMap<>();

    // pending client inputs for reconciliation
    private final NavigableMap<Long, InputPacket> pendingInputs = new ConcurrentSkipListMap<>();

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private volatile long clientTick = 0;

    public ClientGame(NetworkClient net) {
        this.net = net;
        this.clientId = net.getAssignedId();
        // set snapshot callback
        net.setOnSnapshot(this::onSnapshot);
    }

    public void start() {
        // tick loop at client fps for local prediction
        long fps = ApplicationProperties.getInt("game.fps");
        long ms = 1000L / fps;
        executor.scheduleAtFixedRate(this::clientTick, 0, ms, TimeUnit.MILLISECONDS);
        started = true;
    }
    public void stop() {
        executor.shutdownNow();
        started = false;
    }

    // call to send input from UI: forward/back/left/right, mouseDelta
    public void sendInput(boolean f, boolean b, boolean l, boolean r, double mouseDelta) {
        clientTick++;
        double newView = local.viewAngle + mouseDelta;
        InputPacket ip = new InputPacket(clientId, clientTick, f, b, l, r, mouseDelta, newView);
        // apply prediction locally
        local.applyInput(ip);
        pendingInputs.put(clientTick, ip);
        // send to server via UDP
        net.sendInput(ip);
    }

    // called periodically to simulate local physics a frame (simple; already applied in sendInput)
    private void clientTick() {
        // For now no periodic state update required; we rely on sendInput applying movement.
        // We do update other players' extrapolation here
        double dt = 1.0 / ApplicationProperties.getInt("game.fps");
        for (RemotePlayer rp : others.values()) {
            rp.simulate(dt);
        }
    }

    // called on incoming authoritative state from server (UDP)
    private void onSnapshot(StateSnapshot snap) {
        // iterate server players
        for (PlayerState ps : snap.players) {
            if (ps.id == clientId) {
                // reconciliation for local player
                double dx = ps.xCoord - local.x;
                double dy = ps.yCoord - local.y;
                double err = Math.hypot(dx,dy);
                if (err > 0.001) {
                    // correct and replay pending inputs after ack tick
                    local.x = ps.xCoord; local.y = ps.yCoord; local.vx = ps.velX; local.vy = ps.velY; local.viewAngle = ps.viewAngle;
                    // replay all pending inputs with tick > snap.ackClientTick
                    long ack = snap.ackClientTick;
                    SortedMap<Long, InputPacket> toReplay = pendingInputs.tailMap(ack+1);
                    List<Long> keys = new ArrayList<>(toReplay.keySet());
                    for (long k : keys) {
                        InputPacket ip = pendingInputs.get(k);
                        local.applyInput(ip);
                    }
                }
                // drop acknowledged inputs
                long ack = snap.ackClientTick;
                pendingInputs.headMap(ack+1).clear();
            } else {
                // update remote player authoritative state
                RemotePlayer rp = others.computeIfAbsent(ps.id, RemotePlayer::new);
                rp.receiveServerState(ps);
            }
        }
    }

    // expose state for rendering
    public double getLocalX() {
        return local.x;
    }

    public double getLocalY() {
        return local.y;
    }

    public double getLocalViewAngle() {
        return local.viewAngle;
    }

    public MapData getMap() {
        return map;
    } // map filled during handshake by NetworkClient; left for you to integrate

    // GameMode interface methods (UI will call these)
    @Override public void update() {
        boolean forward = local.keySet.contains("w");
        boolean backward = local.keySet.contains("s");
        boolean left = local.keySet.contains("a");
        boolean right = local.keySet.contains("d");

        sendInput(forward, backward, left, right, 0);
    }

    @Override
    public void render(JPanel window, Graphics g) {
        // You should integrate your previous PlayingState.render code here, using:
        // - local.x / local.y as camera origin (replace player references)
        // - use map returned from handshake (net receives MapData in handshake; store into map)
        // For brevity this demo just draws a HUD and remote players.
        window.setBackground(Color.darkGray);
        g.setColor(Color.WHITE);
        g.drawString(String.format("Client %d  Local: (%.2f, %.2f), va=%.2f pending=%d", clientId, local.x, local.y, local.viewAngle, pendingInputs.size()), 10, 20);

        // draw simple markers for remote players
        int idx = 0;
        for (RemotePlayer rp : others.values()) {
            int sx = 100 + (idx * 40);
            int sy = 40;
            g.setColor(Color.CYAN);
            g.fillOval(sx, sy, 12, 12);
            g.setColor(Color.WHITE);
            g.drawString(String.format("P%d (%.2f,%.2f)", rp.id, rp.x, rp.y), sx + 16, sy + 10);
            idx++;
        }
    }

    @Override public void keyPressed(java.awt.event.KeyEvent e) {
        switch (e.getKeyCode()) {
           case KeyEvent.VK_W -> local.keySet.add("w");
           case KeyEvent.VK_A -> local.keySet.add("a");
           case KeyEvent.VK_S -> local.keySet.add("s");
           case KeyEvent.VK_D -> local.keySet.add("d");
        }
    }
    @Override public void keyReleased(java.awt.event.KeyEvent e) {
        switch (e.getKeyCode()) {
           case KeyEvent.VK_W -> local.keySet.remove("w");
           case KeyEvent.VK_A -> local.keySet.remove("a");
           case KeyEvent.VK_S -> local.keySet.remove("s");
           case KeyEvent.VK_D -> local.keySet.remove("d");
        }
    }
    @Override public void mouseClicked(java.awt.event.MouseEvent e) {}
    @Override public void mouseMoved(int e) {
        // forwarded mouse delta: convert to an input with zero movement and only rotation
        sendInput(false, false, false, false, e * ApplicationProperties.getDouble("game.user.sensitivity"));
    }

    // internal helper classes: predicted local player and remote players
    static class PredictedPlayer {
        private double x = 0;
        private double y = 0;
        private double vx = 0;
        private double vy = 0;
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
                vx += dirX*ACCEL;
                vy += dirY*ACCEL;

                double speed = Math.hypot(vx, vy);
                if (speed > MAX_SPEED) {
                    vx = (vx / speed) * MAX_SPEED;
                    vy = (vy / speed) * MAX_SPEED;
                }
            } else {
                vx *= FRICTION;
                vy *= FRICTION;
            }
            x += vx;
            y += vy;
            viewAngle = ip.viewAngle;
        }
    }

    static class RemotePlayer {
        final int id;
        volatile double x;
        volatile double y;
        volatile double vx;
        volatile double vy;
        volatile double viewAngle;

        // interpolation target
        volatile double tx;
        volatile double ty;
        volatile double tvx;
        volatile double tvy;
        volatile double tAngle;

        final Object lock = new Object();
        public RemotePlayer(int id) {
            this.id = id;
            this.x = this.y = 0;
            this.vx = this.vy = 0;
        }
        public void receiveServerState(PlayerState ps) {
            synchronized(lock) {
                // set target to server values; we'll lerp in simulate()
                tx = ps.xCoord;
                ty = ps.yCoord;
                tvx = ps.velX;
                tvy = ps.velY;
                tAngle = ps.viewAngle;
            }
        }
        public void simulate(double dt) {
            synchronized(lock) {
                // simple linear interpolation towards target (smooth correction)
                double blend = Math.min(1.0, dt * 10.0); // correction speed
                x += (tx - x) * blend;
                y += (ty - y) * blend;
                vx = tvx; vy = tvy;
                viewAngle += (tAngle - viewAngle) * blend;
            }
        }
    }
}
