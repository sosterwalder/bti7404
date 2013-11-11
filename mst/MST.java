import java.io.Serializable;
import java.util.ArrayList;

import p2pmpi.mpi.MPI;
import p2pmpi.mpi.Request;

public class MST {
	
	static int timeCap = 10;
	
	static class E implements Serializable{ // edges 
		private static final long serialVersionUID = 1L;
		int from, to, weight;
		boolean mst = false;
		
		E(int f, int t, int w){
			from=f;
			to=t;
			weight=w;
		}
		
		public String toString(){
			return from+" -> "+to+" ("+weight+"), MST: "+mst;
		}
	}

	static class Msg implements Serializable{
		private static final long serialVersionUID = 1L;
		int type;
		int leader;
		E edge;
		
		public String toString() {
			StringBuffer output = new StringBuffer();
			output.append(String.format("  toString for Message {%s}\n", this.hashCode()));
			output.append(String.format("    >type: %d\n", type));
			output.append(String.format("    >leader: %d\n", leader));
			output.append(String.format("    >edge: %s", edge.toString()));
			
			return output.toString();
		}
	}
	static int rank, size;
	// message types
	static final int IS_CANDIDATE=0;
	static final int IS_LEADER=1;	
	
	// nb. of processes (i.e nodes of the graph) 
	static final int N = 4;
	// our graph:   
	//         [0]    [1]             [2]    
	//        /  |   /    \          /  |
	//      7   16  2      10      13   9
	//     /     | /          \   /     |
	//  [3]__3__[4]__5__[5]_18_[6]__12__[7] 
	//     \     | \     |    / \        \
	//      1   15  6    17  4   19      14
	//        \  |    \  | /      \        \ 
	//          [8]_13__[9]__8__[10]__11__[11]
	
	
	// our graph:	
	//         [0]    
	//        /   \
	//      16     6
	//     /         \
	//  [1]---- 7 ----[2]---- 20 -----[6]
	//   |			 / |               |
	//	 |		    /  |               |
	//	 |         /   |               |
	//   |        /    |               |
	//   |       /     |               |
	//   1      14     8               17
	//   |     /       |               |
	//   |    /        |               |
	//   |   /         |               |
	//   |  /		   |               |
	//   | /		   |               |
	//	[3]---- 9 ----[4]---- 18 -----[7]
    //     \         /
	//      3       2
	//		  \    /
	//		   [5]
	
	static final E incList[][] = {
		{new E(0,2,6),new E(0,1,16)},  					// 0
		{new E(1,2,7),new E(1,0,16), new E(1,3,1)}, 	// 1
		{new E(2,0,6),new E(2,1,7), new E(2, 3, 14)},  	// 2
		{new E(3,1,1),new E(3,2,14)},  					// 3
		
		/*
		{new E(1,4,2),new E(1,6,10)},  		// 1
		{new E(2,6,13),new E(2,7,9)},  		// 2
		{new E(3,0,7),new E(3,4,3),new E(3,8,1)},  		// 3
		{new E(4,3,3),new E(4,0,16),new E(4,1,2),new E(4,5,5),new E(4,9,6),new E(4,8,15)}, //4
		{new E(5,4,5),new E(5,6,18),new E(5,9,17)},  		// 5
		{new E(6,5,18),new E(6,1,10),new E(6,2,13),new E(6,7,12),new E(6,10,19),new E(6,9,4)}, //6 
		{new E(7,6,12),new E(7,2,9),new E(7,11,14)},  // 7
		{new E(8,3,1),new E(8,4,15),new E(8,9,13)},  // 8
		{new E(9,8,13),new E(9,4,6),new E(9,5,17),new E(9,6,4),new E(9,10,8)}, //9
		{new E(10,9,8),new E(10,6,19),new E(10,11,11)},  // 10
		{new E(11,10,11),new E(11,7,14)},  	// 11
		*/		
	}; 							
	static E[] neighbors;
	static E[] mst;
	
	public static int findLeader() throws Exception {
		int currentLeader = rank;
		boolean isConnected = false;
		E incomingEdge = null;
		E outgoingEdge = null;
		
		// we find our neighbors in incList:
		int deg = neighbors.length;
		System.out.println(rank + ": Has " + deg + " neighbors");
		
		// we print our neighbors for debugging reasons:
		StringBuffer sb = new StringBuffer(rank + ": Neighbours: ");
		String prefix = "  > ";
		for (int j = 0; j < deg; j++) {
			sb.append(prefix);
			sb.append(incList[rank][j].toString());
			prefix = ",";
		}
		System.out.println(sb);
		
		// we need requests for all our neighbors
		Request[] reqs = new Request[deg];
		boolean[] hasGotMsg = new boolean[deg];
		int m = 0; // count for the successful reads
		Msg[] recBuf = new Msg[deg];
		Msg[] sendBuf = new Msg[deg];
		
		System.out.println(rank + ": Getting minimal edge and postulating to neighbours");

		// Determine smallest neighbour and send to other neighbours
		int minimalWeight = Integer.MAX_VALUE;
		int minimalWeightedEdge = Integer.MAX_VALUE;
		for (int nb = 0; nb < deg; nb++) {
			if (neighbors[nb].weight < minimalWeight) {
				minimalWeight = neighbors[nb].weight;
				minimalWeightedEdge = nb;
			}
		}
		System.out.println(rank + ": Found minimal weighted edge: {" + neighbors[minimalWeightedEdge].toString() + "}");
		
		for (int nb = 0; nb < deg; nb++) {
			System.out.println(rank + ": Sending found edge to neighbour {" + neighbors[nb].to + "}" );
			sendBuf[nb] = new Msg();
			sendBuf[nb].leader = rank;
			sendBuf[nb].type = IS_LEADER;
			sendBuf[nb].edge = neighbors[minimalWeightedEdge];
			MPI.COMM_WORLD.Isend(
				sendBuf,
				nb,
				1,
				MPI.OBJECT,
				neighbors[nb].to, // Destination
				0
			);
		} 
		
		// we start to listen to all neighbors
		for (int nb = 0; nb < deg; nb++) {
			System.out.println(rank + ": Listening to neighbour " + neighbors[nb].to);
			reqs[nb] = MPI.COMM_WORLD.Irecv(
				recBuf,
				nb,
				1,
				MPI.OBJECT,
				neighbors[nb].to, // Source 
				MPI.ANY_TAG
			);
		}
		
		do {
			//System.out.println(rank + ": Polling neighbors");
			
			// Poll all neighbors:
			for (int nb = 0; nb < deg; nb++) {				
				if (!hasGotMsg[nb]) {
					//System.out.println(rank + ": Polling neighbour " + neighbors[nb].to);
					if (reqs[nb].Test() != null) {
						System.out.println(rank + ": Got response from neighbour " + neighbors[nb].to);
						System.out.println(rank + ": " + recBuf[nb].toString());
						hasGotMsg[nb] = true;
						m++;
						if (recBuf[nb].edge.to == rank) {
							isConnected = true;
							incomingEdge = recBuf[nb].edge;
						}
					}
				} else {
					System.out.println(rank + ": Got response from neighbour " + neighbors[nb].to + " skipping");
				}
			}
			try {
				Thread.currentThread();
				Thread.sleep(timeCap);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} while (m < deg - 1);

		if (m == deg) {			
			System.out.println(rank + ": Got responses from all neighbors");
			
			// Determine shortest edge from either current rank or connected neighbour
			
			// Get connected neighbour
			ArrayList<Integer> connectedNeighbours = new ArrayList<Integer>();
			for (int nb = 0; nb < deg; nb++) {
				if (hasGotMsg[nb]) {
					if (	recBuf[nb].edge.to == rank ||
							recBuf[nb].edge.from == rank
						) {
						connectedNeighbours.add(nb);
					}
				}
			}
			
			if (connectedNeighbours.size() == 1) {
				
				System.out.println(rank + ": Connected neighbour: " + neighbors[connectedNeighbours.get(0)].toString());
				
				minimalWeight = Integer.MAX_VALUE;
				minimalWeightedEdge = Integer.MAX_VALUE;
				
				for (int nb = 0; nb < deg; nb++) {
					if (neighbors[nb].weight < minimalWeight) {
						minimalWeight = neighbors[nb].weight;
						minimalWeightedEdge = nb;
					}
				}
				
				if (!connectedNeighbours.contains(minimalWeightedEdge)) {
					System.out.println(rank + ": Added new edge to MST: " + neighbors[minimalWeightedEdge].to);
					
					for (int nb = 0; nb < deg; nb++) {
						System.out.println(rank + ": Sending found edge " + neighbors[minimalWeightedEdge].toString() + " to neighbour {" + neighbors[nb].to + "}" );
						sendBuf[nb] = new Msg();
						sendBuf[nb].leader = rank;
						sendBuf[nb].type = IS_LEADER;
						sendBuf[nb].edge = neighbors[minimalWeightedEdge];
						MPI.COMM_WORLD.Isend(
							sendBuf,
							nb,
							1,
							MPI.OBJECT,
							neighbors[nb].to, // Destination
							0
						);
					} 
					
				} else {
					System.out.println(rank + ": No new edges found, found is already connected");
					
					for (int nb = 0; nb < deg; nb++) {
						System.out.println(rank + ": Sending already connected edge " + sendBuf[nb].edge.toString() + " to neighbour {" + neighbors[nb].to + "}" );
						MPI.COMM_WORLD.Isend(
							sendBuf,
							nb,
							1,
							MPI.OBJECT,
							neighbors[nb].to, // Destination
							0
						);
					} 
				}
			} else {
				System.out.println(rank + ": No new edges found");
				for (int nb = 0; nb < deg; nb++) {
					System.out.println(rank + ": Sending already connected edge " + sendBuf[nb].edge.toString() + " to neighbour {" + neighbors[nb].to + "}" );
					MPI.COMM_WORLD.Isend(
						sendBuf,
						nb,
						1,
						MPI.OBJECT,
						neighbors[nb].to, // Destination
						0
					);
				} 
			}
		} else {
			// We have got a message from at least one neighbor
			System.out.println(rank + ": Message from at least one neighbour remaining");
			
			int missingNb=0;
			while(hasGotMsg[missingNb]) missingNb++;
			// now  neighbour[nb] is the neighbour from
			// which we have not got a message
			// so we send our candidate msg to the link 'nb' where we got no msg
			System.out.println(rank + ": Sending message to remaining neighbour " + neighbors[missingNb].to);
			sendBuf[missingNb] = new Msg();
			sendBuf[missingNb].leader = rank;
			sendBuf[missingNb].type = IS_LEADER;
			sendBuf[missingNb].edge = neighbors[minimalWeightedEdge];
			Request rr =MPI.COMM_WORLD.Isend(
					sendBuf,
					missingNb,
					1,
					MPI.OBJECT,
					neighbors[missingNb].to, // Destination
					0
					);
			rr.Wait(); 
			
			do{ 
				if (reqs[missingNb].Test()!=null){
					m = m + 1;
					// Determine shortest edge from either current rank or connected neighbour
					
					// Get connected neighbour
					ArrayList<Integer> connectedNeighbours = new ArrayList<Integer>();
					for (int nb = 0; nb < deg; nb++) {
						if (hasGotMsg[nb]) {
							if (	recBuf[nb].edge.to == rank ||
									recBuf[nb].edge.from == rank
								) {
								connectedNeighbours.add(nb);
							}
						}
					}
					
					if (connectedNeighbours.size() == 1) {
						
						System.out.println(rank + ": Connected neighbour: " + neighbors[connectedNeighbours.get(0)].toString());
						
						minimalWeight = Integer.MAX_VALUE;
						minimalWeightedEdge = Integer.MAX_VALUE;
						
						for (int nb = 0; nb < deg; nb++) {
							System.out.println(rank + ": Testing " + nb);
							if (nb != connectedNeighbours.get(0)) {
								if (neighbors[nb].weight < minimalWeight) {
									minimalWeight = neighbors[nb].weight;
									minimalWeightedEdge = nb;
								}
							}
						}
						System.out.println(rank + ": Minimal " + minimalWeightedEdge);
						
						if (minimalWeightedEdge < connectedNeighbours.get(0)) {
							System.out.println(rank + ": Added new edge to MST: " + neighbors[minimalWeightedEdge].to);
							
							for (int nb = 0; nb < deg; nb++) {
								if (nb != missingNb) {
									System.out.println(rank + ": Sending found edge " + neighbors[minimalWeightedEdge].toString() + " to neighbour {" + neighbors[nb].to + "}" );
									sendBuf[nb] = new Msg();
									sendBuf[nb].leader = rank;
									sendBuf[nb].type = IS_LEADER;
									sendBuf[nb].edge = neighbors[minimalWeightedEdge];
									MPI.COMM_WORLD.Isend(
										sendBuf,
										nb,
										1,
										MPI.OBJECT,
										neighbors[nb].to, // Destination
										0
									);
								}
							} 
							
						} else {
							System.out.println(rank + ": No new edges found, found " +neighbors[minimalWeightedEdge].toString() + " is already connected");
							
							for (int nb = 0; nb < deg; nb++) {
								if (nb != missingNb) {
									System.out.println(rank + ": Sending already connected edge " + sendBuf[nb].edge.toString() + " to neighbour {" + neighbors[nb].to + "}" );
									MPI.COMM_WORLD.Isend(
										sendBuf,
										nb,
										1,
										MPI.OBJECT,
										neighbors[nb].to, // Destination
										0
									);
								}
							} 
						}
					} else {
						System.out.println(rank + ": No new edges found");
						for (int nb = 0; nb < deg; nb++) {
							if (nb != missingNb) {
								System.out.println(rank + ": Sending already connected edge " + sendBuf[nb].edge.toString() + " to neighbour {" + neighbors[nb].to + "}" );
								MPI.COMM_WORLD.Isend(
									sendBuf,
									nb,
									1,
									MPI.OBJECT,
									neighbors[nb].to, // Destination
									0
								);
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
			} while (m<deg);
		}
		return currentLeader;
	}

	
	public static void main(String[] args) throws Exception {
		MPI.Init(args);
		size = MPI.COMM_WORLD.Size();
		rank = MPI.COMM_WORLD.Rank();
		neighbors = incList[rank]; // our neighbors in the tree graph
		
		if (size != N) {
			System.out.println("run with -n "+N);
		}
		else {
			int leader = findLeader();
			System.out.println("******rank "+rank+", leader: "+leader);
		}
		
		MPI.Finalize();
	}
}
 
