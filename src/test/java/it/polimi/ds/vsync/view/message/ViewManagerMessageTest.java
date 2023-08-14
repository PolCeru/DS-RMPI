package it.polimi.ds.vsync.view.message;

import com.google.gson.Gson;
import it.polimi.ds.utils.MessageGsonBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ViewManagerMessageTest {
    @Nested
    @DisplayName("Serialization test")
    class SerializationTest{
        @Test
        @DisplayName("Serialization of initial topology message with empty view")
        void EmptyTopologySerialization(){
            Gson gson = new MessageGsonBuilder().registerViewMessageAdapter().create();
            InitialTopologyMessage message = new InitialTopologyMessage(UUID.randomUUID(), new ArrayList<>());
            String json = gson.toJson(message);
            System.out.println(json);
        }

        @Test
        @DisplayName("Serialization of initial topology message with view")
        void TopologySerialization(){
            Gson gson = new MessageGsonBuilder().registerViewMessageAdapter().create();
            ArrayList<UUID> topology = new ArrayList<>();
            topology.add(UUID.randomUUID());
            topology.add(UUID.randomUUID());
            InitialTopologyMessage message = new InitialTopologyMessage(UUID.randomUUID(), topology);
            String json = gson.toJson(message);
            System.out.println(json);
        }

        @Test
        @DisplayName("Deserialization of initial topology message with empty view")
        void EmptyTopologyDeSerialization(){
            Gson gson = new MessageGsonBuilder().registerViewMessageAdapter().create();
            InitialTopologyMessage message = new InitialTopologyMessage(UUID.randomUUID(), new ArrayList<>());
            String json = gson.toJson(message);
            InitialTopologyMessage deserializedMessage = (InitialTopologyMessage) gson.fromJson(json, ViewManagerMessage.class);
            assertEquals(message, deserializedMessage);
        }

        @Test
        @DisplayName("Deserialization of initial topology message with view")
        void TopologyDeSerialization(){
            Gson gson = new MessageGsonBuilder().registerViewMessageAdapter().create();
            ArrayList<UUID> topology = new ArrayList<>();
            topology.add(UUID.randomUUID());
            topology.add(UUID.randomUUID());
            InitialTopologyMessage message = new InitialTopologyMessage(UUID.randomUUID(), topology);
            String json = gson.toJson(message);
            InitialTopologyMessage deserializedMessage = (InitialTopologyMessage) gson.fromJson(json, ViewManagerMessage.class);
            assertEquals(message, deserializedMessage);
        }
    }
}
