package it.polimi.ds.comunication;

import java.time.LocalDateTime;
import java.util.UUID;

public class DiscoveryMessage extends BasicMessage {
    protected DiscoveryMessage(LocalDateTime timestamp, UUID senderUID, MessageType messageType) {
        super(timestamp, senderUID, messageType);
    }
}
