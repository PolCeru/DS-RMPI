package it.polimi.ds.lib.communication.message;

import it.polimi.ds.lib.reliability.ReliabilityMessage;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Data messages are used to send data between nodes. They are sent by a node to its neighbors when it wants
 * to send data to the network. The message contains the payload to be sent which is a ReliabilityMessage.
 */
public class DataMessage extends BasicMessage {

    /**
     * attribute to store the payload in form of byte array
     */
    public final ReliabilityMessage payload;

    public DataMessage(LocalDateTime timestamp, UUID senderUID, ReliabilityMessage payload) {
        super(timestamp, senderUID, MessageType.DATA);
        this.payload = payload;
    }
}
