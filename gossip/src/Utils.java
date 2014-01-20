public final class Utils {
	public final static int QUERY_TAG = 900;
	public final static int UPDATE_TAG = 901;
	public final static int GOSSIP_TAG = 902;
	
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
	
	public final static int[] getGreaterTimestamp(int[] timestampOne, int[] timestampTwo) {
		boolean timestampOneIsGreater = false;
		
		assert(timestampOne.length == timestampTwo.length);
		
		for (int i = 0; i < timestampOne.length; i++) {
			if (timestampOne[i] > timestampTwo[i]) {
				timestampOneIsGreater = true;
			}
		}
		
		if (timestampOneIsGreater) {
			return timestampOne;
		}
		else {
			return timestampTwo;
		}
	}
	
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
