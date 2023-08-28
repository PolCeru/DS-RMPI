package it.polimi.ds.lib.reliability;

import it.polimi.ds.lib.reliability.AcknowledgeMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

class AcknowledgeMapTest {
    @Test
    @Timeout(2)
    void testWaitEmptyWithNewMap() throws InterruptedException {
        AcknowledgeMap acknowledgeMap = new AcknowledgeMap();
        acknowledgeMap.waitEmpty();
    }

    @Test
    @Timeout(2)
    void testWaitEmptyWith1Message() throws InterruptedException {
        AcknowledgeMap acknowledgeMap = new AcknowledgeMap();
        UUID messageId = UUID.randomUUID();
        UUID recipientId = UUID.randomUUID();
        acknowledgeMap.sendMessage(messageId, Collections.singletonList(recipientId));
        acknowledgeMap.receiveAck(messageId, recipientId, Collections.singletonList(recipientId));
        if (acknowledgeMap.isComplete(messageId))
            acknowledgeMap.remove(messageId);
        acknowledgeMap.waitEmpty();
    }

    @Test
    @Timeout(2)
    void testWaitEmptyBeforeEmptyMap() throws InterruptedException {
        AcknowledgeMap acknowledgeMap = new AcknowledgeMap();
        UUID messageId = UUID.randomUUID();
        UUID recipientId = UUID.randomUUID();
        acknowledgeMap.sendMessage(messageId, Collections.singletonList(recipientId));
        Thread thread = new Thread(() -> {
            try {
                acknowledgeMap.waitEmpty();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        thread.start();
        acknowledgeMap.receiveAck(messageId, recipientId, Collections.singletonList(recipientId));
        if (acknowledgeMap.isComplete(messageId))
            acknowledgeMap.remove(messageId);
        thread.join();
    }

    @Test
    @Timeout(2)
    void testWaitEmptyBeforeEmptyMapMultipleInsertion() throws InterruptedException {
        AcknowledgeMap acknowledgeMap = new AcknowledgeMap();
        List<UUID> messageId = new ArrayList<>(List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()));
        UUID recipientId = UUID.randomUUID();
        for (UUID uuid : messageId) {
            acknowledgeMap.sendMessage(uuid, Collections.singletonList(recipientId));
        }
        Collections.shuffle(messageId);
        Thread thread = new Thread(() -> {
            try {
                acknowledgeMap.waitEmpty();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        thread.start();
        for (UUID uuid : messageId) {
            acknowledgeMap.receiveAck(uuid, recipientId, Collections.singletonList(recipientId));
            if (acknowledgeMap.isComplete(uuid))
                acknowledgeMap.remove(uuid);
        }
        thread.join();
    }
}