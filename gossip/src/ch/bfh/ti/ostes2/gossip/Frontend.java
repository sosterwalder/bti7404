/*
 * Frontend.java
 * 
 * 1.8
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
import java.util.Random;

import p2pmpi.mpi.MPI;
import p2pmpi.mpi.IntraComm;
import p2pmpi.mpi.Request;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;

/**
 * This class represents the {@link Frontend} in 
 * the gossip architecture. It is intended to forward
 * updates and queries from clients (computers, tablets
 * and so on) to a available {@link ReplicationManager}.
 * 
 * As this is only a simulation, the {@link Frontend}
 * picks randomly a available {@link ReplicationManager},
 * posts randomly messages and sends randomly queries.
 * 
 * The updating and deleting of messages is not yet
 * implemented.  
 * 
 * For each communication category (updating and querying)
 * the tags from {@link Utils} are used to distinct them.
 * 
 * @author sosterwalder
 *
 */
public class Frontend {
	private Logger 					logger = null;	
	private IntraComm 				comm = null;
	private int						size = 0;
	private int 					rank = 0;
	private int[]					timestamp = null;
	private int[]					replicaManagerIds = null;
	private ArrayList<LogRecord>	sentMessages = null; 
	private ActionMessage[]			answerBuffer = null;
	private Request[]				updateAnswers = null;
	private QueryMessage[]			queryBuffer = null;
	private Request[]				queryAnswers = null;
	
	/**
	 * Constructor
	 */
	public Frontend() {
		this.setupLogger();
		
		comm = MPI.COMM_WORLD;
		size = comm.Size();
		rank = comm.Rank();
		this.initializeBuffers();
		this.initializeReplicaManagers();
		
		logger.info(
			String.format(
				"FE %d: Hej, this is FE %d", rank, rank
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
				"FE %d: Set log-level to %s", rank, logLevel.toString()
			)
		);
	}

	/**
	 * Posts the given message (which is a string)
	 * to some random {@link ReplicationManager}.
	 * 
	 * @param message		which will be sent
	 */
	public void postMessage(String message) {
		// Pick a random replica manager
		// This gives a random integer between 0 (inclusive) and 
		// replicaManagerIds (exclusive)
		Random r = new Random();
		int replicaManagerId = r.nextInt(replicaManagerIds.length);
		
		// Prepare message and send to some replica manager
		ActionMessage[] msg = new ActionMessage[1];
		msg[0] = new ActionMessage();
		msg[0].setTimestamp(timestamp);
		msg[0].setTitle(message);
		msg[0].setUserId(rank);
		msg[0].setOperation(ActionMessage.Operation.INSERT);
		comm.Isend(msg, 0, msg.length, MPI.OBJECT, replicaManagerIds[replicaManagerId], Utils.UPDATE_TAG);
		
		// Keep the message for the record
		LogRecord log = new LogRecord();
		log.setMessage(msg[0]);
		log.setReplicationManagerId(replicaManagerIds[replicaManagerId]);
		log.setTimestamp(timestamp);
		sentMessages.add(log);
		
		logger.info(
			String.format(
				"FE %d: Sending message (ID: %s) to RM %d", rank, msg[0].getId(), replicaManagerIds[replicaManagerId]
			)
		);
	}
	
	/**
	 * Sends an update-request for the given
	 * {@link ActionMessage} to an available
	 * {@link ReplicationManager}.
	 * 
	 * @param msg		the {@link ActionMessage} which
	 * 					should get updated
	 * @param message	the new value of the message
	 */
	public void updateMessage(ActionMessage msg, String message) {
		/*
		 * TODO: Implement
		 * 
		 * - Pick randomly some message from sentMessages
		 * - Copy it to a new object
		 * - Change the message-string
		 * - Send it to some RM using the 
		 *   ActionMessage.Operation.UPDATE operation
		 * - Save it to the sentMessages-Log
		 */
	}
	
	/**
	 * Sends a delete-request for the given
	 * {@link ActionMessage} to an available
	 * {@link ReplicationManager}.
	 * 
	 * @param msg		the {@link ActionMessage} which
	 * 					should get deleted
	 */
	public void deleteMessage(ActionMessage msg) {
		/*
		 * TODO: Implement
		 * 
		 * - Pick randomly some message from sentMessages
		 * - Copy it to a new object
		 * - Send it to some RM using the 
		 *   ActionMessage.Operation.DELETE operation
		 * - Save it to the sentMessages-Log
		 */
	}

	/**
	 * Sends a query-request to an available
	 * {@link ReplicationManager}. The query
	 * tells the {@link Frontend} the current
	 * time stamps of all available 
	 * replication managers known to the
	 * queried replication manager.
	 */
	public void query() {
		// Get a random replica manager
		Random r = new Random();
		int replicaManagerId = r.nextInt(replicaManagerIds.length);
		
		// Set up query-message and send it
		// to some replica manager
		QueryMessage[] msg = new QueryMessage[1];
		msg[0] = new QueryMessage();
		msg[0].setTimestamp(timestamp);
		msg[0].setFrontendId(rank);
		comm.Isend(msg, 0, msg.length, MPI.OBJECT, replicaManagerIds[replicaManagerId], Utils.QUERY_TAG);
		
		// Keep the query for the record
		LogRecord log = new LogRecord();
		log.setMessage(msg[0]);
		log.setReplicationManagerId(replicaManagerIds[replicaManagerId]);
		log.setTimestamp(timestamp);
		sentMessages.add(log);
		
		logger.info(
			String.format(
				"FE %d: Sending query (ID: %s) to RM %d", rank, msg[0].getId(), replicaManagerIds[replicaManagerId]
			)
		);
	}
	
	
	/**
	 * Listens to all available {@link ReplicationManager}s for
	 * possible answers for sent update-requests using
	 * the UPATE-tag from {@link Utils}.
	 */
	public void listenToUpdateAnswers() {
		for (int replicaManagerId = 0; replicaManagerId < replicaManagerIds.length; replicaManagerId++) {
			// If there's no answer yet for
			// current replication manager
			if (updateAnswers[replicaManagerId] == null) {
				// Set up new buffer for reception
				updateAnswers[replicaManagerId] = comm.Irecv(answerBuffer, 0, 1, MPI.OBJECT, replicaManagerIds[replicaManagerId], Utils.UPDATE_TAG);
				logger.trace(
					String.format(
						"FE %d: Listening to RM %d", rank, replicaManagerIds[replicaManagerId]
					)
				);
			}
			else {
				// There seems to be an answer from current
				// replication manager
				if (updateAnswers[replicaManagerId].Test() != null) {
					logger.info(
						String.format(
							"FE %d: Got answer from RM %d: %s", rank, replicaManagerIds[replicaManagerId], Arrays.toString(answerBuffer[0].getTimestamp())
						)
					);
					
					// Merge the time stamp from the replication manager
					// with the own time stamp
					this.timestamp = Utils.mergeTimestamps(answerBuffer[0].getTimestamp(), this.timestamp);
					logger.debug(
						String.format(
							"FE %d: Merged time stamp from RM %d: %s", rank, replicaManagerIds[replicaManagerId], Arrays.toString(this.timestamp)
						)
					);
					// Re-initialize listening to update-answers
					updateAnswers[replicaManagerId] = comm.Irecv(answerBuffer, 0, 1, MPI.OBJECT, replicaManagerIds[replicaManagerId], Utils.UPDATE_TAG);
				}
				else {
					logger.trace(
						String.format(
							"FE %d: Still listening to RM %d", rank, replicaManagerIds[replicaManagerId]
						)
					);
				}
			}
		}		
	}
	
	/**
	 * Listens to all available {@link ReplicationManager}s for
	 * possible answers for sent query-requests using
	 * the QUERY-tag from {@link Utils}.
	 */
	public void listenToQueryAnswers() {
		for (int replicaManagerId = 0; replicaManagerId < replicaManagerIds.length; replicaManagerId++) {
			// If there's no answer yet for
			// current replication manager
			if (queryAnswers[replicaManagerId] == null) {
				// Set up new buffer for reception
				queryAnswers[replicaManagerId] = comm.Irecv(queryBuffer, 0, 1, MPI.OBJECT, replicaManagerIds[replicaManagerId], Utils.QUERY_TAG);
				logger.trace(
					String.format(
						"FE %d: Listening to RM %d", rank, replicaManagerIds[replicaManagerId]
					)
				);
			}
			else {
				// There seems to be an answer from current
				// replication manager
				if (queryAnswers[replicaManagerId].Test() != null) {
					logger.info(
						String.format(
							"FE %d: Got query-answer from RM %d: %s", rank, replicaManagerIds[replicaManagerId], Arrays.toString(queryBuffer[0].getTimestamp())
						)
					);
					
					// Set the component-wise maximum
					// from the time stamp returned by the current replication
					// manager and the own time stamp.
					this.timestamp = Utils.getMaximizedTimestamp(queryBuffer[0].getTimestamp(), this.timestamp);
					
					// Some literature recommends merging
					// the time stamps although. So, at
					// first this was implemented
					// this.timestamp = Utils.mergeTimestamps(queryBuffer[0].getTimestamp(), this.timestamp);
					
					logger.debug(
						String.format(
							"FE %d: Merged time stamp from RM %d: %s", rank, replicaManagerIds[replicaManagerId], Arrays.toString(this.timestamp)
						)
					);
					// Re-initialize listening to query-answers
					queryAnswers[replicaManagerId] = comm.Irecv(queryBuffer, 0, 1, MPI.OBJECT, replicaManagerIds[replicaManagerId], Utils.QUERY_TAG);
				}
				else {
					logger.trace(
						String.format(
							"FE %d: Still listening to query-answers from RM %d", rank, replicaManagerIds[replicaManagerId]
						)
					);
				}
			}
		}		
	}

	/**
	 * Returns all yet sent messages.
	 * 
	 * @return		sent messages
	 */
	public ArrayList<LogRecord> getSentMessages() {
		return sentMessages;
	}
	
	/**
	 * Convenience method for printing all
	 * sent messages within the configured
	 * logger.
	 */
	public void printSentMessages() {

		for (LogRecord msg : this.sentMessages) {
			logger.debug(
				String.format(
					"FE %d - sent messages:	To RM: %d; TS: %s; UUID: %s", rank, msg.getReplicationManagerId(), Arrays.toString(msg.getTimestamp()), msg.getMessage().getId()
				)
			);	
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
		// Set a default level, this can
		// get overridden later on although
		logger.setLevel(Level.INFO);
	}

	/**
	 * Sets up all available {@link ReplicationManager}s
	 * to communicate with.
	 * This is kind of hard-coded as this
	 * is only a simulation at the moment.
	 */
	private void initializeReplicaManagers() {
		// Always take the RM with the least distant
		// ID assuming we have always n RM and n FE.
		// E.g.: Size is 6, FE-ID is 6, then
		// RM-ID = FE-ID - (Size / 2) = 3
		for (int i = 0; i < replicaManagerIds.length; i++) {
			replicaManagerIds[i] = rank - (size / 2);
		}
	}
	
	/**
	 * Initializes all the needed buffers.
	 */
	private void initializeBuffers() {
		timestamp = new int[size / 2];
		replicaManagerIds = new int[size / 2];
		sentMessages = new ArrayList<LogRecord>();
		updateAnswers = new Request[replicaManagerIds.length];
		answerBuffer = new ActionMessage[replicaManagerIds.length];
		queryAnswers = new Request[replicaManagerIds.length];
		queryBuffer = new QueryMessage[replicaManagerIds.length];
	}
}
