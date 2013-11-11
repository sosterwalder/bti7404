import java.io.Serializable;


public class Message implements Serializable {
	private static final long serialVersionUID = 1L;
	int rank;
	// -1: Unknown
	//  0: Is candidate
	//  1: Is leader
	int messageType;
	int leader;
	boolean isMessageReceived;
}