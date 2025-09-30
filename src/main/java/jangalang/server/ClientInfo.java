package jangalang.server;

import java.net.InetAddress;

public class ClientInfo {
    public final int id;
    public final InetAddress addr;
    public final int udpPort;

    public ClientInfo(int id, InetAddress addr, int udpPort) {
        this.id = id;
        this.addr = addr;
        this.udpPort = udpPort;
    }
}
