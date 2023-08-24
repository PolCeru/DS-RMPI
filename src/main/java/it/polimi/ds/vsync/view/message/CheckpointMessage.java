package it.polimi.ds.vsync.view.message;

public class CheckpointMessage extends ViewManagerMessage{

    public CheckpointMessage(){
        super(ViewChangeType.CHECKPOINT);
    }

}
