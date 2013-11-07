import java.io.Serializable;
import java.util.ArrayList;
import java.util.Random;

import com.touchgraph.graphlayout.Edge;

import p2pmpi.mpi.MPI;
import p2pmpi.mpi.Request;
import p2pmpi.mpi.Status;
import p2pmpi.mpi.dev.SendBufferInformation;

public class MST {
	static class E implements Serializable{ // edges 
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
		int type;
		int leader;
		E edge;
	}
	static int rank, size;
	// message types
	static final int IS_CANDIDATE=0;
	static final int IS_LEADER=1;	
	
	// nb. of processes (i.e nodes of the graph) 
	static final int N = 12;
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
	
	
	static final E incList[][] = {
		{new E(0,3,7),new E(0,4,16)},  		// 0
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
		}; 							
	static E[] neighbours;
	
	public static void main(String[] args) {
		MPI.Init(args);
		size = MPI.COMM_WORLD.Size();
		rank = MPI.COMM_WORLD.Rank();
		MPI.Finalize();
	}
}
 
