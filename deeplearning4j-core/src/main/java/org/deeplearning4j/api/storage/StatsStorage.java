package org.deeplearning4j.api.storage;

import org.deeplearning4j.api.storage.Persistable;

import java.io.IOException;
import java.util.List;

/**
 * A general-purpose stats storage mechanism, for storing stats information (mainly used for iteration listeners).
 * <p>
 * Key design ideas:
 * (a) Two types of storable objects:
 * i.  {@link Persistable} objects, for once per session objects ("static info") and also for periodically reported data ("updates")
 * ii. {@link StorageMetaData} objects, for
 * (b) There are 4 types of things used to uniquely identify these Persistable objects:<br>
 * i.   SessionID: A unique identifier for a single session<br>
 * ii.  TypeID: A unique identifier for the listener or type of data<br>
 * For example, we might have stats from 2 (or more) listeners with identical session and worker IDs<br>
 * This is typically hard-coded, per listener class<br>
 * iii. WorkerID: A unique identifier for workers, within a session<br>
 * iv.  Timestamp: time at which the record was created<br>
 * For example, single machine training (with 1 listener) would have 1 session ID, 1 type ID, 1 worker ID, and multiple timestamps.<br>
 * Distributed training multiple listeres could have 1 session ID, multiple type IDs, and multiple worker IDs, and multiple timestamps for each<br>
 * A hyperparameter optimization job could have multiple session IDs on top of that.<br>
 * <p>
 * Note that the StatsStorage interface extends {@link StatsStorageRouter}
 *
 * @author Alex Black
 */
public interface StatsStorage extends StatsStorageRouter {


    /**
     * Close any open resources (files, etc)
     */
    void close() throws IOException;

    /**
     * @return Whether the StatsStorage implementation has been closed or not
     */
    boolean isClosed();

    /**
     * Get a list of all sessions stored by this storage backend
     */
    List<String> listSessionIDs();

    /**
     * Check if the specified session ID exists or not
     *
     * @param sessionID Session ID to check
     * @return true if session exists, false otherwise
     */
    boolean sessionExists(String sessionID);

    /**
     * Get the static info for the given session and worker IDs, or null if no such static info has been reported
     *
     * @param sessionID Session ID
     * @param workerID  worker ID
     * @return Static info, or null if none has been reported
     */
    Persistable getStaticInfo(String sessionID, String typeID, String workerID);


    List<Persistable> getAllStaticInfos(String sessionID, String typeID);

    /**
     * Get the list of type IDs for the given session ID
     *
     * @param sessionID Session ID to query
     * @return List of type IDs
     */
    List<String> listTypeIDsForSession(String sessionID);

    /**
     * For a given session ID, list all of the known worker IDs
     *
     * @param sessionID Session ID
     * @return List of worker IDs, or possibly null if session ID is unknown
     */
    List<String> listWorkerIDsForSession(String sessionID);

    /**
     * Return the number of update records for the given session ID (all workers)
     *
     * @param sessionID Session ID
     * @return number of update records
     */
    int getNumUpdateRecordsFor(String sessionID);

    /**
     * Return the number of update records for the given session ID and worker ID
     *
     * @param sessionID Session ID
     * @param workerID  Worker ID
     * @return number of update records
     */
    int getNumUpdateRecordsFor(String sessionID, String typeID, String workerID);

    /**
     * Get the latest update record (i.e., update record with the largest timestamp value) for the specified
     * session and worker IDs
     *
     * @param sessionID session ID
     * @param workerID  worker ID
     * @return UpdateRecord containing the session/worker IDs, timestamp and content for the most recent update
     */
    Persistable getLatestUpdate(String sessionID, String typeID, String workerID);

    /**
     * Get the specified update (or null, if none exists for the given session/worker ids and timestamp)
     *
     * @param sessionID Session ID
     * @param workerID  Worker ID
     * @param timestamp Timestamp
     * @return Update
     */
    Persistable getUpdate(String sessionID, String typeId, String workerID, long timestamp);

    /**
     * Get the latest update for all workers, for the given session ID
     *
     * @param sessionID Session ID
     * @return List of updates for the given Session ID
     */
    List<Persistable> getLatestUpdateAllWorkers(String sessionID, String typeID);

    /**
     * Get all updates for the given session and worker ID, that occur after (not including) the given timestamp.
     * Results should be sorted by time.
     *
     * @param sessionID Session ID
     * @param workerID  Worker Id
     * @param timestamp Timestamp
     * @return List of records occurring after the given timestamp
     */
    List<Persistable> getAllUpdatesAfter(String sessionID, String typeID, String workerID, long timestamp);


    /**
     * Get the session metadata, if any has been registered via {@link #putStorageMetaData(StorageMetaData)}
     *
     * @param sessionID Session ID to get metadat
     * @return Session metadata, or null if none is available
     */
    StorageMetaData getStorageMetaData(String sessionID, String typeID);

    // ----- Listeners -----

    /**
     * Add a new StatsStorageListener. The given listener will called whenever a state change occurs for the stats
     * storage instance
     *
     * @param listener Listener to add
     */
    void registerStatsStorageListener(StatsStorageListener listener);

    /**
     * Remove the specified listener, if it is present.
     *
     * @param listener Listener to remove
     */
    void deregisterStatsStorageListener(StatsStorageListener listener);

    /**
     * Remove all listeners from the StatsStorage instance
     */
    void removeAllListeners();

    /**
     * Get a list (shallow copy) of all listeners currently present
     *
     * @return List of listeners
     */
    List<StatsStorageListener> getListeners();

}
