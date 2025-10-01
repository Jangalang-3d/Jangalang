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

import jangalang.client.game.PredictedPlayer;
import jangalang.client.game.RemotePlayer;
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
    public void sendInput(boolean forward, boolean backward, boolean left, boolean right, double mouseDelta) {
        clientTick++;
        double newView = local.getViewAngle() + mouseDelta;
        InputPacket ip = new InputPacket(clientId, clientTick, forward, backward, left, right, mouseDelta, newView);
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
                double dx = ps.xCoord - local.getXCoord();
                double dy = ps.yCoord - local.getYCoord();
                double err = Math.hypot(dx,dy);
                if (err > 0.001) {
                    // correct and replay pending inputs after ack tick
                    local.setXCoord(ps.xCoord);
                    local.setYCoord(ps.yCoord);
                    local.setVelX(ps.velX);
                    local.setVelY(ps.velY);
                    local.setViewAngle(ps.viewAngle);

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
        return local.getXCoord();
    }

    public double getLocalY() {
        return local.getYCoord();
    }

    public double getLocalViewAngle() {
        return local.getViewAngle();
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
        g.drawString(
                     String.format("Client %d  Local: (%.2f, %.2f), va=%.2f pending=%d",
                                   clientId,
                                   local.getXCoord(),
                                   local.getYCoord(),
                                   local.getViewAngle(),
                                   pendingInputs.size()),
                     10, 20);

        // draw simple markers for remote players
        int idx = 0;
        for (RemotePlayer rp : others.values()) {
            int sx = 100 + (idx * 40);
            int sy = 40;
            g.setColor(Color.CYAN);
            g.fillOval(sx, sy, 12, 12);
            g.setColor(Color.WHITE);
            g.drawString(
                         String.format("P%d (%.2f,%.2f)",
                                       rp.getId(),
                                       rp.getXCoord(),
                                       rp.getYCoord()),
                         sx + 16, sy + 10);
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
}
