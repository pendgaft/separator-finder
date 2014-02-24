package graph;

import java.util.*;


public class Vertex extends AbstractVertex{

	private boolean visited; 
	private boolean inWardenFringe;
	private boolean inOppositeFringe;
	private int blackNeighborsNumber;
	private int oppositeNeighborsNumber;
	private int separatorNeighborNumber;
	private HashSet<Edge> edgeSet;
	private HashSet<Vertex> neighborSet;
	private List<Vertex> availableNeighborList;

	public Vertex(int idValue) {
		super(idValue);
		this.visited = false;
		this.inWardenFringe = false;
		this.inOppositeFringe = false;
		this.blackNeighborsNumber = 0;
		this.oppositeNeighborsNumber = 0;
		this.separatorNeighborNumber = 0;
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

	public int getNeighborNumber() {
		return this.neighborSet.size();
	}
	public void setNeighborSets(int blackNeighborsNumber) {
		this.blackNeighborsNumber = blackNeighborsNumber;
		this.oppositeNeighborsNumber = this.getNeighborNumber() - blackNeighborsNumber;
	}
	public int getNumberOfBlackNeighbors() {
		return this.blackNeighborsNumber;
	}
	public int getNumberOfOppositeNeighbors() {
		return this.oppositeNeighborsNumber;
	}

	public void setNumberOfSeparatorNeighbors(int separatorCnt) {
		this.separatorNeighborNumber = separatorCnt;
	}
	public void increaseNumberOfSeparatorNeighbors() {
		++this.separatorNeighborNumber;
	}
	public void decreaseNumberOfSeparatorNeighbors() {
		--this.separatorNeighborNumber;
	}
	public int getNumberOfSeparatorNeighbors() {
		return this.separatorNeighborNumber;
	}
	
	public boolean isVisited() {
		if (this.visited)
			return true;
		return false;
	}
	public void setVisited() {
		this.visited = true;
	}
	
	public void setInWardenFringe() {
		this.inWardenFringe = true;
	}	
	public void unsetInWardenFringe() {
		this.inWardenFringe = false;
	}
	public boolean isInWardenFringe() {
		return this.inWardenFringe;
	}
	public void setInOppositeFringe() {
		this.inOppositeFringe = true;
	}	
	public void unsetInOppositeFringe() {
		this.inOppositeFringe = false;
	}
	public boolean isInOppositeFringe() {
		return this.inOppositeFringe;
	}
	public void unsetFringesFlag() {
		this.inWardenFringe = false;
		this.inOppositeFringe = false;
	}
}
