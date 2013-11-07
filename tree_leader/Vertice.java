

import java.util.ArrayList;
import java.util.List;

public class Vertice {
	private List<Edge> 	mEdges;
	private String 		mName;
	
	public Vertice() {
		mEdges = new ArrayList<Edge>();
		setName("New node");
	}
	
	public Vertice(String name) {
		mEdges = new ArrayList<Edge>();
		setName(name);
	}
	
	public Vertice(List<Edge> edges, String name) {
		mEdges = edges;
		setName(name);
	}

	public String getName() {
		return mName;
	}

	public void setName(String name_) {
		mName = name_;
	}
	
	public void addEdge(Edge edge) {
		mEdges.add(edge);
	}
	
	public Edge getEdgeByIndex(int index) {
		return mEdges.get(index);
	}
	
	public List<Vertice> getNeighbours() {
		List<Vertice> neighbours = new ArrayList<Vertice>();
		
		for (Edge edge : mEdges) {
			neighbours.add(edge.getPointB());
		}
		
		return neighbours;
	}
}
