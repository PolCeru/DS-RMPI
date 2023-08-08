package it.polimi.ds.communication.message;

import it.polimi.ds.communication.CommunicationLayer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;


class BasicMessageTest {

    @Nested
    @DisplayName("Test BasicMessage (de)serialization and equivalency")
    class SerializationTests {

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
                Method privateMethod1 = CommunicationLayer.class.getDeclaredMethod("decodeMessage", byte[].class, int.class);
                privateMethod1.setAccessible(true);
                DiscoveryMessage message1 = (DiscoveryMessage) privateMethod1.invoke(CommunicationLayer.class,
                        bytes, bytes.length);
                assertEquals(message, message1);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }
        @Test
        void DataMessageDeserialization() {
            DataMessage initialMessage = new DataMessage(LocalDateTime.now(), UUID.randomUUID(), null);
            try {
                Method privateMethod = CommunicationLayer.class.getDeclaredMethod("encodeMessage", BasicMessage.class);
                privateMethod.setAccessible(true);
                byte[] bytes = (byte[]) privateMethod.invoke(CommunicationLayer.class, initialMessage);
                Method privateMethod1 = CommunicationLayer.class.getDeclaredMethod("decodeMessage", byte[].class, int.class);
                privateMethod1.setAccessible(true);
                DataMessage deserializedMessage = (DataMessage) privateMethod1.invoke(CommunicationLayer.class,
                        bytes, bytes.length);
                assertEquals(initialMessage, deserializedMessage);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                fail(e);
            }
        }
    }
}