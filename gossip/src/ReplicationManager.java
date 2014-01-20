import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.UUID;
import java.util.Vector;

import p2pmpi.mpi.MPI;
import p2pmpi.mpi.IntraComm;
import p2pmpi.mpi.Request;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;


public class ReplicationManager {
	
	private Logger 						logger 				= null;	
	private IntraComm 					comm 				= null;
	private int							size 				= 0;
	private int 						rank 				= 0;
	private int[] 						replicaTimestamp 	= null;
	private int[] 						messageTimestamp 	= null;
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
	private ArrayList<ActionMessage>	messages 			= null;
	private LinkedList<QueryMessage>	pendingQueries		= null;
	
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
	
	public void setLogLevel(Level logLevel) {
		logger.setLevel(logLevel);
		logger.info(
			String.format(
				"RM %d: Set log-level to %s", rank, logLevel.toString()
			)
		);
	}
	
	public void listenToGossip() {
		for (int neighbour = 0; neighbour < neighbourIds.length; neighbour++) {
			if (gossipRequests[neighbour] == null) {
				gossipRequests[neighbour] = comm.Irecv(gossipBuffer, 0, 1, MPI.OBJECT, neighbourIds[neighbour], Utils.GOSSIP_TAG);
				logger.trace(
					String.format(
						"RM %d: Listening to RM %d", rank, neighbourIds[neighbour]
					)
				);
			}
			else {
				if (gossipRequests[neighbour].Test() != null) {
					logger.info(
						String.format(
							"RM %d: Got gossip from RM %d", rank, neighbourIds[neighbour]
						)
					);
					
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
							"RM %d: Merged time stamp from RM %d: %s", rank, neighbourIds[neighbour], Arrays.toString(this.replicaTimestamp)
						)
					);
					
					if (updatesHaveBeenMerged) {
						// Process 'stable' updates
						this.applyStableUpdates();
						
						// Clean log
						this.cleanUpdateLog();
					}
					
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

	public void sendGossip() {		
		replicaTimestamp[this.rank] += 1;
		GossipMessage[] msg = new GossipMessage[1];
		msg[0] = new GossipMessage();
		msg[0].setTimestamp(this.replicaTimestamp);
		msg[0].setLogRecords(this.updateLog);
		
		for (int neighbour = 0; neighbour < neighbourIds.length; neighbour++) {
			comm.Isend(msg, 0, msg.length, MPI.OBJECT, neighbourIds[neighbour], Utils.GOSSIP_TAG);
			logger.info(
				String.format(
					"RM %d: Sending gossip to RM %d", rank, neighbourIds[neighbour]
				)
			);
		}
		
	}
	
	public void listenToUpdates() {
		for (int frontendId = 0; frontendId < frontendIds.length; frontendId++) {
			if (updateRequests[frontendId] == null) {
				updateRequests[frontendId] = comm.Irecv(updateBuffer, 0, 1, MPI.OBJECT, frontendIds[frontendId], Utils.UPDATE_TAG);
				logger.trace(
					String.format(
						"RM %d: Listening to FE %d", rank, frontendIds[frontendId]
					)
				);
			}
			else {
				if (updateRequests[frontendId].Test() != null) {
					logger.info(
						String.format(
							"RM %d: Got update from FE %d. Message-ID: %s.", rank, frontendIds[frontendId], updateBuffer[0].getId()
						)
					);
					
					int[] ts = tryPerformUpdate(updateBuffer[0]);
					if (ts != null) {
						ActionMessage[] msg = new ActionMessage[1];
						msg[0] = new ActionMessage();
						msg[0].setTimestamp(ts);
						comm.Isend(msg, 0, msg.length, MPI.OBJECT, frontendIds[frontendId], Utils.UPDATE_TAG);
						
						logger.info(
							String.format(
								"RM %d: Got update, informed FE %d: %s", rank, frontendIds[frontendId], Arrays.toString(msg[0].getTimestamp())
							)
						);
						
						if (Utils.isSmallerOrEqualThan(updateBuffer[0].getTimestamp(), this.messageTimestamp)) {
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
					else {
						logger.debug(
							String.format(
								"RM %d: Update from FE %d already performed. Doing nothing.", rank, frontendIds[frontendId]
							)
						);
					}
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
	
	public void listenToQueries() {
		for (int frontendId = 0; frontendId < frontendIds.length; frontendId++) {
			if (queryRequests[frontendId] == null) {
				queryRequests[frontendId] = comm.Irecv(queryBuffer, 0, 1, MPI.OBJECT, frontendIds[frontendId], Utils.QUERY_TAG);
				logger.trace(
					String.format(
						"RM %d: Listening to FE %d", rank, frontendIds[frontendId]
					)
				);
			}
			else {
				if (queryRequests[frontendId].Test() != null) {
					logger.info(
						String.format(
							"RM %d: Got query from FE %d. Message-ID: %s.", rank, frontendIds[frontendId], queryBuffer[0].getId()
						)
					);
					
					processQuery(queryBuffer[0]);
					
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
	
	public void processQueuedQueries() {
		for (QueryMessage msg : this.pendingQueries) {
			processQuery(msg);
		}
	}
	
	public void printExecutedMessages() {

		for (ActionMessage msg : this.executedCalls) {
			logger.debug(
				String.format(
					"RM %d - executed messages:	TS: %s; UUID: %s", rank, Arrays.toString(msg.getTimestamp()), msg.getId()
				)
			);	
		}
	}

	private void processQuery(QueryMessage msg) {
		if (Utils.isSmallerOrEqualThan(msg.getTimestamp(), this.messageTimestamp)) {
			// TODO: Execute query
			
			// Inform FE
			QueryMessage[] answer = new QueryMessage[1];
			answer[0] = new QueryMessage();
			answer[0].setTimestamp(this.messageTimestamp);
			comm.Isend(answer, 0, answer.length, MPI.OBJECT, msg.getFrontendId(), Utils.QUERY_TAG);
			
			logger.info(
				String.format(
					"RM %d: Made query, informed FE %d: %s", rank, msg.getFrontendId(), Arrays.toString(answer[0].getTimestamp())
				)
			);
			
		}
		else {
			// Query not ready yet, queue it
			// if not already queued
			if (this.pendingQueries.indexOf(msg) == -1 ) {
				this.pendingQueries.add(msg);
				
				logger.info(
					String.format(
						"RM %d: Not ready for query %s yet. Queued.", rank, msg.getFrontendId(), msg.getId()
					)
				);
			}
			else {
				logger.info(
					String.format(
						"RM %d: Still not ready for query %s yet. Waiting.", rank, msg.getFrontendId(), msg.getId()
					)
				);
			}
		}
	}
	
	private void setupLogger() {
		SimpleLayout layout = new SimpleLayout();
	    ConsoleAppender consoleAppender = new ConsoleAppender( layout );
		
	    logger = LogManager.getLogger(ReplicationManager.class.getName());
		logger.addAppender(consoleAppender);
	    logger.setLevel(Level.INFO);
	}

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
		messages = new ArrayList<ActionMessage>();
		pendingQueries = new LinkedList<QueryMessage>();
	}
	
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
	
	private int[] tryPerformUpdate(ActionMessage msg) {		
		if (!hasBeenExecuted(msg.getId())) {
			replicaTimestamp[this.rank] += 1;
			
			int[] ts = Arrays.copyOf(msg.getTimestamp(), msg.getTimestamp().length);
			ts[this.rank] = replicaTimestamp[this.rank];
			logger.info(
				String.format(
					"RM %d: Set new TS: %s.", rank, Arrays.toString(ts)
				)
			);
			
			LogRecord logRecord = new LogRecord();
			logRecord.setReplicationManagerId(this.rank);
			logRecord.setTimestamp(ts);
			logRecord.setMessage(msg);
			this.updateLog.add(logRecord);
			
			return ts;
		}
		
		return null;
	}
	
	private void applyMessage(ActionMessage msg) {
		switch (msg.getOperation()) {
		case INSERT:
			this.messages.add(msg);
			logger.info(
				String.format(
					"RM %d: Inserted Message (ID %s) to Value-Log.", rank, msg.getId()
				)
			);
			break;
		
		case UPDATE:
			//TODO
			logger.info(
				String.format(
					"RM %d: Trying to update Message-ID: %s.", rank, msg.getId()
				)
			);
			break;
			
		case DELETE:
			//TODO
			logger.info(
				String.format(
					"RM %d: Trying to delete Message-ID: %s.", rank, msg.getId()
				)
			);
			break;
		}
		
		this.executedCalls.add(msg);
		int[] newMessageTimestamp = Utils.getGreaterTimestamp(this.messageTimestamp, msg.getTimestamp());
		this.messageTimestamp = Arrays.copyOf(newMessageTimestamp, newMessageTimestamp.length);
	}
	
	/*
	private void removeFromUpdateLog(Message msg) {
		boolean msgFound = false;
		int messagePosition = 0;
		
		while (messagePosition < this.updateLog.size()) {
			if (this.updateLog.get(messagePosition).getMessage().getId() == msg.getId()) {
				msgFound = true;
				break;
			}
			messagePosition += 1;
		}

		if (msgFound) {
			this.updateLog.remove(messagePosition);
			logger.debug(
				String.format(
					"RM %d: Removed Message (ID  %s) from Update-Log.", rank, msg.getId()
				)
			);
		}
	}
	*/

	private void applyStableUpdates() {
		// Get stable updates
		ArrayList<LogRecord> stableUpdates = new ArrayList<LogRecord>(this.updateLog);
		// Sort them
		Collections.sort(stableUpdates);
		
		for (LogRecord stableUpdate : stableUpdates) {
			this.applyMessage((ActionMessage) stableUpdate.getMessage());
		}
	}
	
	private void cleanUpdateLog() {
		//TODO: Implement
	}
}
