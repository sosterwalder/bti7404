/*
 * MainRunner.java
 * 
 * 2.1
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

import java.io.IOException;
import java.util.Date;
import java.util.Properties;

import org.apache.log4j.Level;

import p2pmpi.mpi.IntraComm;
import p2pmpi.mpi.MPI;
import p2pmpi.p2p.message.UpdateMessage;

/**
 * The {@link MainRunner} class serves
 * as the entry point for the P2P-MPI
 * application. It provides a handling
 * of either a {@link Frontend} or a
 * {@link ReplicationManager} within the
 * gossip-architecture.
 * 
 * The main runner acts as a {@link ReplicationManager}
 * when its rank is below the middle of the
 * available P2P-MPI processes. Otherwise
 * it acts as a {@link Frontend}.
 * 
 * The static variables can be set directly within
 * the class but they serve only as a fall back. 
 * Normally the variables get provided by the
 * config.properties file.
 * 
 * @author sosterwalder
 *
 */
class MainRunner
{
	// Overall running time in seconds
	public static final double RUNNING_TIME = 60.0d;
	// When to send gossip messages in seconds
	public static final double GOSSIP_TIME = 5.0d;
	// When to send update messages in seconds
	public static final double UPDATE_TIME = 15.0d;
	// Default log level for RM
	public static final Level RM_LOG_LEVEL = Level.DEBUG;
	// Default log level for FE
	public static final Level FE_LOG_LEVEL = Level.DEBUG;
	// Name of the configuration file
	public static final String CONFIG_FILE =  "/config.properties";
	
	private boolean isRunning = false;
	private boolean isRM = false;
	private double runningTime = RUNNING_TIME;
	private double gossipTime = GOSSIP_TIME;
	private double updateTime = UPDATE_TIME;
	private Level rmLogLevel = RM_LOG_LEVEL;
	private Level feLogLevel = FE_LOG_LEVEL;
	
	
	/**
	 * Tells whether the process
	 * is running or not.
	 * 
	 * @return		the running state
	 * 				of the application
	 */
	public boolean isRunning() {
		return isRunning;
	}
	
	/**
	 * Returns if the current process
	 * is a {@link ReplicationManager}
	 * or not.
	 * 
	 * @return		the 'role' of the
	 * 				current process
	 */
	public boolean getIsRm() {
		return this.isRM;
	}
	
	/**
	 * Sets whether the current process
	 * is a {@link ReplicationManager}
	 * or not.
	 * 
	 * @param isRm
	 */
	public void setIsRm(boolean isRm) {
		this.isRM = isRm;
	}
	
	/**
	 * Starts the main work flow
	 * of the current process.
	 */
	public void start() {
		isRunning = true;
	}
	
	/**
	 * Stops the main work flow
	 * of the current process.
	 */
	public void stop() {
		isRunning = false;
	}
	
	/**
	 * Returns the currently
	 * defined running time
	 * (in seconds) which defines
	 * how long the current process 
	 * should be running.
	 * 
	 * @return		the defined time
	 * 				for the process
	 * 				to run in seconds
	 */
	public double getRunningTime() {
		return this.runningTime;
	}
	
	/**
	 * Returns the currently
	 * defined time (in seconds)
	 * which defines when a {@link GossipMessage}
	 * should be sent.
	 * 
	 * @return		the defined time
	 * 				for the process
	 * 				to send a {@link GossipMessage}
	 */
	public double getGossipTime() {
		return this.gossipTime;
	}
	
	/**
	 * Returns the currently
	 * defined time (in seconds)
	 * which defines when the
	 * current process should send
	 * a {@link UpdateMessage}.
	 * 
	 * @return		the defined time
	 * 				for the process
	 * 				to send a {@link UpdateMessage}
	 */
	public double getUpdateTime() {
		return this.updateTime;
	}
	
    /**
     * Returns the current set
     * log level for {@link ReplicationManager}s.
     * 
     * @return		the log level
     * 				for {@link ReplicationManager}s.
     */
    public Level getRmLogLevel() {
		return rmLogLevel;
	}

	/**
	 * Sets the log level
	 * for {@link ReplicationManager}s.
	 * 
	 * @param rmLogLevel	the log level
	 */
	public void setRmLogLevel(Level rmLogLevel) {
		this.rmLogLevel = rmLogLevel;
	}

    /**
     * Returns the current set
     * log level for {@link Frontend}s.
     * 
     * @return		the log level
     * 				for {@link Frontend}s.
     */
	public Level getFeLogLevel() {
		return feLogLevel;
	}

	/**
	 * Sets the log level
	 * for {@link Frontend}s.
	 * 
	 * @param rmLogLevel	the log level
	 */
	public void setFeLogLevel(Level feLogLevel) {
		this.feLogLevel = feLogLevel;
	}

	/**
	 * The main entry point of the
	 * application.
	 * 
	 * @param args		possibly set
	 * 					(command line) arguments
	 */
	public static void main(String args[])
    {
    	MainRunner mr = new MainRunner();
    	mr.parseConfig();
        
        MPI.Init(args);
        IntraComm comm = MPI.COMM_WORLD;
 
        int rank = comm.Rank();
        int size = comm.Size();

        // Set the current process
        // to act as a replication manager
        // when its rank is below the
        // half of the available
        // P2P-MPI processes
        mr.setIsRm((rank < (size / 2)));

		long startTime = new Date().getTime();

		mr.start();
		
		// Determine if the current process
		// is a replication manager or not
		if (mr.getIsRm()) {
			ReplicationManager rm = new ReplicationManager();
			rm.setLogLevel(mr.getRmLogLevel());
			
			while (mr.isRunning()) {
				long currentTime = new Date().getTime();
				long timeDifference = currentTime - startTime;
				
				// If the current running time exceeds
				// the set desired running time, stop
				// the process
				if (timeDifference > (1000.0d * mr.getRunningTime())) {
					mr.stop();
				}
				
				// If the current running time matches the
				// defined desired time for sending gossip
				// messages, send a gossip message
				if ((timeDifference % (1000.0d * mr.getGossipTime())) == 0 ) {
					rm.sendGossip();
				}
				else {
					// Listening/Processing loop
					// when nothing other has to be done
					rm.listenToGossip();
					rm.listenToUpdates();
					rm.listenToQueries();
					rm.processQueuedQueries();
				}
			}
			
			// After stopping, output the executed
			// messages
			rm.printExecutedMessages();
		} else {
			Frontend fe = new Frontend();
			fe.setLogLevel(mr.getFeLogLevel());
			
			while (mr.isRunning()) {
				long currentTime = new Date().getTime();
				long timeDifference = currentTime - startTime;
				
				// If the current running time exceeds
				// the set desired running time, stop
				// the process.. yes, this is the same code
				// as above.. this should be generalized..
				// but you know how things work :)
				if (timeDifference > (1000.0d * mr.getRunningTime())) {
					mr.stop();
				}
				
				// If the current running time matches the
				// defined desired time for performing an update,
				// perform an update
				if ((timeDifference % (1000.0d * mr.getUpdateTime())) == 0 ) {
					// We randomly either perform an update
					// or send a query
					if (Math.random() < 0.5d) {
						/*
						 * TODO
						 * Change randomly between:
						 *   - postMessage
						 *   - updateMessage
						 *   - deleteMessage
						 */
						fe.postMessage(
							String.format(
								"This is some nice message :D number %d",
								fe.getSentMessages().size()
							)
						);
					}
					else {
						fe.query();
					}
				}
				
				fe.listenToUpdateAnswers();
				fe.listenToQueryAnswers();
			}
			
			// After stopping, output the sent
			// messages
			fe.printSentMessages();
		}
     
        MPI.Finalize();
    }

	/**
	 * Searches for 
	 */
	private void parseConfig() {
		Properties prop = new Properties();

		// Try to read the configuration values
		// from the MainRunner.CONFIG_FILE within
		// the JAR-file
		try {
			prop.load(getClass().getResourceAsStream(MainRunner.CONFIG_FILE));
			
			this.runningTime = Double.valueOf(prop.getProperty("runningTime", String.valueOf(MainRunner.RUNNING_TIME)));
			this.gossipTime = Double.valueOf(prop.getProperty("gossipTime", String.valueOf(MainRunner.GOSSIP_TIME)));
			this.updateTime = Double.valueOf(prop.getProperty("updateTime", String.valueOf(MainRunner.UPDATE_TIME)));
			
			this.setRmLogLevel(Level.toLevel(prop.getProperty("replication_manager.log_level", MainRunner.RM_LOG_LEVEL.toString())));
			this.setFeLogLevel(Level.toLevel(prop.getProperty("frontend.log_level", MainRunner.FE_LOG_LEVEL.toString())));

		} catch (IOException ex) {
			// Fall back when reading the MainRunner.CONFIG-FILE
			// is not possible
			System.out.println("-------------- EXCEPTION READING CFG! " + ex.toString());
			
			this.runningTime = RUNNING_TIME;
			this.gossipTime = GOSSIP_TIME;
			this.updateTime = UPDATE_TIME;
			
			this.setRmLogLevel(RM_LOG_LEVEL);
			this.setFeLogLevel(FE_LOG_LEVEL);
		}
	}
}
