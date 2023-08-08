package it.polimi.ds.communication.message;

import it.polimi.ds.reliability.ReliabilityMessage;

import java.time.LocalDateTime;
import java.util.UUID;

public class DataMessage extends BasicMessage {

    /**
     * attribute to store the payload in form of byte array
     */
    private final ReliabilityMessage payload;

    public DataMessage(LocalDateTime timestamp, UUID senderUID, ReliabilityMessage payload) {
        super(timestamp, senderUID, MessageType.DATA);
        this.payload = payload;
    }

    public ReliabilityMessage getPayload() {
        return payload;
    }
}
