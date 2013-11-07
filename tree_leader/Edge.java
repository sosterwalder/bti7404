

public class Edge {
	private Vertice	mPointA;
	private Vertice mPointB;
	private int		mWeight;
	
	public Edge() {
		setPointA(null);
		setPointB(null);
		setWeight(0);
		
	}
	
	public Edge(Vertice pointA, Vertice pointB, int weight) {
		setPointA(pointA);
		setPointB(pointB);
		setWeight(weight);
	}

	public Vertice getPointA() {
		return mPointA;
	}

	public void setPointA(Vertice pointA) {
		this.mPointA = pointA;
	}

	public Vertice getPointB() {
		return mPointB;
	}

	public void setPointB(Vertice pointB) {
		this.mPointB = pointB;
	}

	public int getWeight() {
		return mWeight;
	}

	public void setWeight(int weight) {
		this.mWeight = weight;
	}
}
