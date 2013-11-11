
import java.util.ArrayList;
import java.util.List;

import p2pmpi.mpi.MPI;
import p2pmpi.mpi.Request;

public class TreeLeader {
	static int rank;
	static int size;
	
	static final int TIME_TO_WAIT = 10;
	
	// Number of processes (i.e nodes of the graph) 
	static final int VERTICES = 10;
	
	static int timeCap = 10;
	
	// Vertices
	private List<Vertice> mVertices;
	
	
	public TreeLeader() {
		mVertices = new ArrayList<Vertice>();
		
		for (int i = 0; i < VERTICES; i++) {
			mVertices.add(new Vertice(i, String.format("Vertice %d", i)));
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
		
		// Vertice 1
		mVertices.get(1).addEdge(new Edge(mVertices.get(1), mVertices.get(5), 0));
		
		// Vertice 7
		mVertices.get(7).addEdge(new Edge(mVertices.get(7), mVertices.get(6), 0));
		
		// Vertice 6
		mVertices.get(6).addEdge(new Edge(mVertices.get(6), mVertices.get(5), 0));
		
		// Vertice 9
		mVertices.get(9).addEdge(new Edge(mVertices.get(9), mVertices.get(5), 0));
		
		// Vertice 5
		mVertices.get(5).addEdge(new Edge(mVertices.get(5), mVertices.get(1), 0));
		mVertices.get(5).addEdge(new Edge(mVertices.get(5), mVertices.get(6), 0));
		mVertices.get(5).addEdge(new Edge(mVertices.get(5), mVertices.get(9), 0));
	}  
	
	public int findLeader() {
		Request[] requests = null;
		Message[] messageBuffer = null; 
		int currentLeader = rank;
		
		Vertice currentVertice = mVertices.get(rank);
		int numberOfNeighbours = currentVertice.getNeighbourCount();
		List<Vertice> neighbours = currentVertice.getNeighbours();

		messageBuffer = new Message[numberOfNeighbours + 1];
		requests = new Request[numberOfNeighbours + 1];
		
		messageBuffer[0] = new Message();
		messageBuffer[0].rank = rank;
		
		// Output neighbours
		StringBuffer neighboursList = new StringBuffer(
			String.format(
				"Vertice {id: %d, rank: %d}; Neighbours: ",
				currentVertice.getId(),
				rank
			)
		);
		
		String prefix = "";
		for (int neighbour = 0; neighbour < numberOfNeighbours; neighbour++) {
			neighboursList.append(prefix);
			neighboursList.append(neighbours.get(neighbour).getName());
			prefix = ", ";
			
			messageBuffer[neighbour + 1] = new Message();
			messageBuffer[neighbour + 1].rank = neighbour;
		}
		System.out.println(neighboursList);
		
		// Listen to all neighbors
		for (int neighbour = 0; neighbour < numberOfNeighbours; neighbour++) {
			requests[neighbour] = MPI.COMM_WORLD.Irecv(
				messageBuffer,
				0,
				1,
				MPI.OBJECT,
				neighbours.get(neighbour).getId(), // Source
				0
			);
			
			System.out.println(
				String.format(
					"Vertice {%d} wants to receive from neighbour {%d}",
					rank,
					neighbours.get(neighbour).getId()
				)
			);
		}
		
		// Process requests
		int numberOfProcessedNeighbours = 0;
		do {
			for (int neighbour = 0; neighbour < numberOfNeighbours; neighbour++) {
				Message currentMessage = messageBuffer[neighbour];
				System.out.println(
					String.format(
						"Vertice {%d}: Trying to process message",
						rank
					)
				);
				
				if (!currentMessage.isMessageReceived) {
					if (requests[neighbour].Test() != null) {
						currentMessage = new Message();
						currentMessage.isMessageReceived = true;
						currentMessage.rank = neighbours.get(neighbour).getId();
						numberOfProcessedNeighbours++;
						
						if (currentLeader > currentMessage.leader) {
							currentLeader = currentMessage.leader;
						}
						
						System.out.println(
							String.format(
								"Vertice {%d} received answer from neighbour {%d}",
								rank,
								currentMessage.rank
							)
						);
					}
				} else {
					System.out.println(
						String.format(
							"Vertice {%d} already received answer from neighbour {%d}, skipped",
							rank,
							currentMessage.rank
						)
					);
				}
			}
			
			try {
				Thread.currentThread();
				Thread.sleep(TIME_TO_WAIT);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}			
		} while (numberOfProcessedNeighbours < numberOfNeighbours - 1);
		
		if (numberOfProcessedNeighbours == numberOfNeighbours) {
			// We seem to have a mesage from all our neighbours
			System.out.println(
				String.format(
					"Vertice {%d} has all message from his neighbours",
					rank
				)
			);
			
			// Postulate message to all the neighbours
			for (int neighbour = 0; neighbour < numberOfNeighbours; neighbour++) {
				System.out.println(
					String.format(
						"Vertice {%d}: Postulating message to neighbour {%d}",
						rank,
						neighbours.get(neighbour).getId()
					)
				);
				
				Message finalMessage = new Message();
				finalMessage.messageType = 1;
				finalMessage.leader = currentLeader;
				finalMessage.rank = neighbours.get(neighbour).getId();
				finalMessage.isMessageReceived = false;
				
				MPI.COMM_WORLD.Isend(
					finalMessage,
					0,
					1,
					MPI.OBJECT,
					neighbours.get(neighbour).getId(),  // Destination
					0
				); 
			}
		} else {
			// We do not seem to have a message from all our neighbours
			System.out.println(
				String.format(
					"Vertice {%d} has not yet all messages from his neighbours",
					rank
				)
			);
			
			// Find missing neighbour
			int missingNeighbour = 1;
			
			Message currentMessage = messageBuffer[missingNeighbour];
			System.out.println("Parsing message " + currentMessage.rank);
			
			while (currentMessage.isMessageReceived) {
				System.out.println(
					String.format(
						"Vertice {%d}: Got message from neighbour {%d}, trying next",
						rank,
						neighbours.get(missingNeighbour - 1).getId()
					)
				);
				missingNeighbour++;
				currentMessage = messageBuffer[missingNeighbour];
			}
			System.out.println(
				String.format("Vertice {%d} is missing message from neighbour {id: %d, index: %d} and telling it leader candidate", rank, neighbours.get(missingNeighbour - 1).getId(), missingNeighbour)
			);
			// The neighbour should now be set to the missing one
			
			// Tell the neighbour the current candidate
			Message message = new Message();
			message.leader = currentLeader;
			message.messageType = 0;
			message.rank = neighbours.get(missingNeighbour - 1).getId();
			message.isMessageReceived = false;
			
			Request request = MPI.COMM_WORLD.Isend(
				message,
				0,
				1,
				MPI.OBJECT,
				neighbours.get(missingNeighbour - 1).getId(), // Destination
				0
			);
			request.Wait();
			
			do {
				if (requests[missingNeighbour].Test() != null) {
					numberOfProcessedNeighbours++;
					
					if (currentMessage.messageType == 0) {
						if (currentLeader > currentMessage.leader) {
							currentLeader = currentMessage.leader;
						}
					}
				} else {
					currentLeader = currentMessage.leader;
				}
				
				// Send a leader message to all neighbours
				for (int neighbour = 0; neighbour < numberOfNeighbours; neighbour++) {
					if (neighbour != missingNeighbour) {
						System.out.println(
							String.format(
								"Vertice {%d}: Postulating message to neighbour {%d}",
								rank,
								neighbours.get(neighbour).getId()
							)
						);
						
						Message leaderMessage = new Message();
						leaderMessage.messageType = 1;
						leaderMessage.leader = currentLeader;
						leaderMessage.rank = neighbours.get(neighbour).getId();
						leaderMessage.isMessageReceived = false;
						
						Request leaderRequest = MPI.COMM_WORLD.Isend(
							message,
							0,
							1,
							MPI.OBJECT,
							neighbours.get(neighbour).getId(), // Destination
							0
						);
						leaderRequest.Wait();
					}
				}
				
				try {
					Thread.currentThread();
					Thread.sleep(TIME_TO_WAIT);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			} while (numberOfProcessedNeighbours < numberOfNeighbours - 1);
		}
				
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
			System.out.println(
				String.format(
					"Rank {%d} elected leader {%d}",
					rank,
					leader
				)
			);
		}
		MPI.Finalize();
	}
}
