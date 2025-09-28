package jangalang.common.net.messages;

import java.io.Serializable;

public class HandshakeRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    public final int clientUdpPort;

    public HandshakeRequest(int clientUdpPort) {
        this.clientUdpPort = clientUdpPort;
    }
}
