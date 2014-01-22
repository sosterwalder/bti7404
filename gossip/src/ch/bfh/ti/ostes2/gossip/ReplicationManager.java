/*
 * ReplicationManager.java
 * 
 * 3.5
 * 
 * 2014-01-22
 *
 * The MIT License (MIT)
* 
* Copyright (c) 2014 Sven Osterwalder
* 
* Permission is hereby granted, free of charge, to any person obtaining a copy
* of this software and associated documentation files (the "Software"), to deal
* in the Software without restriction, including without limitation the rights
* to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
* copies of the Software, and to permit persons to whom the Software is
* furnished to do so, subject to the following conditions:
* 
* The above copyright notice and this permission notice shall be included in all
* copies or substantial portions of the Software.
* 
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
* IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
* FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
* AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
* LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
* OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
* SOFTWARE.
*/

package ch.bfh.ti.ostes2.gossip;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.UUID;

import p2pmpi.mpi.MPI;
import p2pmpi.mpi.IntraComm;
import p2pmpi.mpi.Request;
import p2pmpi.p2p.message.UpdateMessage;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;

/**
 * This class represents the {@link ReplicationManager} in 
 * the gossip architecture. It is intended to serve
 * clients (computers, tablets and so on) via a {@link Frontend}
 * concerning their {@link QueryMessage}s or {@link UpdateMessage}s. 
 * 
 * For each communication category (updating and querying)
 * the tags from {@link Utils} are used to distinct them.
 * 
 * @author sosterwalder
 *
 */
public class ReplicationManager {
	
	private Logger 						logger 				= null;	
	private IntraComm 					comm 				= null;
	private int							size 				= 0;
	private int 						rank 				= 0;
	private int[] 						replicaTimestamp 	= null; // Also known as replicaTS
	private int[] 						messageTimestamp 	= null; // Also known as valueTS 
	private int[] 						neighbourIds 		= null;
	private int[] 						frontendIds			= null;
	private GossipMessage[] 			gossipBuffer 		= null;
	private Request[]					gossipRequests 		= null;
	private ActionMessage[]				updateBuffer 		= null;
	private Request[]					updateRequests 		= null;
	private QueryMessage[]				queryBuffer 		= null;
	private Request[]					queryRequests		= null;
	private ArrayList<ActionMessage>	executedCalls 		= null;
	private ArrayList<LogRecord>		updateLog 			= null;
	private Map<UUID, ActionMessage>	messages 			= null;
	private LinkedList<QueryMessage>	pendingQueries		= null;
	
	/**
	 * Constructor
	 */
	public ReplicationManager() {
		this.setupLogger();
		
		comm = MPI.COMM_WORLD;
		size = comm.Size();
		rank = comm.Rank();
		frontendIds = new int[1];
		this.initializeNeighbours();
		this.initializeFrontends();
		this.initializeBuffers();
		
		logger.info(
			String.format(
				"RM %d: Hej, this is RM %d", rank, rank
			)
		);
	}
	
	/**
	 * Sets the internal log level
	 * to the given log level.
	 * 
	 * @param logLevel		the log level to set
	 */
	public void setLogLevel(Level logLevel) {
		logger.setLevel(logLevel);
		logger.info(
			String.format(
				"RM %d: Set log-level to %s", rank, logLevel.toString()
			)
		);
	}
	
	/**
	 * Listens to all available {@link ReplicationManager} neighbors
	 * for gossip messages using the GOSSIP-tag from {@link Utils}.
	 */
	public void listenToGossip() {
		for (int neighbour = 0; neighbour < neighbourIds.length; neighbour++) {
			// If there's no message yet for
			// current neighbor
			if (gossipRequests[neighbour] == null) {
				// Set up new buffer for reception
				gossipRequests[neighbour] = comm.Irecv(gossipBuffer, 0, 1, MPI.OBJECT, neighbourIds[neighbour], Utils.GOSSIP_TAG);
				logger.trace(
					String.format(
						"RM %d: Listening to RM %d", rank, neighbourIds[neighbour]
					)
				);
			}
			else {
				// There seems to be a gossip message from current
				// neighbor
				if (gossipRequests[neighbour].Test() != null) {
					logger.info(
						String.format(
							"RM %d: Got gossip from RM %d", rank, neighbourIds[neighbour]
						)
					);
					
					// Process the gossip message
					processGossip(neighbour);
					
					// Reinitialize communication
					gossipRequests[neighbour] = comm.Irecv(gossipBuffer, 0, 1, MPI.OBJECT, neighbourIds[neighbour], Utils.GOSSIP_TAG);
				}
				else {
					logger.trace(
						String.format(
							"RM %d: Still listening to RM %d", rank, neighbourIds[neighbour]
						)
					);
				}
				
			}
		}
	}

	/**
	 * Processes the received gossip message from
	 * given neighbor.
	 * 
	 * Gets all the messages from within the gossip-
	 * message and adds them to the internal update
	 * log when they are not already scheduled or
	 * even executed.
	 * 
	 * Applies the stable updates and (would) clean
	 * the update log (this is not implemented yet).
	 * 
	 * @param neighbour		the neighbor which the
	 * 						gossip message is
	 * 						received from
	 */
	private void processGossip(int neighbour) {
		boolean updatesHaveBeenMerged = false;
		for (int i = 0; i < gossipBuffer[0].getLogRecords().size(); i++) {
			LogRecord msg = gossipBuffer[0].getLogRecords().get(i);
			//Check if already in log or executed
			if (!isScheduled(msg.getMessage().getId()) && !hasBeenExecuted(msg.getMessage().getId())) {
				this.updateLog.add(msg);
				updatesHaveBeenMerged = true;
				
				logger.debug(
					String.format(
						"RM %d: Added Message %s to update-log", rank, msg.getMessage().getId()
					)
				);
			}
		}
		
		// Merge time stamps
		this.replicaTimestamp = Utils.mergeTimestamps(this.replicaTimestamp, gossipBuffer[0].getTimestamp());
		logger.debug(
			String.format(
				"RM %d: Merged time stamp from RM %d. ReplicaTS: %s", rank, neighbourIds[neighbour], Arrays.toString(this.replicaTimestamp)
			)
		);
		
		if (updatesHaveBeenMerged) {
			// Process 'stable' updates
			this.applyStableUpdates();
			
			// Clean log
			this.cleanUpdateLog();
		}
	}

	/**
	 * Sends gossip messages to other, adjacent
	 * {@link ReplicationManager}s.
	 * 
	 * This principally tells them which messages 
	 * the current {@link ReplicationManager} knows
	 * and has in its buffer so that they can be shared. 
	 */
	public void sendGossip() {		
		// Increase own replicaTS
		replicaTimestamp[this.rank] += 1;
		
		// Set up message
		GossipMessage[] msg = new GossipMessage[1];
		msg[0] = new GossipMessage();
		msg[0].setTimestamp(this.replicaTimestamp);
		msg[0].setLogRecords(this.updateLog);
		
		// Spread to all neighbors
		for (int neighbour = 0; neighbour < neighbourIds.length; neighbour++) {
			comm.Isend(msg, 0, msg.length, MPI.OBJECT, neighbourIds[neighbour], Utils.GOSSIP_TAG);
			logger.info(
				String.format(
					"RM %d: Sending gossip to RM %d. ReplicaTS: %s", rank, neighbourIds[neighbour], Arrays.toString(replicaTimestamp)
				)
			);
		}
		
	}
	
	/**
	 * Listens to all available {@link Frontend}s for
	 * possible update messages using
	 * the UPATE-tag from {@link Utils}.
	 */
	public void listenToUpdates() {
		for (int frontendId = 0; frontendId < frontendIds.length; frontendId++) {
			// If there's update from 
			// current front end yet
			if (updateRequests[frontendId] == null) {
				// Set up new buffer for reception
				updateRequests[frontendId] = comm.Irecv(updateBuffer, 0, 1, MPI.OBJECT, frontendIds[frontendId], Utils.UPDATE_TAG);
				logger.trace(
					String.format(
						"RM %d: Listening to FE %d", rank, frontendIds[frontendId]
					)
				);
			}
			else {
				// There seems to be an update from the current front end
				if (updateRequests[frontendId].Test() != null) {
					logger.info(
						String.format(
							"RM %d: Got update from FE %d. Message-ID: %s.", rank, frontendIds[frontendId], updateBuffer[0].getId()
						)
					);
					
					// Try to perform the update
					int[] ts = tryPerformUpdate(updateBuffer[0]);
					if (ts != null) {
						// If it is possible to perform the
						// update perform it actually
						performUpdate(frontendId, ts);
					}
					else {
						logger.debug(
							String.format(
								"RM %d: Update from FE %d already performed. Doing nothing.", rank, frontendIds[frontendId]
							)
						);
					}
					
					// Re-initialize listening to updates
					updateRequests[frontendId] = comm.Irecv(updateBuffer, 0, 1, MPI.OBJECT, frontendIds[frontendId], Utils.UPDATE_TAG);
				}
				else {
					logger.trace(
						String.format(
							"RM %d: Still listening to FE %d", rank, frontendIds[frontendId]
						)
					);
				}
				
			}
		}
	}

	/**
	 * Performs received updates from {@link Frontend}s
	 * as soon as possible.
	 * 
	 * @param frontendId		the id of the sender {@link Frontend}
	 * @param timeStamp			the merged time stamp
	 */
	private void performUpdate(int frontendId, int[] timeStamp) {
		// Set up message and inform front end
		// about the performing of the update
		ActionMessage[] msg = new ActionMessage[1];
		msg[0] = new ActionMessage();
		msg[0].setTimestamp(timeStamp);
		comm.Isend(msg, 0, msg.length, MPI.OBJECT, frontendIds[frontendId], Utils.UPDATE_TAG);
		
		logger.info(
			String.format(
				"RM %d: Got update, informed FE %d: %s", rank, frontendIds[frontendId], Arrays.toString(msg[0].getTimestamp())
			)
		);
		
		// Check if the update is executable right now
		if (Utils.isSmallerOrEqualThan(updateBuffer[0].getTimestamp(), this.messageTimestamp)) {
			// Only perform the update if it
			// hasn't already been performed
			if (!hasBeenExecuted(updateBuffer[0].getId())) {
				this.applyMessage(updateBuffer[0]);
			}
			else {
				logger.trace(
					String.format(
						"RM %d: Update already executed. Message-ID: %s.", rank, updateBuffer[0].getId()
					)
				);
			}
		}
		else {
			logger.debug(
				String.format(
					"RM %d: Can't execute update: u.prev > valueTS. Message-ID: %s.", rank, updateBuffer[0].getId()
				)
			);
		}
	}
	
	/**
	 * Listens to all available {@link Frontend}s for
	 * possible query messages using
	 * the QUERY-tag from {@link Utils}.
	 */
	public void listenToQueries() {
		for (int frontendId = 0; frontendId < frontendIds.length; frontendId++) {
			// If there's no query for
			// current front end yet
			if (queryRequests[frontendId] == null) {
				// Set up new buffer for reception
				queryRequests[frontendId] = comm.Irecv(queryBuffer, 0, 1, MPI.OBJECT, frontendIds[frontendId], Utils.QUERY_TAG);
				logger.trace(
					String.format(
						"RM %d: Listening to FE %d", rank, frontendIds[frontendId]
					)
				);
			}
			else {
				// There seems to be a query from the current
				// front end
				if (queryRequests[frontendId].Test() != null) {
					logger.info(
						String.format(
							"RM %d: Got query from FE %d. Message-ID: %s.", rank, frontendIds[frontendId], queryBuffer[0].getId()
						)
					);
					
					// Process the query
					processQuery(queryBuffer[0]);

					// Re-initialize listening to query messages
					queryRequests[frontendId] = comm.Irecv(queryBuffer, 0, 1, MPI.OBJECT, frontendIds[frontendId], Utils.QUERY_TAG);
				}
				else {
					logger.trace(
						String.format(
							"RM %d: Still listening to FE %d", rank, frontendIds[frontendId]
						)
					);
				}
				
			}
		}
	}
	
	/**
	 * Processes queued queries
	 * (if there are any) as soon
	 * as they get executable.
	 */
	public void processQueuedQueries() {
		for (QueryMessage msg : this.pendingQueries) {
			processQuery(msg);
		}
	}
	
	/**
	 * Convenience method for printing all
	 * executed messages within the configured
	 * logger.
	 */
	public void printExecutedMessages() {

		for (ActionMessage msg : this.executedCalls) {
			logger.debug(
				String.format(
					"RM %d - executed messages:	TS: %s; UUID: %s", rank, Arrays.toString(msg.getTimestamp()), msg.getId()
				)
			);		
		}
	}

	/**
	 * Processes the given query message.
	 * 
	 * The query message gets then executed when
	 * the time stamp of the query-message is smaller
	 * or equal than the current own time stamp. 
	 * 
	 * @param msg		the query-message to execute
	 */
	private void processQuery(QueryMessage msg) {
		// Execute query only if the current own time stamp
		// is smaller or equal than the messages time stamp
		if (Utils.isSmallerOrEqualThan(msg.getTimestamp(), this.messageTimestamp)) {
			// TODO: Execute query
			
			// Inform FE about execution
			QueryMessage[] answer = new QueryMessage[1];
			answer[0] = new QueryMessage();
			answer[0].setTimestamp(this.messageTimestamp);
			comm.Isend(answer, 0, answer.length, MPI.OBJECT, msg.getFrontendId(), Utils.QUERY_TAG);
			
			logger.info(
				String.format(
					"RM %d: Made query, informed FE %d: %s", rank, msg.getFrontendId(), Arrays.toString(answer[0].getTimestamp())
				)
			);
			
			// Remove query from pending queries (if
			// it was pending)
			int msgIndex = this.pendingQueries.indexOf(msg); 
			if (msgIndex > -1 ) {
				this.pendingQueries.remove(msgIndex);
				logger.debug(
					String.format(
						"RM %d: Removed query %s from queue.", rank, msg.getId()
					)
				);
			}
		}
		else {
			// Query not ready yet, queue it
			// if not already queued
			if (this.pendingQueries.indexOf(msg) == -1 ) {
				this.pendingQueries.add(msg);
				
				logger.info(
					String.format(
						"RM %d: Not ready for query %s yet. Queued.", rank, msg.getId()
					)
				);
			}
			else {
				logger.trace(
					String.format(
						"RM %d: Still not ready for query %s yet. Waiting.", rank, msg.getId()
					)
				);
			}
		}
	}
	
	/**
	 * Sets up the logger which
	 * currently appends to the console
	 * (stdout) only.
	 */
	private void setupLogger() {
		SimpleLayout layout = new SimpleLayout();
	    ConsoleAppender consoleAppender = new ConsoleAppender( layout );
		
	    logger = LogManager.getLogger(ReplicationManager.class.getName());
		logger.addAppender(consoleAppender);
	    logger.setLevel(Level.INFO);
	}

	/**
	 * Stores the IDs of other {@link ReplicationManager}s
	 * within the friendly neighborhood (except its own
	 * of course). Assumes that there are as many {@link ReplicationManager}s
	 * as half of the  P2P-MPI processes available.
	 * 
	 * E.g. if there are 6 processes and the own rank is 1,
	 * then there have to be 3 {@link ReplicationManager}s. 
	 * This means, neighbor-IDs are 0 and 2. 
	 */
	private void initializeNeighbours() {
		neighbourIds = new int[(size / 2) - 1];
		for (int i = 0; i < neighbourIds.length; i++) {
			if (i >= rank) {
				neighbourIds[i] = rank + 1;
			}
			else {
				neighbourIds[i] = i;
			}
		}
		
		logger.info(
			String.format(
				"RM %d: # of neighbours: %d", rank, neighbourIds.length
			)
		);
	}
	
	/**
	 * Stores the IDs of {@link Frontend}s around.
	 * Assumes that there are as many {@link Frontend}s
	 * as half of the  P2P-MPI processes available.
	 * 
	 * E.g. if there are 6 processes, then there
	 * have to be 3 {@link Frontend}s. 
	 * This means, frontend-IDs are 3, 4 and 5. 
	 */
	private void initializeFrontends() {
		// Always take the FE with the least distant
		// ID assuming we have always n RM and n FE.
		// E.g.: Size is 6, RM-ID is 2, then
		// FE-ID = RM-ID + (Size / 2) = 2 + (6 / 2) = 5 
		//frontendIds[0] = rank + (size / 2);
		
		frontendIds = new int[size / 2];
		for (int i = 0; i < frontendIds.length; i++) {
			frontendIds[i] = (size / 2) + i;
		}
		
		logger.info(
			String.format(
				"RM %d: # of frontends: %d", rank, frontendIds.length
			)
		);
	}
	
	/**
	 * Initializes all the needed buffers.
	 */
	private void initializeBuffers() {
		replicaTimestamp = new int[size /2];
		messageTimestamp = new int[size / 2];
		gossipRequests = new Request[neighbourIds.length];
		gossipBuffer = new GossipMessage[neighbourIds.length];
		updateRequests = new Request[frontendIds.length];
		updateBuffer = new ActionMessage[frontendIds.length];
		queryRequests = new Request[frontendIds.length];
		queryBuffer = new QueryMessage[frontendIds.length];
		executedCalls = new ArrayList<ActionMessage>();
		updateLog = new ArrayList<LogRecord>();
		messages = new HashMap<UUID, ActionMessage>();
		pendingQueries = new LinkedList<QueryMessage>();
	}
	
	
	/**
	 * Helper method to check if the
	 * message with the given ID is yet
	 * scheduled within to update-log.
	 * 
	 * @param messageId			the id of the message to check
	 * @return					true if the message is scheduled,
	 * 							false if not
	 */
	private boolean isScheduled(UUID messageId) {
		boolean isScheduled = false;
		
		for (LogRecord msg : this.updateLog) {
			if (msg.getMessage().getId().compareTo(messageId) == 0) {
				isScheduled = true;
				break;
			}
		}
		
		return isScheduled;
	}
	
	/**
	 * Helper method to check if the
	 * message with the given ID was yet
	 * executed.
	 * 
	 * @param messageId			the id of the message to check
	 * @return					true if the message was executed,
	 * 							false if not
	 */
	private boolean hasBeenExecuted(UUID messageId) {
		boolean hasBeenExecuted = false;
		
		for (ActionMessage msg : this.executedCalls) {
			if (msg.getId().compareTo(messageId) == 0) {
				hasBeenExecuted = true;
				break;
			}
		}
		
		return hasBeenExecuted;
	}
	
	/**
	 * Tries to perform an update respectively
	 * inserting it into the update-log, if it
	 * was not executed already.
	 * 
	 * @param msg		the update-message
	 * @return			if the update has not been executed yet:
	 * 						the time stamp of the message
	 * 						but with the value of the time
	 * 						stamp of the own replica time stamp
	 * 						at the index of its own rank if
	 *					if the update was already executed:
	 *						null
	 * 						
	 */
	private int[] tryPerformUpdate(ActionMessage msg) {		
		if (!hasBeenExecuted(msg.getId())) {
			replicaTimestamp[this.rank] += 1;
			
			// Build new time stamp based on the message time stamp
			int[] ts = Arrays.copyOf(msg.getTimestamp(), msg.getTimestamp().length);
			ts[this.rank] = replicaTimestamp[this.rank];
			logger.info(
				String.format(
					"RM %d: Set new unique TS %s for update.", rank, Arrays.toString(ts)
				)
			);
			
			// Add the update to the message log
			LogRecord logRecord = new LogRecord();
			logRecord.setReplicationManagerId(this.rank);
			logRecord.setTimestamp(ts);
			logRecord.setMessage(msg);
			this.updateLog.add(logRecord);
			
			return ts;
		}
		
		// Return n
		return null;
	}
	
	/**
	 * Apply the given message according
	 * to its operation.
	 * 
	 * @param msg		the message to apply
	 */
	private void applyMessage(ActionMessage msg) {
		switch (msg.getOperation()) {
		case INSERT:
			this.messages.put(msg.getId(), msg);
			logger.info(
				String.format(
					"RM %d: Inserted Message (ID %s) to Value-Log.", rank, msg.getId()
				)
			);
			break;
		
		case UPDATE:
			//TODO: Error handling
			ActionMessage updatedMessage = this.messages.get(msg.getId());
			updatedMessage.udpate(msg);
			this.messages.put(msg.getId(), updatedMessage);
			
			logger.info(
				String.format(
					"RM %d: Trying to update Message-ID: %s.", rank, msg.getId()
				)
			);
			break;
			
		case DELETE:
			//TODO: Error handling
			this.messages.remove(msg.getId());
			
			logger.info(
				String.format(
					"RM %d: Trying to delete Message-ID: %s.", rank, msg.getId()
				)
			);
			break;
		}
		
		// Add the message to the call log
		this.executedCalls.add(msg);
		
		int[] oldMessageTimestamp = Arrays.copyOf(this.messageTimestamp, this.messageTimestamp.length);
		int[] newMessageTimestamp = Utils.getMaximizedTimestamp(this.messageTimestamp, msg.getTimestamp());
		
		// Set the new message time stamp (valueTS)
		this.messageTimestamp = Arrays.copyOf(newMessageTimestamp, newMessageTimestamp.length);
		
		logger.info(
			String.format(
				"RM %d: Set new valueTS: %s. Based on old valueTS %s and u.TS %s", rank, Arrays.toString(this.messageTimestamp), Arrays.toString(oldMessageTimestamp), Arrays.toString(msg.getTimestamp())
			)
		);
	}

	/**
	 * Applies stable updates from within
	 * the update-log.
	 */
	private void applyStableUpdates() {
		// Get stable updates
		ArrayList<LogRecord> stableUpdates = new ArrayList<LogRecord>(this.updateLog);
		// Sort them
		Collections.sort(stableUpdates);
		
		// Apply them
		for (LogRecord stableUpdate : stableUpdates) {
			this.applyMessage((ActionMessage) stableUpdate.getMessage());
		}
	}
	
	/**
	 * Removes executed updates
	 * from the update-log as soon as
	 * no other {@link ReplicationManager} needs
	 * them anymore.
	 */
	private void cleanUpdateLog() {
		/*
		 * TODO: Implement
		 * 
		 * Requirement: MessageTimestamps from all neighbors.
		 * 
		 * List<int[]> collectedMessageTimestamps;
		 * int[] minimalTimestamp = Utils.getMinimalTimestamp(collectedMessageTimestamps);
		 * 
		 * for (LogRecord msg : updateLog) {
		 *     if (Utils.isSmallerOrEqualThan(msg.getTimestamp(), minimalTimestamp) {
		 *         updateLog.remove(msg.getTimestamp());
		 *     }
		 * } 
		 * 
		 */
	}
}
