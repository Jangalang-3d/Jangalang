package jangalang.common.net.messages;

import java.io.Serializable;

public class Disconnect implements Serializable {
    private static final long serialVersionUID = 1L;
    public final int id;

    public Disconnect(int id) {
        this.id = id;
    }
}
