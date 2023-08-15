package it.polimi.ds.communication.message;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;

/**
 * Discovery messages are used to discover the network topology. They are sent by a node to its neighbors when it wants
 * to join the network. The message contains a random number that is used to identify the sender.
 */
public class DiscoveryMessage extends BasicMessage {

    final int random;

    public DiscoveryMessage(LocalDateTime timestamp, UUID senderUID, int random) {
        super(timestamp, senderUID, MessageType.DISCOVERY);
        this.random = random;
    }

    protected DiscoveryMessage(LocalDateTime timestamp, UUID senderUID) {
        super(timestamp, senderUID, MessageType.DISCOVERY);
        this.random = new Random().nextInt();
    }

    public int getRandom() {return random;}
}
