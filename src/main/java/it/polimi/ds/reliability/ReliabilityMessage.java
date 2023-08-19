package it.polimi.ds.reliability;

import it.polimi.ds.utils.ScalarClock;
import it.polimi.ds.vsync.KnowledgeableMessage;

import java.util.UUID;

public class ReliabilityMessage {
    public final KnowledgeableMessage payload;

    public final UUID messageID;

    public final UUID referenceMessageID;

    public final MessageType messageType;

    public final ScalarClock timestamp;

    public ReliabilityMessage(UUID messageID, KnowledgeableMessage payload, ScalarClock timestamp) {
        this.messageID = messageID;
        this.messageType = MessageType.DATA;
        this.payload = payload;
        this.referenceMessageID = messageID;
        this.timestamp = timestamp;
    }

    protected ReliabilityMessage(UUID messageID, UUID referenceMessageID, ScalarClock timestamp) {
        this.messageID = messageID;
        this.messageType = MessageType.ACK;
        this.payload = null;
        this.referenceMessageID = referenceMessageID;
        this.timestamp = timestamp;
    }
}
