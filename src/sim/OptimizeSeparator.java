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
	/** warden shore contains wardens */
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
		
		//this.removeRedundantComponents();
		this.printResults();
		
		if (this.testResults()) {
			System.out.println("Test Passed before getting optimizing!!!");
		} else {
			System.out.println("Test Failed before getting optimizing!!!");
			return;
		}
		
		long startTime, endTime;
		startTime = System.currentTimeMillis();
		
		this.runOptimization();
		
		endTime = System.currentTimeMillis();
		System.out.println("\nAll running separator optimization took: " + (endTime - startTime) / 1000 + " seconds, "
				+ (endTime - startTime) / 60000 + " minutes.");
		
		
		/*this.printResults();
		System.out.println("\n\ndo remove useless components: ");
		this.removeRedundantComponents();*/
		
		if (this.testResults()) {
			System.out.println("Test Passed!!!");
		} else {
			System.out.println("Test Failed!!!");
		}
		
		this.printResults();
	}

	private void runOptimization() {
		boolean done = false;
		this.createFringeSets();
		
		

		//System.out.println("!!!" + this.wardenShore.size() + ", " + this.oppositeShore.size() + ", " + this.separatorSet.size());

		for (int i = 0; i < this.threshold && !done; ++i) {
			System.out.println("Run: " + (i+1));
			System.out.println("Fringes: warden " + this.wardenFringe.size() + ", opp " + this.oppositeFringe.size() + "\n");
			
			if (Constants.OPT_DEBUG) {
				System.out.println("****warden shore starts: ");
			}
			if (this.optimizeWardenShore()) {
				done = true;
			}
			
			System.out.println("Fringes after warden swap: warden " + this.wardenFringe.size() + ", opp " + this.oppositeFringe.size() + "\n");
			
			/*if (!this.testResults()) {
				System.out.println("Test Failed during swapping warden nodes!!! in Run " + (i+1) + ".");
				//return;
			} else {
				System.out.println("Test Passed in swap one wardne node!!!");
			}*/
			
			
			if (Constants.OPT_DEBUG) {
				System.out.println("****opposite shore starts: ");
			}
			if (!done && this.optimizeOppositeShore()) {
				done = true;
			}
			
			System.out.println("Fringes after opp swap: warden " + this.wardenFringe.size() + ", opp " + this.oppositeFringe.size() + "\n");
			
			
			
			
			
			if (Constants.OPT_DEBUG) {
				System.out.println("****");
			}
			
			/*if (!this.testResults()) {
				System.out.println("Test Failed during swapping opp nodes!!! in Run " + (i+1) + ".\n\n");
				//return;
			} else {
				System.out.println("Test Passed in swapping one opp node!!!");
			}
			if (done) {
				System.out.println("finish!!!");
			}*/
		}
	}
	
	
	/**
	 * @return true if cannot find a candidate to swap into separator.
	 */
	private boolean optimizeWardenShore() {
		if (this.wardenFringe.isEmpty()) {
			System.out.println("Stops at warden fringe is empty.......");
			return true;
		}
		
		Vertex currentNode = this.wardenFringe.poll();
		/* when it only has one warden neighbor, it is not worth to swap anymore */
		if (currentNode.getNumberOfSeparatorNeighbors() == 1) {
			System.out.println("Stops at warden shore is not worth of swapping...........");
			this.wardenFringe.add(currentNode);
			return true;
		}

		if (this.wardenSet.contains(currentNode)) {
			return false;
		}
		this.separatorSet.add(currentNode);
		int part = 0;
		for (Vertex neighbor: currentNode.getAllNeighbors()) {
			
			if (this.separatorSet.contains(neighbor)) {
				/* only put the previous separators which are adjacent to one warden part nodes
				 * into the opposite fringe set, others remain separators */
				if (this.getWardenNeighborNumber(neighbor) == 0) {
					this.putIntoOppositeFringeWithoutConditions(neighbor);
					this.separatorSet.remove(neighbor);
				}
				part = 1;
			} else if (this.wardenShore.contains(neighbor)) {
				
				/* wardens cannot be swapped to separators.. */
				if (!this.wardenSet.contains(neighbor)) {
					this.wardenShore.remove(neighbor);
					this.putIntoWardenFringeWithoutConditions(neighbor);
				}
				part = 2;
			} else if (this.wardenFringe.contains(neighbor)) {
				/* need to update its warden neighbor.. too costly in this way??	 */
				this.wardenFringe.remove(neighbor);
				this.putIntoWardenFringeWithoutConditions(neighbor);
				part = 3;
			} else {
				part = 4;
				if (this.oppositeFringe.contains(neighbor))
					System.out.println("opposite fringe contain it!");
				else if (this.oppositeShore.contains(neighbor))
					System.out.println("opposite shore contain it!");
				else 
					System.out.println("what else can contain..");
				System.out.println("Not possible.. Something's wrong in opp swapping!!");
				//return true;
			}
			if (!this.testResults()) {
				System.out.println("test case fails in warden shore.. part " + part);
			}
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
		if (this.oppositeFringe.isEmpty()) {
			System.out.println("Stops at oppoiste fringe empty.............");
			return true;
		}
		
		Vertex currentNode = this.oppositeFringe.poll();
		if (currentNode.getNumberOfSeparatorNeighbors() == 1) {
			System.out.println("Stops at opposite shore not worth of swapping.........");
			return true;
		}
		int part = 0;
		this.separatorSet.add(currentNode);
		for (Vertex neighbor: currentNode.getAllNeighbors()) {
			
			if (this.separatorSet.contains(neighbor)) {
				//this.putIntoOppositeFringeWithoutConditions(neighbor);
				if (this.getOppositeNeighborNumber(neighbor) == 0) {
					this.putIntoWardenFringeWithoutConditions(neighbor);
					this.separatorSet.remove(neighbor);
				}
				part = 1;
			} else if (this.oppositeShore.contains(neighbor)) {
				this.oppositeShore.remove(neighbor);
				this.putIntoOppositeFringeWithoutConditions(neighbor);
				part = 2;
			} else if (this.oppositeFringe.contains(neighbor)) {
				this.oppositeFringe.remove(neighbor);
				this.putIntoOppositeFringeWithoutConditions(neighbor);
				part = 3;
			} else {
				part = 4;
				if (this.wardenFringe.contains(neighbor))
					System.out.println("wardne fringe contain it!");
				else if (this.wardenShore.contains(neighbor))
					System.out.println("wardne shore contain it!");
				else 
					System.out.println("what else can contain..");
				System.out.println("Not possible.. Something's wrong in opp swapping!!");
				//return true;
			}
			if (!this.testResults()) {
				System.out.println("test case fails in opposite shore.. part " + part);
			}
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
			this.putIntoWardenFringe(node);
		}
		for (Vertex node: tempOppositeShore) {
			this.putIntoOppositeFringe(node);
		}
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
	private int getWardenNeighborNumber(Vertex node) {
		int wardenCnt = 0;
		for (Vertex neighbor: node.getAllNeighbors()) {
			if (this.wardenFringe.contains(neighbor) || this.wardenShore.contains(neighbor)
					|| this.wardenSet.contains(neighbor)) {
				++wardenCnt;
			}
		}
		return wardenCnt;
	}
	
	private int getOppositeNeighborNumber(Vertex node) {
		int oppositeCnt = 0;
		for (Vertex neighbor: node.getAllNeighbors()) {
			if (this.oppositeFringe.contains(neighbor) || this.oppositeShore.contains(neighbor)) {
				++oppositeCnt;
			}
		}
		return oppositeCnt;
	}
	
	/**
	 * count the number of separator neighbors which are only adjacent
	 * to one warden node (node in the warden shore or fringe).
	 * @param node
	 * @return
	 */
	private int updateSeparatorCntForWarden_OneToMany(Vertex node) {
		int separatorCnt = 0;
		for (Vertex neighbor: node.getAllNeighbors()) {
			if (this.separatorSet.contains(neighbor) && this.getWardenNeighborNumber(neighbor) == 1) {
				++separatorCnt;
			}
		}
		node.setNumberOfSeparatorNeighbors(separatorCnt);
		return separatorCnt;
	}
	
	private int updateSeparatorCntForOpposite_OneToMany(Vertex node) {
		int separatorCnt = 0;
		for (Vertex neighbor: node.getAllNeighbors()) {
			if (this.separatorSet.contains(neighbor) && this.getOppositeNeighborNumber(neighbor) == 1) {
				++separatorCnt;
			}
		}
		node.setNumberOfSeparatorNeighbors(separatorCnt);
		return separatorCnt;
	}
	
	/**
	 * count the number of separators that have only one adjacent warden shore node
	 * of a given node, if the number is greater then 2, or say worth swapping,
	 * put it into warden fringe which is the priority queue.
	 * @param node
	 */
	private boolean putIntoWardenFringe(Vertex node) {
		int separatorCnt = this.updateSeparatorCntForWarden_OneToMany(node);
		if (separatorCnt >= 2) {
			this.wardenFringe.add(node);
			this.wardenShore.remove(node);
			return true;
		}
		return false;
	}
	
	private void putIntoWardenFringeWithoutConditions(Vertex node) {
		this.wardenFringe.add(node);
		this.updateSeparatorCntForWarden_OneToMany(node);
		//this.wardenFringe.add(node);
	}
	
	/**
	 * given a node, count the number of valid separators (separators 
	 * must have one adjacent opposite node) they have,
	 * if it is greater than 2, take it from the shore into the fringe.
	 * @param node
	 * @return true if it is literally put into opposite fringe
	 */
	private boolean putIntoOppositeFringe(Vertex node) {
		int separatorCnt = this.updateSeparatorCntForOpposite_OneToMany(node);
		if (separatorCnt >= 2) {
			this.oppositeFringe.add(node);
			this.oppositeShore.remove(node);
			return true;
		}
		return false;
	}
	
	private void putIntoOppositeFringeWithoutConditions(Vertex node) {
		this.oppositeFringe.add(node);
		this.updateSeparatorCntForOpposite_OneToMany(node);
		//this.oppositeFringe.add(node);
		//this.oppositeShore.remove(node); // not na
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
		
		int cnt = 0;
		for (Vertex warden : this.wardenSet) {
			if (visitedSet.contains(warden))
				continue;
			testQueue.add(warden);
			visitedSet.add(warden);
		
			while (!testQueue.isEmpty()) {
				Vertex currentNode = testQueue.poll();
	
				for (Vertex nextNode : currentNode.getAllNeighbors()) {
					if (visitedSet.contains(nextNode))
						continue;
	
					if (this.separatorSet.contains(nextNode)) {
						visitedSet.add(nextNode);
					} else if (this.wardenShore.contains(nextNode) || this.wardenFringe.contains(nextNode)) {
						/* both shore and fringe are part of wardens */
						testQueue.add(nextNode);
						visitedSet.add(nextNode);
					} else {
						//System.out.println("Test Failed!!!");
						//return false;
						visitedSet.add(nextNode);
						++cnt;
						//System.out.println("**illegel node: " + currentNode.getVertexID() + " to " + nextNode.getVertexID());
					}
				}
			}
		}
		if (cnt != 0) {
			//System.out.println(cnt + " illegel neighbors.. Test Failed!");
			return false;
		}

		if (Constants.OPT_DEBUG) {
			//System.out.println("Pass the test in Optimizing!");
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
		System.out.println("warden size : " + (this.wardenShore.size()+this.wardenFringe.size()));
		System.out.println("opposite size : " + this.oppositeShore.size());
		System.out.println("separator size : " + this.separatorSet.size());
		System.out.println("sum size : " + (this.wardenShore.size()+this.oppositeShore.size()+this.separatorSet.size()) + "\n");
	}
}
