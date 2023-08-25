package it.polimi.ds.vsync.view.message;

import it.polimi.ds.vsync.faultTolerance.Checkpoint;

import java.util.ArrayList;

public class RecoveryRequestMessage extends ViewManagerMessage{

    public ArrayList<Checkpoint> checkpoints = new ArrayList<>();

    public final int lastCheckpointID;

    public RecoveryRequestMessage(int lastCheckpointID){
        super(ViewChangeType.RECOVERY_REQUEST);
        this.lastCheckpointID = lastCheckpointID;
    }

}