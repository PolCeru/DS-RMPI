package it.polimi.ds.lib.vsync.view.message;

public class CheckpointMessage extends ViewManagerMessage{

    public CheckpointMessage(){
        super(ViewChangeType.CHECKPOINT);
    }

}
