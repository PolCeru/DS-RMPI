package it.polimi.ds.communication;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;


class CommunicationLayerTest {

    @Test
    void discoveryMessageSerialization() {
        LocalDateTime time = LocalDateTime.now();
        UUID uuid = UUID.randomUUID();
        DiscoveryMessage message = new DiscoveryMessage(time, uuid, 4445);
        try {
            Method privateMethod = CommunicationLayer.class.getDeclaredMethod("encodeMessage", BasicMessage.class);
            privateMethod.setAccessible(true);
            byte[] bytes = (byte[]) privateMethod.invoke(CommunicationLayer.class, message);
            String json = new String(bytes, StandardCharsets.UTF_8);
            assertTrue(json.contains(String.valueOf(time.toInstant(ZoneOffset.UTC).toEpochMilli())));
            assertTrue(json.contains(uuid.toString()));
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
    @Test
    void discoveryMessageDeserialization() {
        LocalDateTime time = LocalDateTime.now();
        UUID uuid = UUID.randomUUID();
        DiscoveryMessage message = new DiscoveryMessage(time, uuid, 4445);
        try {
            Method privateMethod = CommunicationLayer.class.getDeclaredMethod("encodeMessage", BasicMessage.class);
            privateMethod.setAccessible(true);
            byte[] bytes = (byte[]) privateMethod.invoke(CommunicationLayer.class, message);
            Method privateMethod1 = CommunicationLayer.class.getDeclaredMethod("decodeMessage", byte[].class);
            privateMethod1.setAccessible(true);
            DiscoveryMessage message1 = (DiscoveryMessage) privateMethod1.invoke(CommunicationLayer.class,
                    (Object) bytes);
            assertEquals(message, message1);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}