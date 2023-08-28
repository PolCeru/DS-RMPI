package it.polimi.ds.lib.reliability;

import it.polimi.ds.lib.vsync.KnowledgeableMessage;
import it.polimi.ds.lib.vsync.view.message.ViewManagerMessage;

import java.util.UUID;

public class ReliabilityMessage implements Comparable<ReliabilityMessage> {
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

    public ReliabilityMessage(UUID messageID, ViewManagerMessage payload, MessageType messageType, ScalarClock timestamp) {
        this.messageID = messageID;
        this.messageType = messageType;
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
    public KnowledgeableMessage getPayload() {
        return payload;
    }

    @Override
    public int compareTo(ReliabilityMessage o) {
        return timestamp.compareTo(o.timestamp);
    }
}
