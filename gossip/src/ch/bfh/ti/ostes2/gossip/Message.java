/*
 * Message.java
 * 
 * 1.0
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

import java.util.UUID;

/**
 * This class is an interface for the message
 * containers: {@link ActionMessage} and
 * {@link GossipMessage}. It is mainly used to 
 * provide a general class for casting objects.
 * 
 * @author sosterwalder
 *
 */
public interface Message {
	/**
	 * Returns the unique ID of the {@link Message}.
	 * 
	 * @return		the unique ID of the {@link Message}
	 */
	public UUID getId();
	
	/**
	 * Returns the time stamp of the {@link Message}.
	 * 
	 * @return			the time stamp of the {@link Message}
	 */
	public int[] getTimestamp();
	
	/**
	 * Sets the time stamp for the current {@link Message}.
	 * 
	 * @param timestamp	the time stamp of the message
	 */
	public void setTimestamp(int timestamp[]);
}
