package it.polimi.ds.vsync.faultTolerance;

import com.google.gson.Gson;
import it.polimi.ds.reliability.ScalarClock;
import it.polimi.ds.vsync.VSyncLayer;
import it.polimi.ds.vsync.VSyncMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class FaultRecovery {

    private final static Logger logger = LogManager.getLogger();
    private final VSyncLayer vSyncLayer;

    private final SortedSet<VSyncWrapper> log = new TreeSet<>();

    private final ArrayList<Checkpoint> checkpoints = new ArrayList<>();

    private int checkpointCounter = 0;

    private final Gson gson = new Gson();

    private final ReentrantLock lock;
    
    private final Condition thresholdCondition;
    
    private final int LOG_THRESHOLD = 1024;

    private final String CHECKPOINTS_FILE_PATH = "checkpoints/Checkpoint" + checkpointCounter + ".bin";

    public final String RECOVERY_FILE_PATH = "checkpoints/_recoveryCounter.txt";

    private final Properties properties = new Properties();

    public FaultRecovery(VSyncLayer vSyncLayer) {
        this.vSyncLayer = vSyncLayer;
        this.lock = new ReentrantLock();
        this.thresholdCondition = lock.newCondition();
        new Thread(this::checkCondition, "logConditionChecker").start();
    }

    /**
     * This method adds the checkpoints received in the list and updates the checkpoint counter.
     * Then proceeds to write the checkpoints into disk.
     * in case the log in the checkpointsToAdd is not empty it adds the messages in the log in the correct order
     * @param checkpointsToAdd the recovery packet containing the checkpoint and the log
     */
    public void addMissingCheckpoints(ArrayList<Checkpoint> checkpointsToAdd){
        if(checkpointsToAdd.isEmpty()) return;
        checkpoints.addAll(checkpointsToAdd);
        checkpoints.sort(Comparator.comparingInt(Checkpoint::getCheckpointID));
        checkpointCounter = Math.max(checkpoints.get(checkpoints.size() - 1).getCheckpointID(), checkpointCounter) + 1;
        checkpointsToAdd.forEach(checkpoint -> writeCheckpointOnFile(checkpoint.getMessages()));
        log.clear();
    }

    /**
     * This method creates a new checkpoint writing it into disk and emptying the log; then adds it to the list of
     * checkpoints incrementing the counter
     */
    public void doCheckpoint(){
        List<byte[]> byteList = log.stream().map(vSyncWrapper -> gson.toJson(vSyncWrapper.message).getBytes()).toList();
        Checkpoint checkpoint = new Checkpoint(checkpointCounter, byteList);
        writeCheckpointOnFile(byteList);
        checkpoints.add(checkpoint);
        log.clear();
        logger.info("Checkpoint " + (checkpointCounter) + " created successfully");
        logger.trace("Log cleared after checkpoint " + (checkpointCounter));
        checkpointCounter++;
    }

    /**
     * This method search for the requested checkpoint and returns every checkpoint after it in the list (including
     * it) and the current content of the log in a recovery packet
     * @param checkpointID the checkpoint to be recovered
     * @return the recovery packet containing the requested checkpoints and the log
     */
    public ArrayList<Checkpoint> recoverCheckpoint(int checkpointID){
        for (Checkpoint checkpoint : checkpoints) {
            if(checkpoint.getCheckpointID() == checkpointID){
                ArrayList<Checkpoint> checkpointsToReturn = new ArrayList<>();
                for (int i = checkpointCounter; i < checkpoints.size(); i++) {
                    checkpointsToReturn.add(checkpoints.get(i));
                }
                return new ArrayList<>(checkpointsToReturn);
            } else throw new IllegalArgumentException("Checkpoint not found");
        }
        return null;
    }

    /**
     * This method writes the checkpoints on the disk
     * @param whatToWrite the checkpoints to be written in form of list of byte array
     */
    private void writeCheckpointOnFile(List<byte[]> whatToWrite) {
        logger.debug("Writing checkpoint " + checkpointCounter + " to file");
        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(CHECKPOINTS_FILE_PATH))) {
            for (byte[] row: whatToWrite)
                bos.write(row);
        } catch (IOException e) {
            logger.error("Error writing checkpoint to file\n" + e.getMessage());
            logger.error(e.getStackTrace());
        }

        logger.debug("Updating Checkpoint counter in the file");
        properties.setProperty("CheckpointCounter", String.valueOf(checkpointCounter));
        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(RECOVERY_FILE_PATH))) {
            properties.store(bos, "Checkpoint counter to be used in case of recovery");
        } catch (IOException e) {
            logger.error("Error writing checkpoint to file\n" + e.getMessage());
            logger.error(e.getStackTrace());
        }
    }

    /**
     * This method adds the message to the log and if the log is full starts the checkpoint procedure
     * @param message the message to be added to the log
     */
    public void logMessage(VSyncMessage message, ScalarClock timestamp) {
        lock.lock();
        try {
            log.add(new VSyncWrapper(message, timestamp));
            thresholdCondition.signalAll();
        } finally {
            lock.unlock();
        }
    }

    public void checkCondition(){
        lock.lock();
        try {
            while (log.size() < LOG_THRESHOLD) {
                // Wait for the log to reach the threshold to call handleCheckpoint
                thresholdCondition.await();
            }
            vSyncLayer.getViewManager().freezeAndCheckpoint();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }
    }

    private record VSyncWrapper(VSyncMessage message, ScalarClock timestamp) implements Comparable<VSyncWrapper> {
        @Override
        public int compareTo(VSyncWrapper o) {
            return timestamp.compareTo(o.timestamp);
        }
    }

    public Properties getProperties() {
        return properties;
    }
}
