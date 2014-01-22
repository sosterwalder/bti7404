/*
 * GossipMessage.java
 * 
 * 1.1
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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.UUID;


/**
 * This class acts as container for messages
 * used to send gossip messages between
 * {@link ReplicationManager}s.  
 * 
 * @author sosterwalder
 *
 */
public class GossipMessage implements Serializable, Message {
	private static final long 		serialVersionUID = -1072189410074226816L;
	private UUID id = UUID.randomUUID();
	private int timestamp[] = null;
	private ArrayList<LogRecord>	logRecords = null;
	
	/* (non-Javadoc)
	 * @see ch.bfh.ti.ostes2.gossip.Message#getId()
	 */
	public UUID getId() {
		return id;
	}

	/* (non-Javadoc)
	 * @see ch.bfh.ti.ostes2.gossip.Message#getTimestamp()
	 */
	public int[] getTimestamp() {
		return timestamp;
	}

	/* (non-Javadoc)
	 * @see ch.bfh.ti.ostes2.gossip.Message#setTimestamp(int[])
	 */
	public void setTimestamp(int timestamp[]) {
		this.timestamp = timestamp;
	}
	
	/**
	 * Returns all messages which were
	 * sent by a {@link ReplicationManager}
	 * when sending this gossip message.
	 * 
	 * @return		an array list containing
	 * 				the messages
	 */
	public ArrayList<LogRecord> getLogRecords() {
		return logRecords;
	}

	/**
	 * Sets the messages which were
	 * sent by a {@link ReplicationManager}
	 * when sending this gossip message.
	 * 
	 * @param logRecords	the messages sent by
	 * 						the replication manager
	 */
	public void setLogRecords(ArrayList<LogRecord> logRecords) {
		this.logRecords = logRecords;
	}
}