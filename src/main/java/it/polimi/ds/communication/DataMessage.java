package it.polimi.ds.communication;

import java.time.LocalDateTime;
import java.util.UUID;

public class DataMessage extends BasicMessage {
    protected DataMessage(LocalDateTime timestamp, UUID senderUID, MessageType messageType) {
        super(timestamp, senderUID, messageType);
    }
}
