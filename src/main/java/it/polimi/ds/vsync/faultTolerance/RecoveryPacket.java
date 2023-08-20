package it.polimi.ds.vsync.faultTolerance;

import java.util.ArrayList;

public record RecoveryPacket(ArrayList<Checkpoint> checkpoints, ArrayList<Byte[]> log){

}
