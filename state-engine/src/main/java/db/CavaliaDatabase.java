package db;

import common.collections.Configuration;
import durability.ftmanager.FTManager;
import durability.logging.LoggingStrategy.ImplLoggingManager.WALManager;
import durability.snapshot.SnapshotResult.SnapshotResult;
import storage.EventManager;
import storage.StorageManager;
import storage.TableRecord;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

/**
 * original designer for CavaliaDatabase: Yingjun Wu.
 */
public class CavaliaDatabase extends Database {
    public CavaliaDatabase(Configuration configuration) {
        storageManager = new StorageManager(configuration);
        eventManager = new EventManager();
        switch (configuration.getInt("FTOption")) {
            case 0 :
            case 1 :
                this.loggingManager = null;
                break;
            case 2 :
                this.loggingManager = new WALManager(configuration);
        }
    }

    /**
     * @param table
     * @param record
     * @throws DatabaseException
     */
    @Override
    public void InsertRecord(String table, TableRecord record, int partition_id) throws DatabaseException {
        storageManager.InsertRecord(table, record, partition_id);
    }

    @Override
    public void asyncSnapshot(long snapshotId, int partitionId, FTManager ftManager) throws IOException {
        this.storageManager.asyncSnapshot(snapshotId, partitionId, ftManager);
    }

    @Override
    public void asyncCommit(long groupId, int partitionId, FTManager ftManager) throws IOException {
        this.loggingManager.commitLog(groupId, partitionId, ftManager);
    }

    @Override
    public void syncReloadDB(SnapshotResult snapshotResult) throws IOException, ExecutionException, InterruptedException {
        this.storageManager.syncReloadDatabase(snapshotResult);
    }
}
