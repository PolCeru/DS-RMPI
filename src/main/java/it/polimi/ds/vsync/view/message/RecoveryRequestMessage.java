package it.polimi.ds.vsync.view.message;

import it.polimi.ds.vsync.faultTolerance.Checkpoint;

import java.util.ArrayList;
import java.util.UUID;

public class RecoveryRequestMessage extends ViewManagerMessage{

    public ArrayList<Checkpoint> checkpoints = new ArrayList<>();

    public final int lastCheckpointID;

    public final UUID senderUUID;

    public RecoveryRequestMessage(int lastCheckpointID, UUID uuid){
        super(ViewChangeType.RECOVERY_REQUEST);
        this.lastCheckpointID = lastCheckpointID;
        this.senderUUID = uuid;
    }

}