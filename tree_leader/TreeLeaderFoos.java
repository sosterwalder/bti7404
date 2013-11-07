import p2pmpi.mpi.MPI;
import p2pmpi.mpi.Request;

public class TreeLeaderFoos {

	static int rank, size;
	// message types
	static final int IS_CANDIDATE=0;
	static final int IS_LEADER=1;	
	
	// nb. of processes (i.e nodes of the graph) 
	static final int N = 10;
	// incidence list of our graph  
	//
	//   0    1
	//   |    |
	// 2-3-4  5-6-7
	//   |    |
	//   8    9
	//
	static final int incList[][] = {
		{3},  		// 0
		{5},  		// 1
		{3},  		// 2
		{0,2,4,8}, 	// 3
		{3},  		// 4
		{1,6,9},		// 5
		{5,7},  		// 6
		{6},  		// 7
		{3},  		// 8
		{5}}; 		// 9
	static int [] neighbours;		
	static int timeCap = 10;  
	
	public static int findLeader(){
		int currentLeader = rank;
		// we find our neighbours in incList:
		int deg = neighbours.length;
		// we print our neighbours for debugging reasons:
		StringBuffer sb = new StringBuffer("rank "+rank+" nb: ");
		for (int j=0; j<deg; j++) sb.append(neighbours[j]+", ");
		System.out.println(sb);
		// we need requests for all our neighbours
		Request[] reqs = new Request[deg];
		boolean [] hasGotMsg = new boolean[deg];
		int m = 0; // count for the successfull reads
		int [][] recBuf=null;
		recBuf = new int [deg][2];
		// we start to listen to all neighbours
		for (int nb=0; nb<deg;nb++){
			reqs[nb] = MPI.COMM_WORLD.Irecv(recBuf[nb],0, 2, MPI.INT,neighbours[nb],0);
		}
		do {
			// poll all neighbours nb:
			for (int nb=0; nb<deg;nb++){
				if ( ! hasGotMsg[nb]){ 
					if (reqs[nb].Test() != null){
						hasGotMsg[nb] = true;
						m++;
						if (currentLeader > recBuf[nb][1]){
							currentLeader = recBuf[nb][1];
						}
					}
				}
			}
			try {
				Thread.currentThread();
				Thread.sleep(timeCap);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} while (m < deg-1);
	
		if (m==deg) {
			// we got from all neighbours a message
			// so we know the leader and send it to all neigbours
			for (int nb=0;nb<deg;nb++){
				int [] sendBuf = new int[2];
				sendBuf[0] = IS_LEADER;
				sendBuf[1] = currentLeader;
				MPI.COMM_WORLD.Isend(sendBuf,0, 2, MPI.INT,neighbours[nb],0);
			}
		}
		else {
			// we have got a mesg from all but one neighbour
			int nb=0;
			while(hasGotMsg[nb]) nb++;
			// now  neighbour[nb] is the neighbour from
			// which we have not got a message
			// so we send our candidate msg to the link 'nb' where we got no msg
			int  [] sendBuf = new int[2];
			sendBuf[0] = IS_CANDIDATE;
			sendBuf[1] = currentLeader;				
			Request rr =MPI.COMM_WORLD.Isend(sendBuf,0, 2, MPI.INT,neighbours[nb],0);
			rr.Wait();
			// now we wait for the  msg on  link 'i'
			do{ 
				if (reqs[nb].Test()!=null){
					m=m+1;
					if (recBuf[nb][0]==IS_CANDIDATE){
						// we got a candidate msg so we find out the leader 
						// and send it to all our neighbours but not the one we got
						// the message from
						if (currentLeader > recBuf[nb][1]){
							currentLeader = recBuf[nb][1];
						}						
					}
					else {
						// we cot a leader msg and we send it to all neighbours 
						// but not to the receiving one		

						currentLeader = recBuf[nb][1];
					}
					// send a is-leader msg to all neighbours but 
					for (int k=0;k<deg;k++){
						if (k!=nb){
							int [] sndBuf = new int[2];
							sndBuf[0] = IS_LEADER;
							sndBuf[1] = currentLeader;
							rr = MPI.COMM_WORLD.Isend(sndBuf,0, 2, MPI.INT,neighbours[k],0);
							rr.Wait();
						}
					}						
				}
				try {
					Thread.currentThread();
					Thread.sleep(timeCap);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}					
			} while (m<deg);
		}
		return currentLeader;
	}

	public static void main(String[] args) {
		MPI.Init(args);
		size = MPI.COMM_WORLD.Size();
		rank = MPI.COMM_WORLD.Rank();
		neighbours = incList[rank]; // our neighbours in the tree graph
		if (size != N) System.out.println("run with -n "+N);
		else {
			int leader = findLeader();
			System.out.println("******rank "+rank+", leader: "+leader);
		}
		MPI.Finalize();
	}
}
