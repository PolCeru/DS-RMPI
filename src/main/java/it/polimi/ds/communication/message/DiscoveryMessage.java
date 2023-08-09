package it.polimi.ds.communication.message;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;

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
