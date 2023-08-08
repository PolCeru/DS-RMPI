package it.polimi.ds.reliability;

import it.polimi.ds.communication.message.MessageType;
import it.polimi.ds.vsync.VSyncMessage;

import java.util.UUID;

public class ReliabilityMessage {
    private final VSyncMessage payload;

    private final UUID messageID;

    private final MessageType messageType;

    public ReliabilityMessage(UUID messageID, MessageType messageType, VSyncMessage payload) {
        this.messageID = messageID;
        this.messageType = messageType;
        this.payload = payload;
    }

    public MessageType getMessageType() {
        return messageType;
    }
}
