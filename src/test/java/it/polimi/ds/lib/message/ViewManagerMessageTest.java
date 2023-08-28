package it.polimi.ds.lib.message;

import com.google.gson.Gson;
import it.polimi.ds.lib.communication.CommunicationLayer;
import it.polimi.ds.lib.communication.message.BasicMessage;
import it.polimi.ds.lib.communication.message.DataMessage;
import it.polimi.ds.lib.reliability.ReliabilityMessage;
import it.polimi.ds.lib.reliability.ScalarClock;
import it.polimi.ds.lib.utils.MessageGsonBuilder;
import it.polimi.ds.lib.vsync.faultTolerance.FaultRecovery;
import it.polimi.ds.lib.vsync.view.ViewManagerBuilder;
import it.polimi.ds.lib.vsync.view.message.InitialTopologyMessage;
import it.polimi.ds.lib.vsync.view.message.ViewManagerMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

public class ViewManagerMessageTest {
    @Nested
    @DisplayName("Serialization test")
    class SerializationTest {
        @Test
        @DisplayName("Serialization of initial topology message with empty view")
        void EmptyTopologySerialization() {
            Gson gson = new MessageGsonBuilder().registerViewMessageAdapter().registerKnowledgeableMessage().create();
            InitialTopologyMessage message = new InitialTopologyMessage(UUID.randomUUID(), 0,
                    new ArrayList<>(), null, 0);
            String json = gson.toJson(message);
            System.out.println(json);
        }

        @Test
        @DisplayName("Serialization of initial topology message with view")
        void TopologySerialization() {
            Gson gson = new MessageGsonBuilder().registerViewMessageAdapter().registerKnowledgeableMessage().create();
            ArrayList<UUID> topology = new ArrayList<>();
            topology.add(UUID.randomUUID());
            topology.add(UUID.randomUUID());
            InitialTopologyMessage message = new InitialTopologyMessage(UUID.randomUUID(), 0,
                    topology, null, 0);
            String json = gson.toJson(message);
            System.out.println(json);
        }

        @Test
        @DisplayName("Deserialization of initial topology message with empty view")
        void EmptyTopologyDeSerialization() {
            Gson gson = new MessageGsonBuilder().registerViewMessageAdapter().registerKnowledgeableMessage().create();
            InitialTopologyMessage message = new InitialTopologyMessage(UUID.randomUUID(), 0,
                    new ArrayList<>(), null, 0);
            String json = gson.toJson(message);
            InitialTopologyMessage deserializedMessage = (InitialTopologyMessage) gson.fromJson(json, ViewManagerMessage.class);
            assertEquals(message, deserializedMessage);
        }

        @Test
        @DisplayName("Deserialization of initial topology message with view")
        void TopologyDeSerialization() {
            Gson gson = new MessageGsonBuilder().registerViewMessageAdapter().registerKnowledgeableMessage().create();
            ArrayList<UUID> topology = new ArrayList<>();
            topology.add(UUID.randomUUID());
            topology.add(UUID.randomUUID());
            InitialTopologyMessage message = new InitialTopologyMessage(UUID.randomUUID(), 0,
                    topology, null, 0);
            String json = gson.toJson(message);
            InitialTopologyMessage deserializedMessage = (InitialTopologyMessage) gson.fromJson(json, ViewManagerMessage.class);
            assertEquals(message, deserializedMessage);
        }
    }

    @Nested
    @DisplayName("Encode & decode message into/from bytes")
    class DecodingEncodingTest {
        @Test
        void testEncodingInitialTopology() {
            CommunicationLayer communicationLayer =
                    CommunicationLayer.defaultConfiguration(new ViewManagerBuilder(null, new FaultRecovery(null)));
            InitialTopologyMessage initialTopologyMessage = new InitialTopologyMessage(null, 0,
                    null, null, 0);
            ReliabilityMessage reliabilityMessage = new ReliabilityMessage(null, initialTopologyMessage, new ScalarClock(0, 0));
            DataMessage dataMessage = new DataMessage(LocalDateTime.now(), null, reliabilityMessage);
            try {
                Method encodeMethod = CommunicationLayer.class.getDeclaredMethod("encodeMessage", BasicMessage.class);
                encodeMethod.setAccessible(true);
                byte[] buffer = (byte[]) encodeMethod.invoke(communicationLayer, dataMessage);
                System.out.println(new String(buffer, StandardCharsets.UTF_8));
            } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        @Test
        void testDecodingInitialTopology() {
            CommunicationLayer communicationLayer =
                    CommunicationLayer.defaultConfiguration(new ViewManagerBuilder(null, new FaultRecovery(null)));
            String json = "{\"payload\":{\"payload\":{\"topology\":null,\"viewManagerId\":null,\"messageType\":\"INIT_VIEW\",\"knowledgeableMessageType\":\"VIEW\"},\"messageID\":null,\"referenceMessageID\":null,\"messageType\":\"DATA\"},\"timestamp\":\"1692140967076\",\"senderUID\":null,\"messageType\":\"DATA\"}";
            byte[] buffer = json.getBytes();
            try {
                Method decodeMethod = CommunicationLayer.class.getDeclaredMethod("decodeMessage", byte[].class, int.class);
                decodeMethod.setAccessible(true);
                DataMessage message = (DataMessage) decodeMethod.invoke(communicationLayer, buffer, buffer.length);
                assertInstanceOf(InitialTopologyMessage.class, message.payload.getPayload());
            } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
