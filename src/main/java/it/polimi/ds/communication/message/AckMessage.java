package it.polimi.ds.communication.message;

import java.time.LocalDateTime;
import java.util.UUID;

public class AckMessage extends BasicMessage {

    private final UUID referenceNumber;
    protected AckMessage(LocalDateTime timestamp, UUID senderUID, MessageType messageType, UUID referenceNumber) {
        super(timestamp, senderUID, messageType);
        this.referenceNumber = referenceNumber;
    }

    public UUID getReferenceNumber() {return referenceNumber;}
}
