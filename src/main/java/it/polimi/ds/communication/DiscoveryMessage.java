package it.polimi.ds.communication;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;

public class DiscoveryMessage extends BasicMessage {

    final int port;
    final int random;

    protected DiscoveryMessage(LocalDateTime timestamp, UUID senderUID, int port, int random) {
        super(timestamp, senderUID, MessageType.DISCOVERY);
        this.port = port;
        this.random = random;
    }

    protected DiscoveryMessage(LocalDateTime timestamp, UUID senderUID, int port){
        super(timestamp, senderUID, MessageType.DISCOVERY);
        this.port = port;
        this.random = new Random().nextInt();
    }

    public int getPort() {
        return port;
    }

    public int getRandom() {return random;}
}
