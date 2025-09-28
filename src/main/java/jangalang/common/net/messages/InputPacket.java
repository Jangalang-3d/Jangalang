package jangalang.common.net.messages;

import java.io.Serializable;

public class InputPacket implements Serializable {
    private static final long serialVersionUID = 1L;
    public final int clientId;
    public final long tick;
    public final boolean forward;
    public final boolean backward;
    public final boolean left;
    public final boolean right;
    public final double mouseDelta;
    public final double viewAngle;

    public InputPacket(int clientId, long tick, boolean forward, boolean backward, boolean left, boolean right, double mouseDelta, double viewAngle) {
        this.clientId = clientId;
        this.tick = tick;
        this.forward = forward;
        this.backward = backward;
        this.left = left;
        this.right = right;
        this.mouseDelta = mouseDelta;
        this.viewAngle = viewAngle;
    }
}
