package jangalang.server;

import java.net.Socket;
import java.io.*;
import jangalang.common.net.messages.*;
import jangalang.common.maps.MapData;

/**
 * Handle a single TCP client for handshake and graceful disconnects.
 * The UDP traffic happens over GameServer's UDP socket.
 */
public class TcpClientHandler implements Runnable {
    private final Socket socket;
    private final GameServer server;

    public TcpClientHandler(Socket s, GameServer server) {
        this.socket = s;
        this.server = server;
    }

    @Override
    public void run() {
        try (ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
             ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream())) {

            Object o = ois.readObject();
            if (!(o instanceof HandshakeRequest)) {
                socket.close();
                return;
            }
            HandshakeRequest req = (HandshakeRequest) o;
            // register client
            int assigned = server.registerClient(socket.getInetAddress(), req.clientUdpPort);
            MapData map = server.getMap();
            HandshakeResponse resp = new HandshakeResponse(assigned, server.getUdpPort(), map);
            oos.writeObject(resp);
            oos.flush();

            // now wait for Disconnect messages (or just close when connection closes)
            while (true) {
                Object in = ois.readObject();
                if (in instanceof Disconnect) {
                    Disconnect d = (Disconnect) in;
                    server.unregisterClient(d.id);
                    break;
                }
            }
        } catch (EOFException eof) {
            // client disconnected
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
