/*
 * ActionMessage.java
 * 
 * 1.3
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
import java.util.UUID;

/**
 * This class acts as container for messages
 * used either to perform updates from a {@link Frontend}
 * to a {@link ReplicationManager} or for a ReplicationManager
 * to answer a request from a Frontend.  
 * 
 * @author sosterwalder
 *
 */
public class ActionMessage implements Serializable, Message {
	public enum Operation {
		INSERT, UPDATE, DELETE
	}
	private static final long serialVersionUID = 2479571943630888851L;
	private UUID id = UUID.randomUUID();
	private int timestamp[] = null;
	private int userId	= Integer.MAX_VALUE;
	private String title = "";
	private String body = "";
	private Operation operation = null;

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
	 * Returns the title of the {@link ActionMessage}.
	 * 
	 * @return			the title of the action message
	 */
	public String getTitle() {
		return title;
	}
	
	/**
	 * Sets the title of the {@link ActionMessage}.
	 * 
	 * @param title		the title which the action
	 * 					message shall have
	 */
	public void setTitle(String title) {
		this.title = title;
	}
	
	/**
	 * Returns the body of the {@link ActionMessage}.
	 * 
	 * @return			the body of the message
	 */
	public String getBody() {
		return body;
	}
	
	/**
	 * Sets the body of the {@link ActionMessage}.
	 * 
	 * @param body		the body which the 
	 * 					action message shall have
	 */
	public void setBody(String body) {
		this.body = body;
	}
	
	/**
	 * Returns the (user-) id of the creator
	 * of the {@link ActionMessage}.
	 * 
	 * @return			the id of the creator 
	 * 					of the message 
	 */
	public int getUserId() {
		return userId;
	}

	/**
	 * Sets the user-id which is the creator
	 * of the {@link ActionMessage}.
	 * 
	 * @param userId	the id of the user who
	 * 					wrote the message
	 */
	public void setUserId(int userId) {
		this.userId = userId;
	}

	/**
	 * Returns the operation which will
	 * be performed on the {@link ReplicationManager}
	 * with this {@link ActionMessage}. 
	 * 
	 * @return			the operation which shall performed
	 * 					with the message
	 */
	public Operation getOperation() {
		return operation;
	}

	/**
	 * Sets the operation which will
	 * be performed on the {@link ReplicationManager}
	 * with this {@link ActionMessage}.
	 * 
	 * @param operation	decides what happens with the
	 * 					message on a replication manager
	 */
	public void setOperation(Operation operation) {
		this.operation = operation;
	}
	
	/**
	 * Applies the attributes user-id, title
	 * and body from given {@link ActionMessage}.
	 * 
	 * @param msg		contains attributes to
	 * 					set on the current message
	 */
	public void udpate(ActionMessage msg) {
		this.userId = msg.getUserId();
		this.title = msg.getTitle();
		this.body = msg.getBody();
	}
}
