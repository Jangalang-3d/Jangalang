package jangalang.client;

import jangalang.common.PlayerState;
import jangalang.common.maps.MapData;
import jangalang.common.maps.Wall;
import jangalang.common.ApplicationProperties;
import jangalang.common.net.messages.*;
import jangalang.common.types.Vector;

import java.util.*;
import java.util.concurrent.*;

import javax.swing.JPanel;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

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

    private BufferedImage wallTexture = ResourceLoader.wallTextures.get(1);
    private BufferedImage floorTexture = ResourceLoader.floorTextures.get(2);

    private int currentFireFrame = 0;
    private boolean isShooting = false;
    private int frameTimer = 0;
    private int frameDelay = 3;

    public ClientGame(NetworkClient net) {
        this.net = net;
        this.clientId = net.getAssignedId();
        this.map = net.getMap();
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

        final int screenW = window.getWidth();
        final int screenH = window.getHeight();

        // --- Prepare fast framebuffer ---
        // Create a single buffered image we can write pixels into (INT_RGB for speed)
        BufferedImage frame = new BufferedImage(screenW, screenH, BufferedImage.TYPE_INT_RGB);
        final int[] pixels = ((DataBufferInt) frame.getRaster().getDataBuffer()).getData();

        final int skyColor = (105 << 16) | (12 << 8) | 15;
        // Render sky half
        for (int y = 0; y < screenH / 2; ++y) {
            int idx = y * screenW;
            for (int x = 0; x < screenW; ++x) pixels[idx++] = skyColor;
        }

        final int floorBase = (30 << 16) | (30 << 8) | 30;
        // Render floor half
        for (int y = screenH / 2; y < screenH; ++y) {
            int idx = y * screenW;
            for (int x = 0; x < screenW; ++x) pixels[idx++] = floorBase;
        }

        // --- Cache player / camera vectors and texture pixel arrays ---
        final double ox = local.getXCoord();
        final double oy = local.getYCoord();

        // center view vector and player angle
        Vector center = new Vector(local.getViewAngle());
        final double dirX = center.x;
        final double dirY = center.y;
        final double playerAngle = Math.atan2(dirY, dirX);

        final double fov = ApplicationProperties.getDouble("game.user.fov");

        // Precompute camera plane (perpendicular to view dir). We need this for floor-casting interpolation.
        // plane vector length is tan(fov/2)
        final double planeScale = Math.tan(fov / 2.0);
        final double planeX = -dirY * planeScale;
        final double planeY =  dirX * planeScale;

        // fetch texture pixels once (fast access)
        final BufferedImage wallTex = this.wallTexture;
        final int wallW = wallTex.getWidth();
        final int wallH = wallTex.getHeight();
        final int[] wallPixels = new int[wallW * wallH];
        wallTex.getRGB(0, 0, wallW, wallH, wallPixels, 0, wallW);

        final BufferedImage floorTex = this.floorTexture;
        final int floorW = floorTex.getWidth();
        final int floorH = floorTex.getHeight();
        final int[] floorPixels = new int[floorW * floorH];
        floorTex.getRGB(0, 0, floorW, floorH, floorPixels, 0, floorW);

        // precompute proj plane distance used for vertical slice height -> helps remove fish-eye
        final double projPlaneDist = (screenW / 2.0) / Math.tan(fov / 2.0);

        // --- FLOOR CASTING (per-row) ---
        // We'll use interpolation between left and right ray directions for each row.
        // Precompute left and right ray directions (for cameraX = -1 and +1)
        double leftRayX = dirX + planeX * -1.0;
        double leftRayY = dirY + planeY * -1.0;
        double rightRayX = dirX + planeX * 1.0;
        double rightRayY = dirY + planeY * 1.0;

        final int halfH = screenH / 2;
        // iterate rows bottom half only
        for (int y = halfH; y < screenH; ++y) {
            // row distance: how far in world units this row corresponds to (approximation)
            // Derived from similar triangles: rowDistance = projPlaneDist * worldCamHeight / (y - screenH/2)
            // We'll use worldCamHeight = 1.0 (unit wall / floor height) — tweakable if you add camera height.
            final double rowFactor = (double) (y - halfH);
            if (rowFactor == 0) continue;
            final double rowDistance = projPlaneDist / rowFactor; // simple, fast

            // Interpolate start world point (floor) for x=0 and step per column
            // worldX = ox + dir * rowDistance; but we need different direction per column between leftRay and rightRay.
            // Precompute base for left ray:
            final double floorStartX = (ox * -0.5) + leftRayX * rowDistance;
            final double floorStartY = (oy * -0.5) + leftRayY * rowDistance;
            // step increments (difference across the screen)
            final double stepX = (rightRayX - leftRayX) * (rowDistance / (double) (screenW - 1));
            final double stepY = (rightRayY - leftRayY) * (rowDistance / (double) (screenW - 1));

            double worldX = floorStartX;
            double worldY = floorStartY;

            int baseIdx = y * screenW;
            for (int x = 0; x < screenW; ++x) {
                // sample floor texture using fractional part (wrap)
                double fx = worldX - Math.floor(worldX);
                double fy = worldY - Math.floor(worldY);
                int tx = (int) (fx * floorW) & (floorW - 1); // bitmask wrap if width is power-of-two
                int ty = (int) (fy * floorH) & (floorH - 1);

                // fallback to safe modulo if not pow2:
                if ((floorW & (floorW - 1)) != 0) {
                    tx = ((int) (Math.abs(fx * floorW))) % floorW;
                }
                if ((floorH & (floorH - 1)) != 0) {
                    ty = ((int) (Math.abs(fy * floorH))) % floorH;
                }

                int fpix = floorPixels[ty * floorW + tx];

                // Apply simple distance-based darkening (using rowDistance)
                double shade = 1.0 - Math.min(rowDistance / Math.max(1.0, ApplicationProperties.getDouble("game.user.viewdist")), 1.0);
                shade = 0.2 + 0.8 * shade;
                int red = (int) (((fpix >> 16) & 0xFF) * shade);
                int green = (int) (((fpix >> 8) & 0xFF) * shade);
                int blue = (int) ((fpix & 0xFF) * shade);
                int shaded = (red << 16) | (green << 8) | blue;

                pixels[baseIdx + x] = shaded;

                worldX += stepX;
                worldY += stepY;
            }
        }

        // --- WALL RENDERING (per-column) ---
        final double textureScaleHorizontal = 2.0; // Tiling for horizontal walls
        final double textureScaleVertical = 1.0; // Tiling for vertical walls

        for (int x = 0; x < screenW; ++x) {
            double cameraX = (2.0 * x / (screenW - 1) - 1.0);
            double rdx = dirX + planeX * cameraX;
            double rdy = dirY + planeY * cameraX;

            double closest = Double.POSITIVE_INFINITY;
            Wall hitWall = null;
            double hitX = 0.0;
            double hitY = 0.0;

            // Find closest wall intersection
            for (Wall wall : map.getWalls()) {
                Double u = wall.rayIntersect(ox, oy, rdx, rdy);
                if (u != null && u > 1e-9 && u < closest) {
                    closest = u;
                    hitWall = wall;
                    hitX = ox + rdx * u;
                    hitY = oy + rdy * u;
                }
            }

            if (closest == Double.POSITIVE_INFINITY) {
                continue;
            }

            double perpDist = closest;
            int lineHeight = (int) (screenH / perpDist);
            int drawStart = screenH / 2 - lineHeight / 2;
            int drawEnd = screenH / 2 + lineHeight / 2;

            if (drawStart < 0)
                drawStart = 0;
            if (drawEnd >= screenH)
                drawEnd = screenH - 1;

            // Texture coordinate calculation with intelligent tiling
            double texXf = 0.0;
            double textureScale = textureScaleHorizontal; // default

            if (hitWall != null) {
                double wallDx = hitWall.end.getKey() - hitWall.start.getKey();
                double wallDy = hitWall.end.getValue() - hitWall.start.getValue();
                double wallLength = Math.sqrt(wallDx * wallDx + wallDy * wallDy);

                double hitDx = hitX - hitWall.start.getKey();
                double hitDy = hitY - hitWall.start.getValue();

                double hitDist = (hitDx * wallDx + hitDy * wallDy) / wallLength;

                // Determine if wall is more horizontal or vertical
                boolean isHorizontal = Math.abs(wallDx) > Math.abs(wallDy);
                textureScale = isHorizontal ? textureScaleHorizontal : textureScaleVertical;

                texXf = hitDist * textureScale;
                texXf = texXf - Math.floor(texXf);
            }

            int texCol = (int) (texXf * wallW);
            texCol = Math.max(0, Math.min(wallW - 1, texCol));

            // Render wall slice
            for (int y = drawStart; y <= drawEnd; ++y) {
                double relativeY = (y - drawStart) / (double) lineHeight;
                double texY = relativeY * textureScale;
                texY = texY - Math.floor(texY);

                int texRow = (int) (texY * wallH);
                texRow = Math.max(0, Math.min(wallH - 1, texRow));

                int texturePixel = wallPixels[texRow * wallW + texCol];

                double maxView = ApplicationProperties.getDouble("game.user.viewdist");
                double shade = 1.0 - Math.min(perpDist / maxView, 1.0);
                shade = 0.3 + 0.7 * shade;

                int r = (int) (((texturePixel >> 16) & 0xFF) * shade);
                int gg = (int) (((texturePixel >> 8) & 0xFF) * shade);
                int b = (int) ((texturePixel & 0xFF) * shade);
                int shaded = (r << 16) | (gg << 8) | b;

                pixels[y * screenW + x] = shaded;
            }
        }
        // --- BLIT FRAMEBUFFER ONTO SCREEN ---
        g.drawImage(frame, 0, 0, null);

        // --- HUD / weapon (draw after framebuffer) ---
        // Weapon sprite bottom-right scaled
        if (this.isShooting) {
            this.frameTimer++;
            if (this.frameTimer >= this.frameDelay) {
                this.frameTimer = 0;
                this.currentFireFrame++;
                if (this.currentFireFrame >= ResourceLoader.weaponSprites.size()) {
                    this.isShooting = false;
                    this.currentFireFrame = 0;
                }
            }
        }

        BufferedImage weaponSprite = ResourceLoader.weaponSprites.get(this.currentFireFrame);

        double scale = 0.4; // 30% of screen height
        int h = (int)(screenH * scale);
        int w = weaponSprite.getWidth() * h / weaponSprite.getHeight();
        int wx = Math.max(0, screenW - (int)(w * 1.5));
        int wy = Math.max(0, screenH - h);
        g.drawImage(weaponSprite, wx, wy, w, h, null);

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
