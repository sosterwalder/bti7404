import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Properties;

import org.apache.log4j.Level;

import p2pmpi.mpi.IntraComm;
import p2pmpi.mpi.MPI;

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
	
	private boolean isRunning = false;
	private boolean isRM = false;
	private double runningTime = RUNNING_TIME;
	private double gossipTime = GOSSIP_TIME;
	private double updateTime = UPDATE_TIME;
	private Level rmLogLevel = RM_LOG_LEVEL;
	private Level feLogLevel = FE_LOG_LEVEL;
	
	
	public boolean isRunning() {
		return isRunning;
	}
	
	public boolean getIsRm() {
		return this.isRM;
	}
	
	public void setIsRm(boolean isRm) {
		this.isRM = isRm;
	}
	
	public void start() {
		isRunning = true;
	}
	
	public void stop() {
		isRunning = false;
	}
	
	public double getRunningTime() {
		return this.runningTime;
	}
	
	public double getGossipTime() {
		return this.gossipTime;
	}
	
	public double getUpdateTime() {
		return this.updateTime;
	}
	
    public Level getRmLogLevel() {
		return rmLogLevel;
	}

	public void setRmLogLevel(Level rmLogLevel) {
		this.rmLogLevel = rmLogLevel;
	}

	public Level getFeLogLevel() {
		return feLogLevel;
	}

	public void setFeLogLevel(Level feLogLevel) {
		this.feLogLevel = feLogLevel;
	}

	public static void main(String args[])
    {
    	MainRunner mr = new MainRunner();
    	mr.parseConfig();
        
        MPI.Init(args);
        IntraComm comm = MPI.COMM_WORLD;
 
        int rank = comm.Rank();
        int size = comm.Size();

        mr.setIsRm((rank < (size / 2)));

		long startTime = new Date().getTime();

		mr.start();
		
		if (mr.getIsRm()) {
			ReplicationManager rm = new ReplicationManager();
			rm.setLogLevel(mr.getRmLogLevel());
			
			while (mr.isRunning()) {
				long currentTime = new Date().getTime();
				long timeDifference = currentTime - startTime;
				
				if (timeDifference > (1000.0d * mr.getRunningTime())) {
					mr.stop();
				}
				
				if ((timeDifference % (1000.0d * mr.getGossipTime())) == 0 ) {
					rm.sendGossip();
				}
				else {
					rm.listenToGossip();
					rm.listenToUpdates();
					rm.listenToQueries();
				}
			}
			
			rm.printExecutedMessages();
		} else {
			Frontend fe = new Frontend();
			fe.setLogLevel(mr.getFeLogLevel());
			
			while (mr.isRunning()) {
				long currentTime = new Date().getTime();
				long timeDifference = currentTime - startTime;
				
				if (timeDifference > (1000.0d * mr.getRunningTime())) {
					mr.stop();
				}
				
				if ((timeDifference % (1000.0d * mr.getUpdateTime())) == 0 ) {
					if (Math.random() < 0.5d) { 
						fe.postMessage(
							String.format(
								"This is some nice message :D number %d",
								fe.getSentMessages().size()
							)
						);
					}
					else {
						fe.query();
						// TODO: Check query
					}
				}
				
				fe.listenToUpdateAnswers();
				fe.listenToQueryAnswers();
			}
			
			fe.printSentMessages();
		}
     
        MPI.Finalize();
    }

	private void parseConfig() {
		Properties prop = new Properties();

		try {
			File configFilePath = new File(System.getProperty("user.dir"));
			File configFile = new File(configFilePath, "config.properties");
			prop.load(new FileInputStream(configFile.getPath()));


			this.runningTime = Double.valueOf(prop.getProperty("runningTime", String.valueOf(MainRunner.RUNNING_TIME)));
			this.gossipTime = Double.valueOf(prop.getProperty("gossipTime", String.valueOf(MainRunner.GOSSIP_TIME)));
			this.updateTime = Double.valueOf(prop.getProperty("updateTime", String.valueOf(MainRunner.UPDATE_TIME)));
			
			this.setRmLogLevel(Level.toLevel(prop.getProperty("replication_manager.log_level", MainRunner.RM_LOG_LEVEL.toString())));
			this.setFeLogLevel(Level.toLevel(prop.getProperty("frontend.log_level", MainRunner.FE_LOG_LEVEL.toString())));

		} catch (IOException ex) {
			System.out.println("-------------- EXCEPTION READING CFG! " + ex.toString());
			
			this.runningTime = RUNNING_TIME;
			this.gossipTime = GOSSIP_TIME;
			this.updateTime = UPDATE_TIME;
			
			this.setRmLogLevel(RM_LOG_LEVEL);
			this.setFeLogLevel(FE_LOG_LEVEL);
		}
	}
}
