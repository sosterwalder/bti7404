import java.io.Serializable;


public class LogRecord implements Serializable, Comparable<LogRecord> {
	private static final long serialVersionUID = 5086244161314321504L;
	private int replicationManagerId = Integer.MAX_VALUE;
	private int[] timestamp = null;
	private Message message = null;
	
	public int getReplicationManagerId() {
		return replicationManagerId;
	}
	
	public void setReplicationManagerId(int replicationManagerId) {
		this.replicationManagerId = replicationManagerId;
	}
	
	public int[] getTimestamp() {
		return timestamp;
	}
	
	public void setTimestamp(int[] timestamp) {
		this.timestamp = timestamp;
	}
	
    public Message getMessage() {
		return message;
	}

	public void setMessage(Message message) {
		this.message = message;
	}

	@Override
    public int compareTo(LogRecord logRecord) {
    	final int BEFORE = -1;
        final int AFTER = 1;
        
        if (Utils.isSmallerOrEqualThan(this.timestamp, logRecord.timestamp)) {
        	return BEFORE;
        }
        else {
        	return AFTER;
        }
    }
}