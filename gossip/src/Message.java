import java.util.UUID;

public interface Message {
	public UUID getId();
	public int[] getTimestamp();
	public void setTimestamp(int timestamp[]);
}
