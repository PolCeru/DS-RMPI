package it.polimi.ds.communication.message;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * BasicMessage is the abstract class that represents the basic structure of a message.
 * From BasicMessage are derived AckMessage, DataMessage and DiscoveryMessage.
 */
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

    private static boolean compareDate(LocalDateTime t1, LocalDateTime t2) {
        return t1.getDayOfYear() == t2.getDayOfYear() && t1.getYear() == t2.getYear() && t1.getSecond() == t2.getSecond() && t1.getMinute() == t2.getMinute() && t1.getHour() == t2.getHour();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BasicMessage that = (BasicMessage) o;
        return compareDate(this.timestamp, that.timestamp) && Objects.equals(senderUID,
                that.senderUID) && messageType == that.messageType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(timestamp, senderUID, messageType);
    }
}
