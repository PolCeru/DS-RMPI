package it.polimi.ds.reliability;

import it.polimi.ds.communication.ClientHandler;
import it.polimi.ds.communication.CommunicationLayer;
import it.polimi.ds.communication.message.DataMessage;
import it.polimi.ds.vsync.VSyncLayer;
import it.polimi.ds.vsync.VSyncMessage;
import it.polimi.ds.vsync.view.ViewManagerBuilder;
import jdk.jfr.Description;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

/**
 * Test class for the ReliabilityLayer
 */
public class ReliabilityLayerTest {
    @Test
    @DisplayName("Read DATA Message Test")
    @Description("Tests that the ReliabilityLayer sends an ACK message when it receives a DataMessage")
    public void testReadDataMessage() {
        // mock creation
        VSyncLayer mockVSL = mock(VSyncLayer.class);
        ViewManagerBuilder VMB = new ViewManagerBuilder(mockVSL);
        CommunicationLayer mockCL = mock(CommunicationLayer.class);
        UUID senderUUID = UUID.randomUUID();
        UUID dataMessageUUID = UUID.randomUUID();
        ReliabilityMessage dataMessage = new ReliabilityMessage(dataMessageUUID, new VSyncMessage("test".getBytes()), new ScalarClock(0, 0));

        // stubbing
        when(mockCL.isConnected()).thenReturn(true).thenReturn(false);
        when(mockCL.getMessage()).thenReturn(new DataMessage(LocalDateTime.now(), senderUUID, dataMessage));

        doAnswer(invocation -> {
            UUID passedClientID = invocation.getArgument(0);
            ReliabilityMessage passedMessage = invocation.getArgument(1);
            // check that the message is sent to the correct client
            assertEquals(senderUUID, passedClientID);
            assertEquals(passedMessage.getMessageType(), MessageType.ACK);
            assertEquals(passedMessage.getReferenceMessageID(), dataMessageUUID);
            return null;
        }).when(mockCL).sendMessage(any(UUID.class), any(ReliabilityMessage.class));

        //setup ReliabilityLayer
        ReliabilityLayer RL = new ReliabilityLayer(VMB);
        RL.setCommunicationLayer(mockCL);

        // check that the message is received and an ACK is sent
        verify(mockCL).isConnected();
        verify(mockCL).getMessage();
        verify(mockCL).sendMessage(eq(senderUUID), any(ReliabilityMessage.class));

    }

    @Test
    @DisplayName("Send DATA Message Test")
    @Description("Tests the sending of a DATA message for each connected client")
    public void testReadAckMessage() {
        // mock creation
        VSyncLayer senderMockVSL = mock(VSyncLayer.class);
        VSyncLayer mockVSL = mock(VSyncLayer.class);
        VSyncLayer mockVSL2 = mock(VSyncLayer.class);
        ViewManagerBuilder senderVMB = new ViewManagerBuilder(senderMockVSL);
        ViewManagerBuilder VMB = new ViewManagerBuilder(mockVSL);
        ViewManagerBuilder VMB2 = new ViewManagerBuilder(mockVSL2);
        CommunicationLayer senderMockCL = mock(CommunicationLayer.class);
        CommunicationLayer mockCL = mock(CommunicationLayer.class);
        CommunicationLayer mockCL2 = mock(CommunicationLayer.class);

        //setup ReliabilityLayer
        ReliabilityLayer senderRL = new ReliabilityLayer(senderVMB);
        ReliabilityLayer RL = new ReliabilityLayer(VMB);
        ReliabilityLayer RL2 = new ReliabilityLayer(VMB2);
        when(senderMockVSL.getHandler()).thenReturn(senderRL);
        when(mockVSL.getHandler()).thenReturn(RL);
        when(mockVSL2.getHandler()).thenReturn(RL2);
        senderRL.setCommunicationLayer(senderMockCL);
        RL.setCommunicationLayer(mockCL);
        RL2.setCommunicationLayer(mockCL2);

        //setup connected clients
        ClientHandler senderCH = null, receiverCH = null, receiverCH2 = null;
        try {
            senderCH = new ClientHandler(senderRL.getViewManager().getClientUID(), new Socket(), senderMockCL);
            receiverCH = new ClientHandler(RL.getViewManager().getClientUID(), new Socket(), mockCL);
            receiverCH2 = new ClientHandler(RL2.getViewManager().getClientUID(), new Socket(), mockCL2);
        } catch (IOException e) {
            System.out.println("Error creating ClientHandler");
        }
        VSyncMessage messageToSend = new VSyncMessage("test".getBytes());

        HashMap<UUID, ClientHandler> connectedClientMock = new HashMap<>();
        connectedClientMock.put(senderRL.getViewManager().getClientUID(), senderCH);
        connectedClientMock.put(RL.getViewManager().getClientUID(), receiverCH);
        connectedClientMock.put(RL2.getViewManager().getClientUID(), receiverCH2);

        //fake sending of a message
        when(senderMockCL.getConnectedClients()).thenReturn(connectedClientMock);
        when(senderMockCL.isConnected()).thenReturn(true).thenReturn(false);
        senderMockVSL.getHandler().sendMessage(messageToSend);

        // check that the message is sent to the correct clients by capturing the UUIDs of the clients
        ArgumentCaptor<UUID> uuidCaptor = ArgumentCaptor.forClass(UUID.class);
        verify(senderMockCL, times(2)).sendMessage(uuidCaptor.capture(), any(ReliabilityMessage.class));

        ArrayList<UUID> capturedUUIDs = (ArrayList<UUID>) uuidCaptor.getAllValues();
        assertTrue(capturedUUIDs.contains(RL.getViewManager().getClientUID()));
        assertTrue(capturedUUIDs.contains(RL2.getViewManager().getClientUID()));
        verify(senderMockCL).sendMessageBroadcast(any(ReliabilityMessage.class));
    }
}


