package it.polimi.ds.vsync.view.message;

import com.google.gson.Gson;
import it.polimi.ds.utils.MessageGsonBuilder;
import it.polimi.ds.vsync.view.HostInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
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
            InitialTopologyMessage message = new InitialTopologyMessage(new ArrayList<>());
            String json = gson.toJson(message);
            System.out.println(json);
        }

        @Test
        @DisplayName("Serialization of initial topology message with view")
        void TopologySerialization(){
            Gson gson = new MessageGsonBuilder().registerViewMessageAdapter().create();
            ArrayList<HostInfo> topology = new ArrayList<HostInfo>();
            topology.add(new HostInfo(UUID.randomUUID(), InetAddress.getLoopbackAddress()));
            topology.add(new HostInfo(UUID.randomUUID(), InetAddress.getLoopbackAddress()));
            InitialTopologyMessage message = new InitialTopologyMessage(topology);
            String json = gson.toJson(message);
            System.out.println(json);
        }

        @Test
        @DisplayName("Deserialization of initial topology message with empty view")
        void EmptyTopologyDeSerialization(){
            Gson gson = new MessageGsonBuilder().registerViewMessageAdapter().create();
            InitialTopologyMessage message = new InitialTopologyMessage(new ArrayList<>());
            String json = gson.toJson(message);
            InitialTopologyMessage deserializedMessage = (InitialTopologyMessage) gson.fromJson(json, ViewManagerMessage.class);
            assertEquals(message, deserializedMessage);
        }

        @Test
        @DisplayName("Deserialization of initial topology message with view")
        void TopologyDeSerialization(){
            Gson gson = new MessageGsonBuilder().registerViewMessageAdapter().create();
            ArrayList<HostInfo> topology = new ArrayList<HostInfo>();
            topology.add(new HostInfo(UUID.randomUUID(), InetAddress.getLoopbackAddress()));
            topology.add(new HostInfo(UUID.randomUUID(), InetAddress.getLoopbackAddress()));
            InitialTopologyMessage message = new InitialTopologyMessage(topology);
            String json = gson.toJson(message);
            InitialTopologyMessage deserializedMessage = (InitialTopologyMessage) gson.fromJson(json, ViewManagerMessage.class);
            assertEquals(message, deserializedMessage);
        }
    }
}
