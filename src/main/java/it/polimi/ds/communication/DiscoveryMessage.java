package it.polimi.ds.communication;

import java.time.LocalDateTime;
import java.util.UUID;

public class DiscoveryMessage extends BasicMessage {

    final int port;

    protected DiscoveryMessage(LocalDateTime timestamp, UUID senderUID, int port) {
        super(timestamp, senderUID, MessageType.DISCOVERY);
        this.port = port;
    }

    public int getPort() {
        return port;
    }
}
