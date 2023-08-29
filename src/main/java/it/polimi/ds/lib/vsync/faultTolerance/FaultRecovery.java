package it.polimi.ds.lib.vsync.faultTolerance;

import com.google.gson.Gson;
import it.polimi.ds.lib.reliability.ReliabilityLayer;
import it.polimi.ds.lib.reliability.ReliabilityMessage;
import it.polimi.ds.lib.reliability.ScalarClock;
import it.polimi.ds.lib.vsync.VSyncLayer;
import it.polimi.ds.lib.vsync.VSyncMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedOutputStream;
import java.io.File;
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
    
    private final int LOG_THRESHOLD = 100;

    private String CHECKPOINTS_FILE_PATH;

    public final String RECOVERY_FILE_PATH;

    private final Properties properties = new Properties();

    public FaultRecovery(VSyncLayer vSyncLayer) {
        this.vSyncLayer = vSyncLayer;
        this.lock = new ReentrantLock();
        this.thresholdCondition = lock.newCondition();

        RECOVERY_FILE_PATH = System.getProperty("user.home") + File.separator + "recovery" + File.separator +
                "checkpoints" + File.separator + "_recoveryCounter.txt";


        File recoveryFile = new File(RECOVERY_FILE_PATH);
        File recoveryDirectory = recoveryFile.getParentFile();

        // Verifies whether directory exists, if not creates it
        if (!recoveryDirectory.exists()) {
            if (recoveryDirectory.mkdirs())
                logger.debug("Directory for file recovery created");
            else logger.error("Error creating directory");
        }
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
        saveCheckpoints(checkpointsToAdd);
        log.clear();
    }

    /**
     * This method creates a new checkpoint writing it into disk and emptying the log; then adds it to the list of
     * checkpoints incrementing the counter
     */
    public void doCheckpoint(){
        List<byte[]> byteList = log.stream().map(vSyncWrapper -> gson.toJson(vSyncWrapper).getBytes()).toList();
        Checkpoint checkpoint = new Checkpoint(checkpointCounter, byteList);
        if (!byteList.isEmpty()){
            writeCheckpointOnFile(byteList);
            checkpoints.add(checkpoint);
            logger.info("Checkpoint " + (checkpointCounter) + " created successfully");
            log.clear();
            logger.trace("Log cleared after checkpoint " + (checkpointCounter));
            checkpointCounter++;
        } else logger.debug("There was no need to create a checkpoint, the log is empty");
    }

    /**
     * This method search for the requested checkpoint and returns every checkpoint after it in the list (including
     * it) and the current content of the log in a recovery packet
     * @param checkpointID the checkpoint to be recovered
     * @return the recovery packet containing the requested checkpoints and the log
     */
    public ArrayList<Checkpoint> recoverCheckpoint(int checkpointID){
        ArrayList<Checkpoint> checkpointsToReturn = new ArrayList<>();
        for (Checkpoint checkpoint : checkpoints) {
            if(checkpoint.getCheckpointID() >= checkpointID){
                checkpointsToReturn.add(checkpoint);
            }
        }
        if(checkpointsToReturn.isEmpty()) throw new IllegalArgumentException("Checkpoint not found");
        else return checkpointsToReturn;
    }

    /**
     * This method writes the checkpoints on the disk
     * @param whatToWrite the checkpoints to be written in form of list of byte array
     */
    private void writeCheckpointOnFile(List<byte[]> whatToWrite) {
        CHECKPOINTS_FILE_PATH = System.getProperty("user.home") + File.separator + "recovery" + File.separator +
                "checkpoints" + File.separator +
                "Checkpoint" + checkpointCounter + ".bin";
        File file = new File(CHECKPOINTS_FILE_PATH);
        File recoveryFile = new File(RECOVERY_FILE_PATH);

        if (!recoveryFile.exists()) {
            try {
                if (recoveryFile.createNewFile()) logger.debug("File for recovery created");
                else logger.debug("Impossible to create file");
            } catch (IOException e) {
                logger.fatal("Error creating file: " + e.getMessage());
            }
        }

        if (!file.exists()) {
            try {
                if (file.createNewFile()) logger.debug("File for checkpoints created");
                else logger.debug("Impossible to create file");
            } catch (IOException e) {
                logger.fatal("Error creating file: " + e.getMessage());
            }
        }
        logger.debug("Writing checkpoint " + checkpointCounter + " to file");
        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(CHECKPOINTS_FILE_PATH))) {
            for (byte[] row : whatToWrite)
                bos.write(row);
        } catch (IOException e) {
            logger.fatal("Error writing checkpoint to file: " + e.getMessage());
        }

        logger.debug("Updating Checkpoint counter in the file");
        properties.setProperty("CheckpointCounter", String.valueOf(checkpointCounter));
        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(RECOVERY_FILE_PATH))) {
            properties.store(bos, "Checkpoint counter to be used in case of recovery");
        } catch (IOException e) {
            logger.fatal("Error writing checkpoint to file: " + e.getMessage());
        }
    }

    private void saveCheckpoints(List<Checkpoint> whatToWrite) {
        File recoveryFile = new File(RECOVERY_FILE_PATH);

        if (!recoveryFile.exists()) {
            try {
                if (recoveryFile.createNewFile()) logger.debug("File for recovery created");
                else logger.debug("Impossible to create file");
            } catch (IOException e) {
                logger.fatal("Error creating file: " + e.getMessage());
            }
        }

        logger.debug("Updating Checkpoint counter in the file");
        properties.setProperty("CheckpointCounter", String.valueOf(checkpointCounter));
        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(RECOVERY_FILE_PATH))) {
            properties.store(bos, "Checkpoint counter to be used in case of recovery");
        } catch (IOException e) {
            logger.fatal("Error writing checkpoint to file: " + e.getMessage());
        }

        for (Checkpoint checkpoint : whatToWrite) {
            CHECKPOINTS_FILE_PATH = System.getProperty("user.home") + File.separator + "recovery" + File.separator +
                    "checkpoints" + File.separator +
                    "Checkpoint" + checkpoint.getCheckpointID() + ".bin";
            File file = new File(CHECKPOINTS_FILE_PATH);

            if (!file.exists()) {
                try {
                    if (file.createNewFile()) logger.debug("File for checkpoints created");
                    else logger.debug("Impossible to create file");
                } catch (IOException e) {
                    logger.fatal("Error creating file: " + e.getMessage());
                }
            }
            logger.debug("Writing checkpoint " + checkpoint.getCheckpointID() + " to file");
            try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(CHECKPOINTS_FILE_PATH))) {
                for (byte[] row : whatToWrite.stream().map(ch -> gson.toJson(ch).getBytes()).toList())
                    bos.write(row);
            } catch (IOException e) {
                logger.fatal("Error writing checkpoint to file: " + e.getMessage());
            }

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
            logger.debug("Log size "+log.size());
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

    public void setCheckpointCounter(int checkpointCounter) {
        this.checkpointCounter = checkpointCounter;
        logger.debug("Updating Checkpoint counter in the file");
        properties.setProperty("CheckpointCounter", String.valueOf(checkpointCounter));
        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(RECOVERY_FILE_PATH))) {
            properties.store(bos, "Checkpoint counter to be used in case of recovery");
        } catch (IOException e) {
            logger.fatal("Error writing checkpoint to file: " + e.getMessage());
        }
    }

    public int getCheckpointCounter() {
        return checkpointCounter;
    }

    public List<VSyncWrapper> deserializeChecPoints(ArrayList<Checkpoint> checkpoints) {
        List<VSyncWrapper> reliabilityMessages = new ArrayList<>();
        for (Checkpoint checkpoint : checkpoints) {
            checkpoint.getMessages().stream().map(msg -> gson.fromJson(Arrays.toString(msg), VSyncWrapper.class)).forEach(reliabilityMessages::add);
        }
        return reliabilityMessages;
    }

    public record VSyncWrapper(VSyncMessage message, ScalarClock timestamp) implements Comparable<VSyncWrapper> {
        @Override
        public int compareTo(VSyncWrapper o) {
            return timestamp.compareTo(o.timestamp);
        }
    }

    public Properties getProperties() {
        return properties;
    }
}
