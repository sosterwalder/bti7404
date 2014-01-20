import java.io.Serializable;
import java.util.UUID;


public class QueryMessage implements Serializable, Message {
	private static final long serialVersionUID = -5430283984414519557L;
	private  UUID id = UUID.randomUUID();
	private int timestamp[] = null;
	private int frontendId = Integer.MAX_VALUE;

	@Override
	public UUID getId() {
		return id;
	}

	@Override
	public int[] getTimestamp() {
		return timestamp;
	}

	@Override
	public void setTimestamp(int[] timestamp) {
		this.timestamp = timestamp;
	}

	public int getFrontendId() {
		return frontendId;
	}

	public void setFrontendId(int frontendId) {
		this.frontendId = frontendId;
	}
}
