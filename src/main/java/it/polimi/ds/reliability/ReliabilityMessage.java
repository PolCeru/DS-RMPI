package it.polimi.ds.reliability;

import it.polimi.ds.communication.MessageType;
import it.polimi.ds.vsync.VSyncMessage;

import java.util.UUID;

public class ReliabilityMessage {
    private final VSyncMessage payload;

    private final UUID messageID;

    private final UUID referenceMessageID;

    private final MessageType messageType;

    protected ReliabilityMessage(UUID messageID, VSyncMessage payload) {
        this.messageID = messageID;
        this.messageType = MessageType.DATA;
        this.payload = payload;
        this.referenceMessageID = messageID;
    }

    protected ReliabilityMessage(UUID messageID, UUID referenceMessageID) {
        this.messageID = messageID;
        this.messageType = MessageType.ACK;
        this.payload = null;
        this.referenceMessageID = referenceMessageID;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public UUID getReferenceMessageID() {
        return referenceMessageID;
    }

    public UUID getMessageID() {
        return messageID;
    }
}
