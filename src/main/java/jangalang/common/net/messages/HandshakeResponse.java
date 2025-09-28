package jangalang.common.net.messages;

import java.io.Serializable;
import jangalang.common.maps.MapData;

public class HandshakeResponse implements Serializable {
    private static final long serialVersionUID = 1L;
    public final int assignedId;
    public final int serverUdpPort;
    public final MapData map;

    public HandshakeResponse(int assignedId, int serverUdpPort, MapData map) {
        this.assignedId = assignedId;
        this.serverUdpPort = serverUdpPort;
        this.map = map;
    }
}
