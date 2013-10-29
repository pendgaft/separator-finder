package graph;

import java.util.*;


public class Vertex extends AbstractVertex{
        
	private HashSet<Edge> edgeSet;
	private HashSet<Vertex> neighborSet;
	private List<Vertex> availableNeighborList;

	public Vertex(int idValue) {
		super(idValue);
		this.edgeSet = new HashSet<Edge>();
		this.neighborSet = new HashSet<Vertex>();
        this.availableNeighborList = new ArrayList<Vertex>();
	}
        
	public Vertex(int idValue, Object supData){
		super(idValue, supData);
		this.edgeSet = new HashSet<Edge>();
	}
        
	public void addNeighbor(Vertex newNeighbor) {
		this.neighborSet.add(newNeighbor);
	}
        
	public HashSet<Vertex> getAllNeighbors() {
		return this.neighborSet;
	}
        
	public List<Vertex> getAvailableNeighbors() {
		return this.availableNeighborList;
	}
	
	public void createAvailableNeighborList() {
		this.availableNeighborList.clear();
		for (Vertex neighbor : this.neighborSet) {
			this.availableNeighborList.add(neighbor);
		}
	}
	
	public void printAvailableNeighborList() {
		for (Vertex node : this.availableNeighborList) {
			System.out.print(node.getVertexID() + ", ");
		}
		System.out.println();
	}
	
	/**
	 * randomly select and return a neighbor from the available neighbors,
	 * if the list is empty, return null
	 * 
	 * @param random
	 * @return
	 */
	public Vertex randomSelectANeighbor(Random random) {
		if (this.availableNeighborList.isEmpty()) {
			return null;
		} else {
			int randomIndex = random.nextInt(this.availableNeighborList.size());
			Vertex nextNode = this.availableNeighborList.get(randomIndex);
			this.availableNeighborList.remove(randomIndex);
			//System.out.println("availableNeighborList size : " + this.availableNeighborList.size());
			return nextNode;
		}
	}
        
	public void addEdge(Edge newEdge){
		this.edgeSet.add(newEdge);
	}
        
	public Collection<Edge> getEdges(){
		return this.edgeSet;
	}
        
	public Collection<Integer> getAdjecentVertexIDs() {
		HashSet<Integer> idSet = new HashSet<Integer>();
                
		for(Edge tEdge: this.edgeSet){
			idSet.add(tEdge.getOtherVertex(this).getVertexID());
		}
		return idSet;
	}
}
