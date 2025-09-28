package jangalang.engine.gamemode;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import javax.swing.JPanel;
import java.awt.image.DataBufferInt;

import jangalang.engine.Game;
import jangalang.engine.GameState;
import jangalang.engine.maps.Map;
import jangalang.engine.maps.Wall;
import jangalang.game.Player;
import jangalang.util.types.Vector;
import jangalang.util.GameProperties;

import java.util.HashSet;

public class PlayingState implements GameMode {
    private HashSet<String> keySet = new HashSet<String>();
    private Player player = Game.getPlayer();
    private Map gameMap = Game.getMap();

    private int currentFireFrame = 0;
    private boolean isShooting = false;
    private int frameTimer = 0;
    private int frameDelay = 3;

    @Override
    public void update() {
        // Update player location
        double dirX = 0;
        double dirY = 0;
        Vector forward = player.getViewAngle();

        if (keySet.contains("w")) {
            dirX += forward.x;
            dirY += forward.y;
        }
        if (keySet.contains("s")) {
            dirX -= forward.x;
            dirY -= forward.y;
        }
        if (keySet.contains("a")) {
            dirX += forward.y;
            dirY -= forward.x;
        }
        if (keySet.contains("d")) {
            dirX -= forward.y;
            dirY += forward.x;
        }

        boolean accelerating = !keySet.isEmpty();
        player.move(gameMap, dirX, dirY, accelerating);
    }

    @Override
    public void render(JPanel window, Graphics g) {
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
        final double ox = player.getXCoord();
        final double oy = player.getYCoord();

        // center view vector and player angle
        Vector center = player.getViewAngle();
        final double dirX = center.x;
        final double dirY = center.y;
        final double playerAngle = Math.atan2(dirY, dirX);

        final double fov = Player.FOV;

        // Precompute camera plane (perpendicular to view dir). We need this for floor-casting interpolation.
        // plane vector length is tan(fov/2)
        final double planeScale = Math.tan(fov / 2.0);
        final double planeX = -dirY * planeScale;
        final double planeY =  dirX * planeScale;

        // fetch texture pixels once (fast access)
        final BufferedImage wallTex = Game.wallTexture;
        final int wallW = wallTex.getWidth();
        final int wallH = wallTex.getHeight();
        final int[] wallPixels = new int[wallW * wallH];
        wallTex.getRGB(0, 0, wallW, wallH, wallPixels, 0, wallW);

        final BufferedImage floorTex = Game.floorTexture;
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
            final double floorStartX = ox + leftRayX * rowDistance;
            final double floorStartY = oy + leftRayY * rowDistance;
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
                double shade = 1.0 - Math.min(rowDistance / Math.max(1.0, Player.RAY_MAX_LENGTH), 1.0);
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
        // For each vertical screen column cast a ray (same logic you used before) but write pixels into `pixels[]`
        for (int x = 0; x < screenW; ++x) {
            // cameraX in -1..1
            double cameraX = (screenW == 1) ? 0.0 : (2.0 * x / (screenW - 1) - 1.0);

            // ray direction using plane interpolation (dir + plane * cameraX)
            double rdx = dirX + planeX * cameraX;
            double rdy = dirY + planeY * cameraX;
            // normalize is not necessary for intersection math; rayIntersect expects direction vector.

            // find closest intersection along this ray
            double closest = Double.POSITIVE_INFINITY;
            double hitX = 0.0, hitY = 0.0;
            for (Wall wall : gameMap.getWalls()) {
                Double u = wall.rayIntersect(ox, oy, rdx, rdy);
                if (u != null && u > 1e-9 && u < closest) {
                    closest = u;
                    hitX = ox + rdx * u;
                    hitY = oy + rdy * u;
                }
            }
            if (closest == Double.POSITIVE_INFINITY) closest = Player.RAY_MAX_LENGTH;

            // remove fish-eye (convert to perpendicular distance)
            double perpDist = closest * Math.cos(Math.atan2(rdy, rdx) - playerAngle);
            if (perpDist < 1e-6) perpDist = 1e-6;

            // compute vertical slice height
            int lineHeight = (int) ((1.0 * screenH) / perpDist * (projPlaneDist / (screenW / 2.0)));

            int drawStart = -lineHeight / 2 + screenH / 2;
            int drawEnd = lineHeight / 2 + screenH / 2;
            if (drawStart < 0) drawStart = 0;
            if (drawEnd >= screenH) drawEnd = screenH - 1;

            // texture X: decide whether to use fractional part of hitX or hitY depending on wall orientation
            double texXf = hitX - Math.floor(hitX);
            // detect near-integer X (vertical wall) — threshold small to avoid floating rounding issues
            if (Math.abs(hitX - Math.floor(hitX + 0.5)) < 1e-3) texXf = hitY - Math.floor(hitY);
            int texCol = (int) (texXf * wallW);
            if (texCol < 0) texCol = 0;
            if (texCol >= wallW) texCol = wallW - 1;

            final int colIndexBase = texCol; // column offset in texture

            // Loop over vertical slice and copy into framebuffer
            final int sliceHeight = Math.max(1, drawEnd - drawStart + 1);
            for (int y = drawStart; y <= drawEnd; ++y) {
                double texYRatio = (y - drawStart) / (double) sliceHeight;
                int texRow = (int) (texYRatio * wallH);
                if (texRow < 0) texRow = 0;
                if (texRow >= wallH) texRow = wallH - 1;

                int texturePixel = wallPixels[texRow * wallW + colIndexBase];

                // shading by perpendicular distance
                double maxView = Player.RAY_MAX_LENGTH;
                double shade = 1.0 - Math.min(perpDist / Math.max(1.0, maxView), 1.0);
                shade = 0.2 + 0.8 * shade;
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
                if (this.currentFireFrame >= Game.weaponSprite.length) {
                    this.isShooting = false;
                    this.currentFireFrame = 0;
                }
            }
        }

        BufferedImage weaponSprite = Game.weaponSprite[this.currentFireFrame];

        double scale = 0.4; // 30% of screen height
        int h = (int)(screenH * scale);
        int w = weaponSprite.getWidth() * h / weaponSprite.getHeight();
        int wx = Math.max(0, screenW - (int)(w * 1.5));
        int wy = Math.max(0, screenH - h);
        g.drawImage(weaponSprite, wx, wy, w, h, null);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_W -> keySet.add("w");
            case KeyEvent.VK_A -> keySet.add("a");
            case KeyEvent.VK_S -> keySet.add("s");
            case KeyEvent.VK_D -> keySet.add("d");
            case KeyEvent.VK_ESCAPE -> Game.setGameState(GameState.PAUSED);
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_W -> keySet.remove("w");
            case KeyEvent.VK_A -> keySet.remove("a");
            case KeyEvent.VK_S -> keySet.remove("s");
            case KeyEvent.VK_D -> keySet.remove("d");
        }
    }

    @Override
    public void mouseMoved(int e) {
        player.rotate(e * GameProperties.getDouble("game.user.sensitivity"));
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (!isShooting) {
            this.isShooting = true;
            this.currentFireFrame = 0;
            this.frameTimer = 0;
        }
    }
}
