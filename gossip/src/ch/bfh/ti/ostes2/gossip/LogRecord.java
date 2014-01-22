/*
 * LogRecord.java
 * 
 * 1.2
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


/**
 * This class represents a single record
 * within the update-log of a {@link ReplicationManager}.
 * It contains references to the {@link ReplicationManager}
 * which stored the record, the time stamp
 * of the {@link ReplicationManager} during the storing
 * as well as message of the record itself.
 * 
 * The class implements Comparable and overrides the compareTo() method
 * allowing the comparison of log records.
 * 
 * @author sosterwalder
 *
 */
public class LogRecord implements Serializable, Comparable<LogRecord> {
	private static final long serialVersionUID = 5086244161314321504L;
	private int replicationManagerId = Integer.MAX_VALUE;
	private int[] timestamp = null;
	private Message message = null;
	
	/**
	 * Returns the id of the {@link ReplicationManager}
	 * which set this {@link LogRecord}.
	 * 
	 * @return 		the id of the {@link ReplicationManager}
	 */
	public int getReplicationManagerId() {
		return replicationManagerId;
	}
	
	/**
	 * Sets the id of the {@link ReplicationManager}
	 * which uses this {@link LogRecord}.
	 * 
	 * @param replicationManagerId		the id of the {@link ReplicationManager}
	 */
	public void setReplicationManagerId(int replicationManagerId) {
		this.replicationManagerId = replicationManagerId;
	}
	
	/**
	 * Returns the current set time stamp
	 * when using this {@link LogRecord}. 
	 * 
	 * @return		the time stamp
	 */
	public int[] getTimestamp() {
		return timestamp;
	}
	
	/**
	 * Sets the current time stamp
	 * when using this {@link LogRecord}.
	 * 
	 * @param timestamp		the time stamp
	 */
	public void setTimestamp(int[] timestamp) {
		this.timestamp = timestamp;
	}
	
    /**
     * Returns the {@link Message} of the {@link LogRecord}
     * which can be either an {@link ActionMessage},
     * a {@link QueryMessage} or a {@link GossipMessage}.
     * 
     * @return		the (abstract) message
     */
    public Message getMessage() {
		return message;
	}

	/**
	 * Sets the {@link Message} of the {@link LogRecord}.
	 * 
	 * @param message	the (abstract) message
	 */
	public void setMessage(Message message) {
		this.message = message;
	}

	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
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