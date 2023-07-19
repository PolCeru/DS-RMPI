package it.polimi.ds.reliability;

import it.polimi.ds.comunication.CommunicationLayer;
import it.polimi.ds.comunication.DataMessage;
import it.polimi.ds.comunication.MessageType;

import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ReliabilityLayer {
    private final int MAX_RETRIES = 2;
    private final int TIMEOUT = 10000;
    private final CommunicationLayer handler;
    private final HashMap<ReliabilityMessage, HashMap<UUID, Boolean>> ackMap = new HashMap<>();
    private final BlockingQueue<ReliabilityMessage> upBuffer = new LinkedBlockingQueue<>();
    private final BlockingQueue<ReliabilityMessage> downBuffer = new LinkedBlockingQueue<>();

    public ReliabilityLayer(CommunicationLayer handler) {
        this.handler = handler;
        new Thread(this::readMessage).start();
    }

    private void readMessage() {
        while (handler.isConnected()) {
            DataMessage dataMessage = (DataMessage) handler.getMessage();
            ReliabilityMessage message = dataMessage.getPayload();
            UUID senderUID = dataMessage.getSenderUID();

            if (ackMap.containsKey(message)) {
                HashMap<UUID, Boolean> singleAck = new HashMap<>();
                for (UUID uuid : handler.getConnectedClients().keySet())
                    singleAck.put(uuid, false);
                ackMap.put(message, singleAck);
            } else if (message.getMessageType() == MessageType.ACK && !ackMap.get(message).get(senderUID)) {
                ackMap.get(message).put(senderUID, true);
            }

            if (ackMap.get(message).values().stream().allMatch(b -> b)) {
                ackMap.remove(message); //TODO: check if this is correct, do we want to remove the message from the ackMap?
                upBuffer.add(message);
            }
        }
    }

    private void sendMessage(){
        try {
            handler.sendMessageBroadcast(downBuffer.take());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
