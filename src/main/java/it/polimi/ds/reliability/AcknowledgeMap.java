package it.polimi.ds.reliability;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public class AcknowledgeMap {
    final Map<UUID, MessageState> ackMap = new HashMap<>();

    public void sendMessage(UUID messageId, List<UUID> recipients) {
        ackMap.put(messageId, MessageState.ofMessage(recipients));
    }

    public void receiveMessage(UUID messageId, List<UUID> recipients) {
        if (isPresent(messageId)) {
            ackMap.get(messageId).setMessageReceived();
        } else {
            ackMap.put(messageId, MessageState.ofMessage(recipients));
        }
    }

    private boolean isPresent(UUID messageId) {
        return ackMap.containsKey(messageId);
    }

    public void receiveAck(UUID messageId, UUID ackHostId, List<UUID> recipients) {
        if (isPresent(messageId)) {
            ackMap.get(messageId).setAck(ackHostId);
        } else {
            ackMap.put(messageId, MessageState.ofAck(ackHostId, recipients));
        }
    }

    public boolean isComplete(UUID messageId) {
        MessageState state = ackMap.get(messageId);
        return state.messageReceived && state.ackMap.values().stream().allMatch(Boolean.TRUE::equals);
    }

    public List<UUID> missingAcks(UUID messageId) {
        MessageState state = ackMap.get(messageId);
        return state.ackMap.entrySet().stream().filter(entry -> entry.getValue().equals(Boolean.FALSE)).map(Map.Entry::getKey).toList();
    }

    public synchronized void remove(UUID messageId) {
        ackMap.remove(messageId);
        notify();
    }

    public synchronized void waitEmpty() throws InterruptedException {
        while (!ackMap.isEmpty()) {
            wait();
        }
    }

    private static class MessageState {
        private boolean messageReceived;
        private final Map<UUID, Boolean> ackMap;

        private MessageState(boolean messageReceived, Map<UUID, Boolean> ackMap) {
            this.messageReceived = messageReceived;
            this.ackMap = ackMap;
        }

        public static MessageState ofMessage(List<UUID> recipients) {
            return new MessageState(true, recipients.stream().collect(Collectors.toMap(Function.identity(), ignored -> Boolean.FALSE)));
        }

        public static MessageState ofAck(UUID senderUid, List<UUID> recipients) {
            Map<UUID, Boolean> hashMap = recipients.stream().collect(Collectors.toMap(Function.identity(), ignored -> Boolean.FALSE));
            hashMap.put(senderUid, Boolean.TRUE);
            return new MessageState(false, hashMap);
        }

        public void setAck(UUID id) {
            this.ackMap.put(id, Boolean.TRUE);
        }

        public void setMessageReceived() {
            this.messageReceived = true;
        }
    }
}
