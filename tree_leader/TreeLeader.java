
import java.util.ArrayList;
import java.util.List;

import p2pmpi.mpi.MPI;

public class TreeLeader {

	static int rank, size;
	// message types
	static final int IS_CANDIDATE	= 0;
	static final int IS_LEADER		= 1;	
	
	// Number of processes (i.e nodes of the graph) 
	static final int VERTICES = 10;
	
	static int timeCap = 10;
	
	// Vertices
	private List<Vertice> mVertices;
	
	
	public TreeLeader() {
		mVertices = new ArrayList<Vertice>();
		
		for (int i = 0; i < VERTICES; i++) {
			mVertices.add(new Vertice(String.format("Vertice %d", i)));
		}
		
		// incidence list of our graph  
		//
		//   0    1
		//   |    |
		// 2-3-4  5-6-7
		//   |    |
		//   8    9

		// Vertice 0
		mVertices.get(0).addEdge(new Edge(mVertices.get(0), mVertices.get(3), 0));
		
		// Vertice 2
		mVertices.get(2).addEdge(new Edge(mVertices.get(2), mVertices.get(3), 0));
		
		// Vertice 4
		mVertices.get(4).addEdge(new Edge(mVertices.get(4), mVertices.get(3), 0));
		
		// Vertice 8
		mVertices.get(8).addEdge(new Edge(mVertices.get(8), mVertices.get(3), 0));
		
		// Vertice 3
		mVertices.get(3).addEdge(new Edge(mVertices.get(3), mVertices.get(0), 0));
		mVertices.get(3).addEdge(new Edge(mVertices.get(3), mVertices.get(2), 0));
		mVertices.get(3).addEdge(new Edge(mVertices.get(3), mVertices.get(4), 0));
		mVertices.get(3).addEdge(new Edge(mVertices.get(3), mVertices.get(8), 0));
	}  
	
	public int findLeader() {
		int currentLeader = rank;
		Vertice currentVertice = mVertices.get(rank);
		
		// we print our neighbours for debugging reasons:
		StringBuffer neighboursList = new StringBuffer("Vertice (rank) "+rank+"; Neighbours: ");
		List<Vertice> neighbours = currentVertice.getNeighbours();
		for (Vertice vertice : neighbours) {
			neighboursList.append(vertice.getName() + ", ");
		}
		System.out.println(neighboursList);
		
		/*
		
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
				Thread.currentThread().sleep(timeCap);
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
					Thread.currentThread().sleep(timeCap);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}					
			} while (m<deg);
		}
		*/
		
		return currentLeader;
	}

	public static void main(String[] args) {
		MPI.Init(args);
		size = MPI.COMM_WORLD.Size();
		rank = MPI.COMM_WORLD.Rank();
		
		TreeLeader treeLeader = new TreeLeader();
		
		if (size != VERTICES) System.out.println("run with -n "+ VERTICES);
		else {
			int leader = treeLeader.findLeader();
			System.out.println("******rank "+rank+", leader: "+leader);
		}
		MPI.Finalize();
	}
}
