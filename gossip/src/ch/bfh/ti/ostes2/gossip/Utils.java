/*
 * utils.java
 * 
 * 2.4
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

/**
 * A class providing various
 * helper functions for time stamps
 * as well as definitions of tags
 * used for communication of {@link Message}s.
 * 
 * @author sosterwalder
 *
 */
public final class Utils {
	public final static int QUERY_TAG = 900;
	public final static int UPDATE_TAG = 901;
	public final static int GOSSIP_TAG = 902;
	
	/**
	 * Checks if the first provided time stamp
	 * is smaller or equal as the second provided 
	 * time stamp, assuming that the time stamps have the
	 * same length.
	 * 
	 * E.g.: 	ts1: [2, 3]
	 *			ts2: [5, 1]
	 *			=> res: false (3 > 1)
	 * 
	 * @param timestampOne		the target time stamp
	 * @param timestampTwo		the source time stamp
	 * @return					true if timestampOne
	 * 							is smaller or equal,
	 * 							otherwise false
	 */
	public final static boolean isSmallerOrEqualThan(int[] timestampOne, int[] timestampTwo) {
		boolean isSmallerOrEqualThan = true;
		
		assert(timestampOne.length == timestampTwo.length);
		
		for (int i = 0; i < timestampOne.length; i++) {
			if (timestampOne[i] > timestampTwo[i]) {
				isSmallerOrEqualThan = false;
			}
		}
		
		return isSmallerOrEqualThan;
	}
	
	/**
	 * Returns the (component-wise) maximum 
	 * of two given time stamps,
	 * assuming that the time stamps have the
	 * same length. 
	 * 
	 * E.g.: 	ts1: [2, 3]
	 *			ts2: [5, 1]
	 *			=> res: [5, 3]
	 * 
	 * @param timestampOne
	 * @param timestampTwo
	 * @return		the maximum out of the
	 * 				two time stamps
	 */
	public final static int[] getMaximizedTimestamp(int[] timestampOne, int[] timestampTwo) {
		assert(timestampOne.length == timestampTwo.length);
		
		int[] maximizedTimestamp = new int[timestampOne.length];
		
		for (int i = 0; i < timestampOne.length; i++) {
			if (timestampOne[i] > timestampTwo[i]) {
				maximizedTimestamp[i] = timestampOne[i];
			}
			else {
				maximizedTimestamp[i] = timestampTwo[i];
			}
		}
		
		return maximizedTimestamp;
	}
	
	/**
	 * Returns the (component-wise) minimum 
	 * of two given time stamps, assuming that the time stamps have the
	 * same length.
	 * 
	 * E.g.: 	ts1: [2, 3]
	 *			ts2: [5, 1]
	 *			=> res: [2, 1]
	 * 
	 * @param timestampOne
	 * @param timestampTwo
	 * @return		the minimum out of the
	 * 				two time stamps
	 */
	public final static int[] getMinimizedTimestamp(int[] timestampOne, int[] timestampTwo) {
		assert(timestampOne.length == timestampTwo.length);
		
		int[] minimizedTimestamp = new int[timestampOne.length];
		
		for (int i = 0; i < timestampOne.length; i++) {
			if (timestampOne[i] < timestampTwo[i]) {
				minimizedTimestamp[i] = timestampOne[i];
			}
			else {
				minimizedTimestamp[i] = timestampTwo[i];
			}
		}
		
		return minimizedTimestamp;
	}
	
	/**
	 * Merges two given time stamps to their
	 * maximal value, assuming that the time stamps have the
	 * same length.
	 * 
	 * E.g.: 	ts1: [2, 3]
	 *			ts2: [5, 1]
	 *			=> res: [5, 3]
	 * 
	 * @param timestampOne
	 * @param timestampTwo
	 * @return		the maximum out of the
	 * 				two time stamps
	 */
	public final static int[] mergeTimestamps(int[] timestampOne, int[] timestampTwo) {		
		assert(timestampOne.length == timestampTwo.length);
		
		int[] mergedTimestamps = new int[timestampOne.length];
		
		for (int i = 0; i < timestampOne.length; i++) {
			if (timestampOne[i] >= timestampTwo[i]) {
				mergedTimestamps[i] = timestampOne[i];
			}
			else {
				mergedTimestamps[i] = timestampTwo[i];
			}
		}
		
		return mergedTimestamps;
	}
}
