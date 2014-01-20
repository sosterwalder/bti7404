import java.io.Serializable;
import java.util.UUID;

public class ActionMessage implements Serializable, Message {
	private static final long serialVersionUID = 2479571943630888851L;
	private UUID id = UUID.randomUUID();
	private int timestamp[] = null;
	public enum Operation {
		INSERT, UPDATE, DELETE
	}
	private int userId	= Integer.MAX_VALUE;
	private String title = "";
	private String body = "";
	private Operation operation = null;

	public UUID getId() {
		return id;
	}
	
	public int[] getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(int timestamp[]) {
		this.timestamp = timestamp;
	}
	
	public String getTitle() {
		return title;
	}
	
	public void setTitle(String title) {
		this.title = title;
	}
	
	public String getBody() {
		return body;
	}
	
	public void setBody(String body) {
		this.body = body;
	}
	
	public int getUserId() {
		return userId;
	}

	public void setUserId(int userId) {
		this.userId = userId;
	}

	public Operation getOperation() {
		return operation;
	}

	public void setOperation(Operation operation) {
		this.operation = operation;
	}
}
