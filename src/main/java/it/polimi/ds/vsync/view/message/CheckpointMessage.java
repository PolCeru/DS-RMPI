package it.polimi.ds.vsync.view.message;

import it.polimi.ds.vsync.faultTolerance.Checkpoint;

public class CheckpointMessage extends ViewManagerMessage{
    public final Checkpoint checkpoint;

    public CheckpointMessage(Checkpoint checkpoint){
        super(ViewChangeType.CHECKPOINT);
        this.checkpoint = checkpoint;
    }

}
