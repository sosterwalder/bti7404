

import java.util.ArrayList;
import java.util.List;

public class Vertice {
	private List<Edge> 	mEdges;
	private String 		mName;
	private int 		mId;
	
	public Vertice() {
		mId = Integer.MAX_VALUE;
		mEdges = new ArrayList<Edge>();
		setName("New node");
	}
	
	public Vertice(int id) {
		setId(id);
		mEdges = new ArrayList<Edge>();
		setName("New node");
	}
	
	public Vertice(String name) {
		setId(Integer.MAX_VALUE);
		mEdges = new ArrayList<Edge>();
		setName(name);
	}
	
	public Vertice(int id, String name) {
		setId(id);
		setName(name);
		mEdges = new ArrayList<Edge>();
	}
	
	public Vertice(List<Edge> edges, int id) {
		setId(id);
		setName("New node");
		mEdges = edges;
	}
	
	public Vertice(List<Edge> edges, String name) {
		setId(Integer.MAX_VALUE);
		setName(name);
		mEdges = edges;
	}
	
	public Vertice(List<Edge> edges, int id, String name) {
		setId(id);
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
	
	public int getNeighbourCount() {
		List<Vertice> neighbours = this.getNeighbours();
		
		return neighbours.size();
	}

	public int getId() {
		return mId;
	}

	public void setId(int id) {
		this.mId = id;
	}
}
