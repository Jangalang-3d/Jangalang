package jangalang.server;

import jangalang.common.maps.MapData;
import jangalang.common.maps.Wall;
import jangalang.common.ApplicationProperties;
import jangalang.common.net.messages.StateSnapshot;
import jangalang.common.net.messages.InputPacket;
import jangalang.common.PlayerState;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.concurrent.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Authoritative server: holds players map, applies inputs, broadcasts state snapshots via UDP.
 */
public class GameServer {
    private final MapData map;
    private final int udpPort;
    private DatagramSocket udpSocket;
    private final ScheduledExecutorService tickExecutor = Executors.newSingleThreadScheduledExecutor();
    private final ExecutorService udpReceiverExecutor = Executors.newSingleThreadExecutor();
    private final ConcurrentMap<Integer, ClientInfo> clients = new ConcurrentHashMap<>();
    private final AtomicInteger nextId = new AtomicInteger(1);

    // authoritative per-client state
    private final ConcurrentMap<Integer, ServerPlayer> players = new ConcurrentHashMap<>();

    // input buffer keyed by client id containing a queue of InputPackets
    private final ConcurrentMap<Integer, ConcurrentLinkedQueue<InputPacket>> inputQueues = new ConcurrentHashMap<>();

    public GameServer(MapData map, int udpPort) {
        this.map = map;
        this.udpPort = udpPort;
        try {
            this.udpSocket = new DatagramSocket(udpPort);
        } catch (SocketException se) {
            se.printStackTrace();
        }
    }

    public MapData getMap() {
        return map;
    }

    public int getUdpPort() {
        return udpPort;
    }

    public int registerClient(InetAddress addr, int clientUdpPort) {
        int id = nextId.getAndIncrement();
        clients.put(id, new ClientInfo(id, addr, clientUdpPort));
        // spawn
        double sx = 0, sy = 0;
        if (!map.getSpawns().isEmpty()) {
            sx = map.getSpawns().get(0).getKey();
            sy = map.getSpawns().get(0).getValue();
        }
        ServerPlayer sp = new ServerPlayer(id, sx, sy);
        players.put(id, sp);
        inputQueues.put(id, new ConcurrentLinkedQueue<>());

        System.out.println("Registered client " + id + " @ " + addr + ":" + clientUdpPort);
        return id;
    }

    public void unregisterClient(int id) {
        clients.remove(id);
        players.remove(id);
        inputQueues.remove(id);
        System.out.println("Unregistered client " + id);
    }

    public void start() {
        // start UDP receiver
        udpReceiverExecutor.submit(this::udpReceiveLoop);

        long tps = ApplicationProperties.getInt("game.tps");
        long tickMs = 1000L / tps;
        tickExecutor.scheduleAtFixedRate(this::tick, 0, tickMs, TimeUnit.MILLISECONDS);
        System.out.println("Game server started (tps=" + tps + ")");
    }

    public void stop() {
        tickExecutor.shutdownNow();
        udpReceiverExecutor.shutdownNow();
        udpSocket.close();
    }

    private void udpReceiveLoop() {
        byte[] buf = new byte[4096];
        DatagramPacket p = new DatagramPacket(buf, buf.length);
        while (!udpSocket.isClosed()) {
            try {
                udpSocket.receive(p);
                // deserialize object
                ByteArrayInputStream bais = new ByteArrayInputStream(p.getData(), 0, p.getLength());
                ObjectInputStream ois = new ObjectInputStream(bais);
                Object o = ois.readObject();
                if (o instanceof InputPacket) {
                    InputPacket ip = (InputPacket) o;
                    // store into inputQueues
                    ConcurrentLinkedQueue<InputPacket> q = inputQueues.get(ip.clientId);
                    if (q != null) q.add(ip);
                }
                // ignore other UDP message types for now
            } catch (SocketException se) { break; }
            catch (Exception e) { e.printStackTrace(); }
        }
    }

    private void tick() {
        // process inputs for each player
        long serverTick = System.currentTimeMillis();
        for (Map.Entry<Integer, ServerPlayer> e : players.entrySet()) {
            int id = e.getKey();
            ServerPlayer sp = e.getValue();
            ConcurrentLinkedQueue<InputPacket> q = inputQueues.get(id);
            // consume inputs since last update
            InputPacket ip;
            while (q != null && (ip = q.poll()) != null) {
                sp.applyInput(ip);
                // resolve collisions with map walls
                sp.resolveCollisions(map);
                sp.lastProcessedClientTick = ip.tick;
            }
            // when no input, still apply friction in sp.applyInput called with no movement when needed
        }
        // broadcast state snapshot to all registered clients
        broadcastSnapshot();
    }

    private void broadcastSnapshot() {
        Collection<ClientInfo> conns = clients.values();
        PlayerState[] arr = players.values().stream()
            .map(p -> new PlayerState(p.id, p.x, p.y, p.vx, p.vy, p.viewAngle))
            .toArray(PlayerState[]::new);
        // For simplicity we don't ack per-client tick here; we'll include last tick processed if we want.
        for (ClientInfo ci : conns) {
            try {
                StateSnapshot snap = new StateSnapshot(System.currentTimeMillis(), arr, ci.id, players.get(ci.id).lastProcessedClientTick);
                // serialize into bytes
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(baos);
                oos.writeObject(snap);
                oos.flush();
                byte[] data = baos.toByteArray();
                DatagramPacket dp = new DatagramPacket(data, data.length, ci.addr, ci.udpPort);
                udpSocket.send(dp);
            } catch (Exception ex) { ex.printStackTrace(); }
        }
    }

    // nested types
    static class ClientInfo {
        public final int id;
        public final InetAddress addr;
        public final int udpPort;
        public ClientInfo(int id, InetAddress a, int p){ this.id=id; this.addr=a; this.udpPort=p;}
    }

    static class ServerPlayer {
        public final int id;
        public double x,y;
        public double vx, vy;
        public double viewAngle = 0.0;
        public long lastProcessedClientTick = 0;
        private static final double ACCEL = 0.01;
        private static final double MAX_SPEED = 0.3;
        private static final double FRICTION = 0.9;
        public ServerPlayer(int id,double x,double y){ this.id=id; this.x=x; this.y=y; this.vx=0; this.vy=0; }
        public void applyInput(InputPacket in) {
            double dirX=0, dirY=0;
            double fx = Math.cos(in.viewAngle), fy = Math.sin(in.viewAngle);
            if (in.forward) { dirX += fx; dirY += fy; }
            if (in.backward) { dirX -= fx; dirY -= fy; }
            if (in.left) { dirX += fy; dirY -= fx; }
            if (in.right) { dirX -= fy; dirY += fx; }
            boolean accelerating = (dirX!=0 || dirY!=0);
            if (accelerating) {
                double len = Math.hypot(dirX,dirY);
                dirX /= len; dirY /= len;
                vx += dirX*ACCEL;
                vy += dirY*ACCEL;
                double speed = Math.hypot(vx,vy);
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
            viewAngle = in.viewAngle;
        }
        public void resolveCollisions(MapData map) {
            double newX = this.x, newY = this.y;
            double radius = 0.5;
            for (Wall w : map.getWalls()) {
                if (w.playerIntersect(newX, newY, radius)) {
                    double[] n = w.getNormal();
                    double nx=n[0], ny=n[1];
                    double dot = vx*nx + vy*ny;
                    vx = vx - dot*nx;
                    vy = vy - dot*ny;
                    newX = this.x + vx;
                    newY = this.y + vy;
                }
            }
            this.x = newX; this.y = newY;
        }
    }
}
