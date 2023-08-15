package it.polimi.ds.reliability;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AcknowledgeMap {
    final Map<UUID, Map<UUID, Boolean>> ackMap = new HashMap<>();

    synchronized public void newBroadcastMessage(UUID messageUUID, List<UUID> recipients) {
        HashMap<UUID, Boolean> map = new HashMap<>();
        recipients.forEach((id) -> map.put(id, Boolean.FALSE));
        ackMap.put(messageUUID, map);
    }

    synchronized public void ackMessage(UUID messageId, UUID receiver) {
        ackMap.get(messageId).put(receiver, Boolean.TRUE);
    }

    synchronized public boolean isAckByEveryone(UUID messageId) {
        return ackMap.get(messageId).values().stream().anyMatch((val) -> val.equals(Boolean.FALSE));
    }

    synchronized public void removeMessage(UUID messageId) {
        ackMap.remove(messageId);
    }

    synchronized public List<UUID> missingAck(UUID messageId) {
        return ackMap.get(messageId).entrySet().stream().filter(entry -> entry.getValue().equals(Boolean.FALSE)).map(Map.Entry::getKey).toList();
    }
}
