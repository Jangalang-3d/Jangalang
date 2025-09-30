package jangalang.client;

import jangalang.common.net.messages.Disconnect;
import jangalang.common.net.messages.HandshakeRequest;
import jangalang.common.net.messages.HandshakeResponse;
import jangalang.common.net.messages.StateSnapshot;
import jangalang.common.net.messages.InputPacket;
import jangalang.common.maps.MapData;
import jangalang.common.ApplicationProperties;

import java.net.*;
import java.io.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * Handles TCP handshake (map download) and a UDP socket for frequent packets.
 */
public class NetworkClient {
    private final String serverHost;
    private final int serverTcpPort;

    private Socket tcpSocket;
    private ObjectInputStream ois;
    private ObjectOutputStream oos;

    private DatagramSocket udpSocket;
    private InetAddress serverAddr;
    private int serverUdpPort;

    private int assignedId = -1;

    private final ExecutorService udpReceiver = Executors.newSingleThreadExecutor();

    // callback when state snapshot arrives
    private Consumer<StateSnapshot> onSnapshot;

    public NetworkClient(String host, int tcpPort) throws Exception {
        this.serverHost = host; this.serverTcpPort = tcpPort;
        connectTcp();
    }

    private void connectTcp() throws Exception {
        tcpSocket = new Socket(serverHost, serverTcpPort);
        oos = new ObjectOutputStream(tcpSocket.getOutputStream());
        ois = new ObjectInputStream(tcpSocket.getInputStream());
        // prepare UDP socket and include its port in handshake
        udpSocket = new DatagramSocket(0); // ephemeral UDP port
        serverAddr = tcpSocket.getInetAddress();

        HandshakeRequest req = new HandshakeRequest(udpSocket.getLocalPort());
        oos.writeObject(req);
        oos.flush();

        Object resp = ois.readObject();
        if (!(resp instanceof HandshakeResponse)) {
            throw new IllegalStateException("Expected HandshakeResponse");
        }
        HandshakeResponse r = (HandshakeResponse) resp;
        this.assignedId = r.assignedId;
        this.serverUdpPort = r.serverUdpPort;
        System.out.printf("Handshake complete: id=%d serverUdp=%d mapLoaded%n", assignedId, serverUdpPort);
        // start UDP receive loop
        udpReceiver.submit(this::udpLoop);
    }

    public int getAssignedId() { return assignedId; }
    public DatagramSocket getUdpSocket() { return udpSocket; }
    public InetAddress getServerAddress() { return serverAddr; }
    public int getServerUdpPort() { return serverUdpPort; }

    public void setOnSnapshot(Consumer<StateSnapshot> cb) { this.onSnapshot = cb; }

    private void udpLoop() {
        byte[] buf = new byte[65536];
        DatagramPacket p = new DatagramPacket(buf, buf.length);
        while (!udpSocket.isClosed()) {
            try {
                udpSocket.receive(p);
                ByteArrayInputStream bais = new ByteArrayInputStream(p.getData(), 0, p.getLength());
                ObjectInputStream ois = new ObjectInputStream(bais);
                Object o = ois.readObject();
                if (o instanceof StateSnapshot) {
                    if (onSnapshot != null) onSnapshot.accept((StateSnapshot) o);
                }
            } catch (SocketException se) {
                break;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void sendInput(InputPacket input) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos2 = new ObjectOutputStream(baos);
            oos2.writeObject(input);
            oos2.flush();
            byte[] data = baos.toByteArray();
            DatagramPacket dp = new DatagramPacket(data, data.length, serverAddr, serverUdpPort);
            udpSocket.send(dp);
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void disconnect() {
        try {
            if (oos != null) {
                oos.writeObject(new Disconnect(assignedId));
                oos.flush();
            }
        } catch (Exception ignored) {}

        try {
            udpSocket.close();
        } catch (Exception ignored) {}

        try {
            tcpSocket.close();
        } catch (Exception ignored) {}
    }
}
