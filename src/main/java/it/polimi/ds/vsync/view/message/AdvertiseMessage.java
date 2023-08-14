package it.polimi.ds.vsync.view.message;

import java.net.InetAddress;
import java.util.UUID;

public class AdvertiseMessage extends ViewManagerMessage {
    public final InetAddress newHostAddress;
    public final UUID newHostId;

    public AdvertiseMessage(InetAddress newHostAddress, UUID newHostId) {
        super(ViewChangeType.ADVERTISE);
        this.newHostAddress = newHostAddress;
        this.newHostId = newHostId;
    }
}
