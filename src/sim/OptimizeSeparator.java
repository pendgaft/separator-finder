package sim;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

import comparator.LargeSeparatorNeighborsCom;

import graph.Vertex;

public class OptimizeSeparator {

	private int threshold;
	private Set<Vertex> separatorSet;
	private Set<Vertex> wardenSet;
	private Set<Vertex> wardenShore;
	private Set<Vertex> oppositeShore;
	
	private PriorityQueue<Vertex> wardenFringe;
	private PriorityQueue<Vertex> oppositeFringe;

	public OptimizeSeparator(Set<Vertex> separators, Set<Vertex> wardenShore,
			Set<Vertex> oppositeShore, Set<Vertex> wardens, int threshold) {
		this.separatorSet = separators;
		this.wardenSet = wardens;
		this.wardenShore = wardenShore;
		this.oppositeShore = oppositeShore;
		this.threshold = threshold;
		
		/* to be initialized */
		this.wardenFringe = new PriorityQueue<Vertex>(1, new LargeSeparatorNeighborsCom());
		this.oppositeFringe = new PriorityQueue<Vertex>(1, new LargeSeparatorNeighborsCom());
	}
	
	public void simulate() {
		
		if (Constants.OPT_DEBUG) {
			/*System.out.println("in opt/nseparators: ");
			for (Vertex node: this.separatorSet)
				System.out.print(node.getVertexID() + ", ");
			System.out.println("\nwarden shore: ");
			for (Vertex node: this.wardenShore)
				System.out.print(node.getVertexID() + ", ");
			System.out.println("\nopposite shore: ");
			for (Vertex node: this.oppositeShore)
				System.out.print(node.getVertexID() + ", ");
			System.out.println();*/
			/*if (this.separatorSet.size()+this.wardenShore.size()+this.oppositeShore.size() != 16) {
				System.out.println("shore error!!");
				return;
			}*/
		}
		//this.removeRedundantComponents();
		this.printResults();
		
		if (this.testResults()) {
			System.out.println("Test Passed!!!");
		} else {
			System.out.println("Test Failed!!!");
			return;
		}
		
		this.runOptimization();
		
		
		/*this.printResults();
		System.out.println("\n\ndo remove useless components: ");
		this.removeRedundantComponents();*/
		
		if (this.testResults()) {
			System.out.println("Test Passed!!!");
		}
		
		this.printResults();
	}

	private void runOptimization() {
		boolean done = false;
		this.createFringeSets();
		
		System.out.println("!!!" + this.wardenFringe.size() + ", " + this.oppositeFringe.size());
		//if (true)
		//	return;

		System.out.println("!!!" + this.wardenShore.size() + ", " + this.oppositeShore.size() + ", " + this.separatorSet.size());
		for (int i = 0; i < this.threshold && !done; ++i) {
			System.out.println("run: " + i);
			if (Constants.OPT_DEBUG) {
				System.out.println("****warden shore starts: ");
			}
			if (this.optimizeWardenShore()) {
				done = true;
			}
			
			if (Constants.OPT_DEBUG) {
				System.out.println("****opposite shore starts: ");
			}
			if (!done && this.optimizeOppositeShore()) {
				done = true;
			}
			
			if (Constants.OPT_DEBUG) {
				System.out.println("****");
			}
		}
	}
	
	/**
	 * @return true if cannot find a candidate to swap into separator.
	 */
	private boolean optimizeWardenShore() {
		if (this.wardenFringe.isEmpty()) 
			return true;
		
		Vertex currentNode = this.wardenFringe.poll();
		/* when it only has one warden neighbor, it is not worth to swap anymore */
		if (currentNode.getNumberOfSeparatorNeighbors() == 1) {
			this.wardenShore.add(currentNode);
			return true;
		}

		this.separatorSet.add(currentNode);
		System.out.println("cur node: " + currentNode.getVertexID() + ", " + currentNode.getNumberOfSeparatorNeighbors());
		
		int cntN = 0;
		System.out.println("total neighbor amount: " + currentNode.getNeighborNumber());
		//if (true)
		//	return false;
		for (Vertex neighbor: currentNode.getAllNeighbors()) {
			//System.out.println("neighbor id : " + neighbor.getVertexID());
			System.out.println("current warden neighbor Cnt: " + (++cntN));
			/*System.out.println("warden shore again..");
			for (Vertex v: this.wardenShore) {
				System.out.print(v.getVertexID() + ", " + v.getNumberOfSeparatorNeighbors() + "  ");
			}*/
			if (this.wardenShore.contains(neighbor)) {
				if (Constants.OPT_DEBUG) {
					System.out.println("warden to push: " + neighbor.getVertexID());
				}

				/* nodes in the warden fringe cannot be a warden */
				if (!this.wardenSet.contains(neighbor)) {
					this.putIntoWardenFringe(neighbor);
				}
			} else if (this.separatorSet.contains(neighbor)) {
				if (Constants.OPT_DEBUG) {
					System.out.println("opposite to push: " + neighbor.getVertexID());
				}

				/*if (!this.isAdjacentToWardens(neighbor)) {
					this.separatorSet.remove(neighbor);
					this.putIntoOppositeFringe(neighbor);
				}*/
				/* since the separator neighbor of the current node can only have
				 * one warden neighbor, so just push it into the opposite fringe */
				this.separatorSet.remove(neighbor);
				this.putIntoOppositeFringe(neighbor);
			} else {
				/* the node is also in the warden fringe, 
				 * just update its separator number .. */
				this.updateSeparatorNeighborNumber(neighbor);
			}
			System.out.println();
		}
		return false;
	}
	
	/**
	 * check if the given separator is adjacent to more than one opposite nodes
	 * @param separator a separator node.
	 * 
	 * because the node that triggers this call is marked as separator,
	 * then just check if there is any more warden shore nodes.
	 * @return	true if it is adjacent to more than one warden nodes
	 * 			false otherwise
	 */
	private boolean isAdjacentToMultiOpposite(Vertex separator) {
		for (Vertex neighbor : separator.getAllNeighbors()) {
			if (this.wardenFringe.contains(neighbor)) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * check if a given node is adjacent to a warden
	 * @param node
	 * @return 	true if it is adjacent to one
	 * 			false if it does not have any wardens adjacent
	 */
	private boolean isAdjacentToWardens(Vertex node) {
		for (Vertex neighbor : node.getAllNeighbors()) 
			if (this.wardenSet.contains(neighbor))
				return true;
		return false;
	}
	
	/**
	 * 
	 * @return true if cannot find a candidate to swap into separator.
	 */
	private boolean optimizeOppositeShore() {
		if (this.oppositeFringe.isEmpty())
			return true;
		
		Vertex currentNode = this.oppositeFringe.poll();
		if (currentNode.getNumberOfSeparatorNeighbors() == 1) {
			this.oppositeShore.add(currentNode);
			return true;
		}
		this.separatorSet.add(currentNode);
		int cntN = 0;
		//System.out.println("cur node: " + currentNode.getVertexID() + ", " + currentNode.getNumberOfSeparatorNeighbors());
		for (Vertex neighbor: currentNode.getAllNeighbors()) {
			System.out.println("current warden neighbor Cnt: " + (++cntN));
			//System.out.println("neighbor id : " + neighbor.getVertexID());
			/*System.out.println("opposite shore again..");
			for (Vertex v: this.oppositeShore) {
				System.out.print(v.getVertexID() + ", " + v.getNumberOfSeparatorNeighbors() + "  ");
			}*/
			
			
			if (this.oppositeShore.contains(neighbor)) {
				System.out.println("opposite to push: " + neighbor.getVertexID());
				//this.putIntoAFringe(neighbor, false);
				this.putIntoOppositeFringe(neighbor);
			} else if (this.separatorSet.contains(neighbor)) {
				System.out.println("warden to push: " + neighbor.getVertexID());
				this.separatorSet.remove(neighbor);
				//this.putIntoAFringe(neighbor, true);
				this.putIntoWardenFringe(neighbor);
			} else {
				/* neighbors are also in the opposite fringe, 
				 * just update its separator number .. */
				this.updateSeparatorNeighborNumber(neighbor);
			}
			System.out.println();
		}
		return false;
	}
	
	/**
	 * create two sets of nodes that are warden and opposite sides
	 * respectively which are adjacent to at least one separators.
	 * put these nodes into the priority queue.
	 */
	private void createFringeSets() {
		
		Set<Vertex> tempWardenShore = new HashSet<Vertex>(this.wardenShore);
		Set<Vertex> tempOppositeShore = new HashSet<Vertex>(this.oppositeShore);
		for (Vertex node: tempWardenShore) {
			//System.out.println("node in warden shore to be put into fringe: " + node.getVertexID());
			//this.putIntoAFringe(node, true);
			this.putIntoWardenFringe(node);
		}
		System.out.println("warden shore cnt: " + this.cntW);
		for (Vertex node: tempOppositeShore) {
			//System.out.println("node in opposite shore to be put into fringe: " + node.getVertexID());
			//this.putIntoAFringe(node, false);
			this.putIntoOppositeFringe(node);
		}
		System.out.println("opposite shore cnt: " + this.cntO);
		System.out.println("sets created!!");
	}
	
	private int updateSeparatorNeighborNumber(Vertex node) {
		int separatorCnt = 0;
		for (Vertex neighbor: node.getAllNeighbors()) {
			if (this.separatorSet.contains(neighbor) && !this.isAdjacentToWardens(neighbor)) {
				//if (this.separatorSet.contains(neighbor)) {//???
				++separatorCnt;
			}
		}
		node.setNumberOfSeparatorNeighbors(separatorCnt);
		return separatorCnt;
	}
	
	/**
	 * count and return the number of nodes that are a part of warden side
	 * of a given node
	 * @param node
	 * @return
	 */
	private int getWardenNeighbor(Vertex node) {
		int cnt = 0;
		for (Vertex neighbor: node.getAllNeighbors()) {
			if (this.wardenFringe.contains(neighbor) || this.wardenShore.contains(neighbor)
					|| this.wardenSet.contains(neighbor)) {
				++cnt;
			}
		}
		return cnt;
	}
	
	/**
	 * count the number of separator neighbors which are only adjacent
	 * to one warden node (node in the warden shore or fringe).
	 * @param node
	 * @return
	 */
	private int updateSeparatorCnt_OneToMany(Vertex node) {
		int separatorCnt = 0;
		for (Vertex neighbor: node.getAllNeighbors()) {
			if (this.separatorSet.contains(neighbor) && this.getWardenNeighbor(neighbor) == 1) {
				++separatorCnt;
			}
		}
		node.setNumberOfSeparatorNeighbors(separatorCnt);
		return separatorCnt;
	}
	
	/**
	 * update the number of neighbor separators of the given node,
	 * take it out of warden shore and put into warden fringe. 
	 * @param node
	 */
	private void putIntoWardenFringe(Vertex node) {
		
		/* only count the separators that have one adjacent warden side node */
		int separatorCnt = updateSeparatorCnt_OneToMany(node);
		/* if the node is not adjacent to any separator, stays where it was */
		if (separatorCnt <= 1) {
			return;
		}
		++this.cntW;
		//System.out.println("%node to be in warden fringe: " + node.getVertexID());
		if (this.wardenFringe.contains(node)) { /* works??? or use hashMap */
			this.wardenFringe.remove(node);
		}
		this.wardenFringe.add(node);
		this.wardenShore.remove(node);
			
		for (Vertex neighbor: node.getAllNeighbors()) {
			this.updateSeparatorCnt_OneToMany(neighbor);
		}
	}
	
	public int cntW = 0;
	public int cntO = 0;
	
	/**
	 * update the number of neighbor separators of the given node,
	 * take it out of opposite shore and put into opposite fringe. 
	 * @param node
	 */
	private void putIntoOppositeFringe(Vertex node) {
		int separatorCnt = this.updateSeparatorCnt_OneToMany(node);
		/* if the node is not adjacent to any separator, stays where it was */
		if (separatorCnt <= 1) {
			return;
		}
		++this.cntO;
		//System.out.println("%node to be in opposite fringe: " + node.getVertexID());
		if (this.oppositeFringe.contains(node)) {
			this.oppositeFringe.remove(node);	
		}
		this.oppositeFringe.add(node);
		this.oppositeShore.remove(node);
		
		for (Vertex neighbor: node.getAllNeighbors()) {
			this.updateSeparatorCnt_OneToMany(neighbor);
		}
	}
	
	/**
	 * remove the connected component of warden shore that do not
	 * contains any wardens, and remove the separators that are
	 * adjacent to them and if they are not adjacent to other warden
	 * shores?  
	 */
	private void removeRedundantComponents() {
		this.wardenShore.addAll(this.wardenFringe);
		this.oppositeShore.addAll(this.oppositeFringe);
		
		System.out.println("***in removing!!!");
		this.printResults();
		
		boolean containsWarden;
		Queue<Vertex> que = new LinkedList<Vertex>();
		Set<Vertex> tempSet = new HashSet<Vertex>();
		
		for (Vertex node : this.wardenShore) {
			if (!node.isVisited()) {
				/* reset helper variables */
				containsWarden = false;
				tempSet.clear();
				
				node.setVisited();
				que.add(node);
				tempSet.add(node);
				while (!que.isEmpty()) {
					Vertex currentNode = que.poll();
					//System.out.println("removing cur: " + currentNode.getVertexID());
					if (this.wardenSet.contains(currentNode))
						containsWarden = true;
					for (Vertex neighbor : currentNode.getAllNeighbors()) {
						if (!neighbor.isVisited()) {
							neighbor.setVisited();
							que.add(neighbor);
							tempSet.add(neighbor);
						}
					}
				}
				/* if the component does not contain any warden, 
				 * remove it from the warden shore. */
				if (!containsWarden) {
					this.wardenShore.removeAll(tempSet);
					this.oppositeShore.addAll(tempSet);
				}
			}
		}
	}
	
	private boolean testResults() {
		Set<Vertex> visitedSet = new HashSet<Vertex>();
		Queue<Vertex> testQueue = new LinkedList<Vertex>();
		testQueue.add((Vertex) this.wardenSet.toArray()[0]);
		visitedSet.add((Vertex) this.wardenSet.toArray()[0]);
		
		while (!testQueue.isEmpty()) {
			Vertex currentNode = testQueue.poll();

			for (Vertex nextNode : currentNode.getAllNeighbors()) {
				if (visitedSet.contains(nextNode))
					continue;

				if (this.separatorSet.contains(nextNode)) {
					visitedSet.add(nextNode);
				} else if (this.wardenShore.contains(nextNode)) {
					testQueue.add(nextNode);
					visitedSet.add(nextNode);
				} else {
					System.out.println("Test Failed!!!");
					return false;
				}
			}
		}

		if (Constants.OPT_DEBUG) {
			System.out.println("Pass the test!");
		}
		return true;
	}
	
	private void printResults() {
		
		System.out.println("Results:: ");
		/*for (Vertex node : this.separatorSet)
			System.out.print(node.getVertexID() + ", ");
		System.out.println("\nwarden shore: ");
		for (Vertex node : this.wardenShore)
			System.out.print(node.getVertexID() + ", ");
		System.out.println("\nopposite shore: ");
		for (Vertex node : this.oppositeShore)
			System.out.print(node.getVertexID() + ", ");
		System.out.println();*/
		/*if (this.wardenShore.size()+this.oppositeShore.size()+this.separatorSet.size() != 16)
			System.out.println("size error!!!!!!!");*/
		System.out.println("warden size : " + this.wardenShore.size());
		System.out.println("opposite size : " + this.oppositeShore.size());
		System.out.println("separator size : " + this.separatorSet.size());
		System.out.println("sum size : " + (this.wardenShore.size()+this.oppositeShore.size()+this.separatorSet.size()) + "\n");
	}
}
