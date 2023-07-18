package it.polimi.ds.communication;

import java.time.LocalDateTime;
import java.util.UUID;

public class DiscoveryMessage extends BasicMessage {
    protected DiscoveryMessage(LocalDateTime timestamp, UUID senderUID) {
        super(timestamp, senderUID, MessageType.DISCOVERY);
    }
}
