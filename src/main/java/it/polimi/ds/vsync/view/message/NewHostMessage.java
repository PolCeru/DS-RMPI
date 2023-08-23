package it.polimi.ds.vsync.view.message;

import java.net.InetAddress;
import java.util.UUID;

public class NewHostMessage extends ViewManagerMessage {
    public final InetAddress newHostAddress;
    public final UUID newHostId;
    public final int newHostRandom;

    public NewHostMessage(InetAddress newHostAddress, UUID newHostId, int newHostRandom) {
        super(ViewChangeType.NEW_HOST);
        this.newHostAddress = newHostAddress;
        this.newHostId = newHostId;
        this.newHostRandom = newHostRandom;
    }
}
