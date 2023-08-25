package it.polimi.ds.vsync.view.message;

import it.polimi.ds.vsync.faultTolerance.Checkpoint;

import java.util.ArrayList;

public class RecoveryPacketMessage extends ViewManagerMessage{

    public final ArrayList<Checkpoint> checkpoints;

    public RecoveryPacketMessage(ArrayList<Checkpoint> checkpoints){
        super(ViewChangeType.RECOVERY_PACKET);
        this.checkpoints = checkpoints;
    }
}
