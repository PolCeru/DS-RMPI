package it.polimi.ds.comunication;

import java.time.LocalDateTime;
import java.util.UUID;

public abstract class BasicMessage {
    private final LocalDateTime timestamp;
    private final UUID senderUID;
    private final MessageType messageType;

    protected BasicMessage(LocalDateTime timestamp, UUID senderUID, MessageType messageType) {
        this.timestamp = timestamp;
        this.senderUID = senderUID;
        this.messageType = messageType;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public UUID getSenderUID() {
        return senderUID;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}
