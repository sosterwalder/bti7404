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
	
	public void setLogLevel(Level logLevel) {
		logger.setLevel(logLevel);
		logger.info(
			String.format(
				"FE %d: Set log-level to %s", rank, logLevel.toString()
			)
		);
	}

	public void postMessage(String message) {
		// Pick a random replica manager
		// This gives a random integer between 0 (inclusive) and 
		// replicaManagerIds (exclusive)
		Random r = new Random();
		int replicaManagerId = r.nextInt(replicaManagerIds.length);
		
		ActionMessage[] msg = new ActionMessage[1];
		msg[0] = new ActionMessage();
		msg[0].setTimestamp(timestamp);
		msg[0].setTitle(message);
		msg[0].setUserId(rank);
		msg[0].setOperation(ActionMessage.Operation.INSERT);
		comm.Isend(msg, 0, msg.length, MPI.OBJECT, replicaManagerIds[replicaManagerId], Utils.UPDATE_TAG);
		
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

	public void query() {
		Random r = new Random();
		int replicaManagerId = r.nextInt(replicaManagerIds.length);
		
		QueryMessage[] msg = new QueryMessage[1];
		msg[0] = new QueryMessage();
		msg[0].setTimestamp(timestamp);
		msg[0].setFrontendId(rank);
		comm.Isend(msg, 0, msg.length, MPI.OBJECT, replicaManagerIds[replicaManagerId], Utils.QUERY_TAG);
		
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
	
	public void listenToUpdateAnswers() {
		for (int replicaManagerId = 0; replicaManagerId < replicaManagerIds.length; replicaManagerId++) {
			if (updateAnswers[replicaManagerId] == null) {
				updateAnswers[replicaManagerId] = comm.Irecv(answerBuffer, 0, 1, MPI.OBJECT, replicaManagerIds[replicaManagerId], Utils.UPDATE_TAG);
				logger.trace(
					String.format(
						"FE %d: Listening to RM %d", rank, replicaManagerIds[replicaManagerId]
					)
				);
			}
			else {
				if (updateAnswers[replicaManagerId].Test() != null) {
					logger.info(
						String.format(
							"FE %d: Got answer from RM %d: %s", rank, replicaManagerIds[replicaManagerId], Arrays.toString(answerBuffer[0].getTimestamp())
						)
					);
					
					this.timestamp = Utils.mergeTimestamps(answerBuffer[0].getTimestamp(), this.timestamp);
					logger.debug(
						String.format(
							"FE %d: Merged time stamp from RM %d: %s", rank, replicaManagerIds[replicaManagerId], Arrays.toString(this.timestamp)
						)
					);
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
	
	public void listenToQueryAnswers() {
		for (int replicaManagerId = 0; replicaManagerId < replicaManagerIds.length; replicaManagerId++) {
			if (queryAnswers[replicaManagerId] == null) {
				queryAnswers[replicaManagerId] = comm.Irecv(queryBuffer, 0, 1, MPI.OBJECT, replicaManagerIds[replicaManagerId], Utils.QUERY_TAG);
				logger.trace(
					String.format(
						"FE %d: Listening to RM %d", rank, replicaManagerIds[replicaManagerId]
					)
				);
			}
			else {
				if (queryAnswers[replicaManagerId].Test() != null) {
					logger.info(
						String.format(
							"FE %d: Got query-answer from RM %d: %s", rank, replicaManagerIds[replicaManagerId], Arrays.toString(queryBuffer[0].getTimestamp())
						)
					);
					
					this.timestamp = Utils.mergeTimestamps(queryBuffer[0].getTimestamp(), this.timestamp);
					logger.debug(
						String.format(
							"FE %d: Merged time stamp from RM %d: %s", rank, replicaManagerIds[replicaManagerId], Arrays.toString(this.timestamp)
						)
					);
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

	public ArrayList<LogRecord> getSentMessages() {
		return sentMessages;
	}
	
	public void printSentMessages() {

		for (LogRecord msg : this.sentMessages) {
			logger.debug(
				String.format(
					"FE %d - sent messages:	To RM: %d; TS: %s; UUID: %s", rank, msg.getReplicationManagerId(), Arrays.toString(msg.getTimestamp()), msg.getMessage().getId()
				)
			);	
		}
	}
	
	private void setupLogger() {
		SimpleLayout layout = new SimpleLayout();
		ConsoleAppender consoleAppender = new ConsoleAppender( layout );
		
		logger = LogManager.getLogger(ReplicationManager.class.getName());
		logger.addAppender(consoleAppender);
		logger.setLevel(Level.INFO);
	}

	private void initializeReplicaManagers() {
		// Always take the RM with the least distant
		// ID assuming we have always n RM and n FE.
		// E.g.: Size is 6, FE-ID is 6, then
		// RM-ID = FE-ID - (Size / 2) = 3
		for (int i = 0; i < replicaManagerIds.length; i++) {
			replicaManagerIds[i] = rank - (size / 2);
		}
	}
	
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
