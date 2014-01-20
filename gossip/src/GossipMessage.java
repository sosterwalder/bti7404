import java.io.Serializable;
import java.util.ArrayList;
import java.util.UUID;


public class GossipMessage implements Serializable, Message {
	private static final long 		serialVersionUID = -1072189410074226816L;
	private UUID id = UUID.randomUUID();
	private int timestamp[] = null;
	private ArrayList<LogRecord>	logRecords = null;
	
	public UUID getId() {
		return id;
	}

	public int[] getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(int timestamp[]) {
		this.timestamp = timestamp;
	}
	
	public ArrayList<LogRecord> getLogRecords() {
		return logRecords;
	}

	public void setLogRecords(ArrayList<LogRecord> logRecords) {
		this.logRecords = logRecords;
	}
}