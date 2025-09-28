package jangalang.common.net.messages;

import java.io.Serializable;
import jangalang.common.PlayerState;

public class StateSnapshot implements Serializable {
    private static final long serialVersionUID = 1L;
    public final long serverTick;
    public final PlayerState[] players;
    public final int ackClientId;
    public final long ackClientTick;

    public StateSnapshot(long serverTick, PlayerState[] players, int ackClientId, long ackClientTick) {
        this.serverTick = serverTick;
        this.players = players;
        this.ackClientId = ackClientId;
        this.ackClientTick = ackClientTick;
    }
}
