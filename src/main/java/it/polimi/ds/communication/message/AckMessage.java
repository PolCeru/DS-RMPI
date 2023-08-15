package it.polimi.ds.communication.message;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Ack messages are used to acknowledge the reception of a message. They are sent by a node to its neighbors when it wants
 * to acknowledge the reception of a message. The message contains the reference number of the message that is being
 * acknowledged.
 */
public class AckMessage extends BasicMessage {

    private final UUID referenceNumber;
    protected AckMessage(LocalDateTime timestamp, UUID senderUID, MessageType messageType, UUID referenceNumber) {
        super(timestamp, senderUID, messageType);
        this.referenceNumber = referenceNumber;
    }

    public UUID getReferenceNumber() {return referenceNumber;}
}
