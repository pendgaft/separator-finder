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
	/** fixed fringes store the nodes only have one connection to the separator set */
	private Set<Vertex> fixedWardenFringe;
	private Set<Vertex> fixedOppositeFringe;

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
		this.fixedWardenFringe = new HashSet<Vertex>();
		this.fixedOppositeFringe = new HashSet<Vertex>();
	}
	
	public void simulate() {
		
		this.printStatistics();
		
		if (Constants.OPT_DEBUG) {
			if (this.testResults()) {
				System.out.println("Test Passed Before Getting Optimizing.");
			} else {
				System.out.println("Test Failed Before Getting Optimizing.");
				return;
			}
		}
		
		this.runOptimization();
		
	
		if (this.testResults()) {
			System.out.println("The Final Result Passed the Test.\n");
		} else {
			System.out.println("The Final Result Failed to Pass the Test...");
		}
		
		this.printStatistics();
	}

	private void runOptimization() {
		boolean done = false;
		this.createFringeSets();
		
		if (Constants.OPT_DEBUG) {
			if (!this.testResults()) {
				System.out.println("Test Failed After Fringes are Created.");
				return;
			} else {
				System.out.println("Test Passed After Fringes are Created.\n");
			}
		}

		System.out.println("Running Optimization ...");
		for (int i = 0; i < this.threshold && !done; ++i) {			
			
			if (Constants.OPT_DEBUG) {
				System.out.println("Run: " + (i+1));
				System.out.println("Warden Fringe " + this.wardenFringe.size() + ", Opposite Fringe " 
						+ this.oppositeFringe.size() + ", Separators " + this.separatorSet.size() + ".");
				
				System.out.println("Warden Shore Starts .. ");
			}
			
			/* Optimize warden shore. */
			if (this.optimizeWardenShore()) {
				done = true;
			}
			
			if (Constants.OPT_DEBUG) {
				if (!this.testResults()) {
					System.out.println("Test Failed during swapping warden nodes in Run " + (i+1) + ".");
					return;
				}				
				System.out.println("Opposite Shore Starts .. ");
			}			
			
			/* Optimize opposite shore. */
			if (!done && this.optimizeOppositeShore()) {
				done = true;
			}
			
			if (Constants.OPT_DEBUG) {
				if (!this.testResults()) {
					System.out.println("Test Failed during swapping opp nodes. in Run " + (i+1) + ".\n\n");
					return;
				}
			}			
			
			if (Constants.OPT_DEBUG) {
				System.out.println("********************************************************");
			}
		}
	}
	
	
	/**
	 * When a node in the warden shore in pushed into the separator set, 
	 * all its neighbors have to be modified accordingly.
	 * 
	 * @return true if cannot find a candidate to swap into separator.
	 */
	private boolean optimizeWardenShore() {
		if (this.wardenFringe.isEmpty()) {
			System.out.println("Stops because the warden fringe is empty.......");
			return true;
		}
		
		Vertex currentNode = this.wardenFringe.poll();
		/* when it only has one separator neighbor, it is not worth to swap anymore */
		if (currentNode.getNumberOfSeparatorNeighbors() == 1) {
			System.out.println("Stops because the warden shore is not worth of swapping...........");
			this.wardenFringe.add(currentNode);
			return true;
		}

		/* wardens cannot be swapped to a separator */
		if (this.wardenSet.contains(currentNode)) {
			return false;
		}
		
		this.separatorSet.add(currentNode);		
		for (Vertex neighbor: currentNode.getAllNeighbors()) {
			
			if (this.separatorSet.contains(neighbor)) {
				/* Only put the previous separators which are adjacent to one warden part nodes
				 * into the opposite fringe set, others remain separators */
				if (this.getWardenNeighborNumber(neighbor) == 0) {
					this.separatorSet.remove(neighbor);
					this.putIntoOppositeFringeAndUpdateFringe(neighbor);
				}
			} else if (this.wardenShore.contains(neighbor)) {
				
				/* Wardens cannot be swapped to separators.. */
				if (!this.wardenSet.contains(neighbor)) {
					this.wardenShore.remove(neighbor);
					this.putIntoWardenFringeWithoutConditions(neighbor);
				}
				
			} else if (this.wardenFringe.contains(neighbor)) {
				this.wardenFringe.remove(neighbor);
				this.putIntoWardenFringeWithoutConditions(neighbor);

			} else if (this.fixedWardenFringe.contains(neighbor)) {
				this.fixedWardenFringe.remove(neighbor);
				this.updateSeparatorNeighborNumber(neighbor);
				this.wardenFringe.add(neighbor);

			} else {
				System.out.println("Not possible.. Something's wrong in warden swapping!!");
				return true;
			}
		}		
		
		return false;
	}
	
	/**
	 * When a node in the opposite shore in pushed into the separator set, 
	 * all its neighbors have to be modified accordingly.
	 * 
	 * @return true if cannot find a candidate to swap into separator.
	 */
	private boolean optimizeOppositeShore() {
		if (this.oppositeFringe.isEmpty()) {
			System.out.println("Stops because the oppoiste fringe is empty.............");
			return true;
		}
		
		Vertex currentNode = this.oppositeFringe.poll();
		if (currentNode.getNumberOfSeparatorNeighbors() == 1) {
			this.oppositeFringe.add(currentNode);
			System.out.println("Stops because the opposite shore is not worth of swapping.........");
			return true;
		}
		
		this.separatorSet.add(currentNode);
		for (Vertex neighbor: currentNode.getAllNeighbors()) {
			
			if (this.separatorSet.contains(neighbor)) {
				if (this.getOppositeNeighborNumber(neighbor) == 0) {
					this.separatorSet.remove(neighbor);
					this.putIntoWardenFringeAndUpdateFringe(neighbor);
				}
			} else if (this.oppositeShore.contains(neighbor)) {
				this.oppositeShore.remove(neighbor);
				this.putIntoOppositeFringeWithoutConditions(neighbor);
				
			} else if (this.oppositeFringe.contains(neighbor)) {
				this.oppositeFringe.remove(neighbor);
				this.putIntoOppositeFringeWithoutConditions(neighbor);
				
			} else if (this.fixedOppositeFringe.contains(neighbor)) {
				this.fixedOppositeFringe.remove(neighbor);
				this.updateSeparatorNeighborNumber(neighbor);
				this.oppositeFringe.add(neighbor);
				
			} else {
				System.out.println("Not possible.. Something's wrong in opposite swapping!!");
				return true;
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
		
		System.out.println("Creating Warden Fringe ...");
		Set<Vertex> tempWardenShore = new HashSet<Vertex>(this.wardenShore);
		Set<Vertex> tempOppositeShore = new HashSet<Vertex>(this.oppositeShore);
		for (Vertex node: tempWardenShore) {
			this.initializeWardenFringe(node);
		}
		
		System.out.println("Creating Opposite Fringe ...");
		for (Vertex node: tempOppositeShore) {
			this.initializeOppoisteFringe(node);
		}
	}
	
	/**
	 * Only count the separators have one connection to shores.
	 * @param node
	 * @return
	 */
	private int updateSeparatorNeighborNumber(Vertex node) {
		int separatorCnt = 0;
		for (Vertex neighbor: node.getAllNeighbors()) {
			if (this.separatorSet.contains(neighbor) && !this.isAdjacentToWardens(neighbor)) {
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
					|| this.wardenSet.contains(neighbor) || this.fixedWardenFringe.contains(neighbor)) {
				++wardenCnt;
			}
		}
		return wardenCnt;
	}
	
	private int getOppositeNeighborNumber(Vertex node) {
		int oppositeCnt = 0;
		for (Vertex neighbor: node.getAllNeighbors()) {
			if (this.oppositeFringe.contains(neighbor) || this.oppositeShore.contains(neighbor) 
					|| this.fixedOppositeFringe.contains(neighbor)) {
				++oppositeCnt;
			}
		}
		return oppositeCnt;
	}
	
	private int getSeparatorNeighborNumber(Vertex node) {
		int separatorCnt = 0;
		for (Vertex neighbor: node.getAllNeighbors()) {
			if (this.separatorSet.contains(neighbor)) {
				++separatorCnt;
			}
		}
		return separatorCnt;
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
	 * Given a node in the warden shore, if it is adjacent to 
	 * separator, put it into the warden fringe or fixed warden fringe
	 * based on the number of valid separators (separators 
	 * must have only one adjacent warden node) they have,
	 * 
	 * If it is greater than 2, put it into the fringe,
	 * otherwise put into the fixed fringe
	 * 
	 * @param node
	 * @return true if it is literally put into warden fringe
	 */
	private boolean initializeWardenFringe(Vertex node) {
		int separatorCnt = 0;
		boolean adjacentToSeparator = false;
		for (Vertex neighbor: node.getAllNeighbors()) {
			if (this.separatorSet.contains(neighbor)) {
				adjacentToSeparator = true;
				if (this.getWardenNeighborNumber(neighbor) == 1) {
					++separatorCnt;
				}
			}
		}
		node.setNumberOfSeparatorNeighbors(separatorCnt);
		if (adjacentToSeparator) {
			this.wardenShore.remove(node);
			if (separatorCnt >= 2) {
				this.wardenFringe.add(node);
			} else {
				this.fixedWardenFringe.add(node);
			}
			return true;
		}
		return false;
	}
	
	private void putIntoWardenFringeWithoutConditions(Vertex node) {
		this.wardenFringe.add(node);
		this.updateSeparatorCntForWarden_OneToMany(node);
	}
	
	/**
	 * Given the node was in the separator set, it should be pushed 
	 * into the warden fringe. 
	 * 
	 * Some of its neighbors which are in the warden fringe already 
	 * and are only connected to it will be pushed into the warden shore.
	 * 
	 * @param node
	 */
	private void putIntoWardenFringeAndUpdateFringe(Vertex node) {
		
		this.updateSeparatorNeighborNumber(node);
		this.wardenFringe.add(node);
		for (Vertex neighbor: node.getAllNeighbors()) {
			/* 
			 * Because the given node has just been put into warden fringe,
			 * the number of separator neighbor then should be zero
			 */
			if (this.wardenFringe.contains(neighbor) && this.getSeparatorNeighborNumber(neighbor) == 0) {
				this.wardenFringe.remove(neighbor);
				this.wardenShore.add(neighbor);
			}
		}
	}
	
	/**
	 * Given a node in the opposite shore, if it is adjacent to 
	 * separator, put it into the opposite fringe or fixed opposite fringe
	 * based on the number of valid separators (separators 
	 * must have only one adjacent opposite node) they have,
	 * 
	 * If it is greater than 2, put it into the fringe,
	 * otherwise put into the fixed fringe
	 * 
	 * @param node
	 * @return true if it is literally put into opposite fringe
	 */
	private boolean initializeOppoisteFringe(Vertex node) {
		int separatorCnt = 0;
		boolean adjacentToSeparator = false;
		for (Vertex neighhor: node.getAllNeighbors()) {
			if (this.separatorSet.contains(neighhor)) {
				adjacentToSeparator = true;
				if (this.getOppositeNeighborNumber(neighhor) == 1) {
					++separatorCnt;
				}
			}
		}
		node.setNumberOfSeparatorNeighbors(separatorCnt);
		
		if (adjacentToSeparator) {
			this.oppositeShore.remove(node);
			if (separatorCnt >= 2) {
				this.oppositeFringe.add(node);
			} else {
				this.fixedOppositeFringe.add(node);
			}
			return true;
		}
		return false;
	}
	
	private void putIntoOppositeFringeWithoutConditions(Vertex node) {
		this.oppositeFringe.add(node);
		this.updateSeparatorCntForOpposite_OneToMany(node);
	}
	
	
	/**
	 * Given the node was in the separator set, it should be pushed 
	 * into the opposite fringe. 
	 * 
	 * Some of its neighbors which are in the opposite fringe already 
	 * and are only connected to it will be pushed into the opposite shore.
	 * 
	 * @param node
	 */
	private void putIntoOppositeFringeAndUpdateFringe(Vertex node) {
		
		this.oppositeFringe.add(node);
		for (Vertex neighbor: node.getAllNeighbors()) {
			/* 
			 * Because the given node has just been put into opposite fringe,
			 * the number of separator neighbor then should be zero
			 */
			if (this.oppositeFringe.contains(neighbor) && this.getSeparatorNeighborNumber(neighbor) == 0) {
				this.oppositeFringe.remove(neighbor);
				this.oppositeShore.add(neighbor);
			}
		}
	}
	
	private boolean testResults() {
		Set<Vertex> visitedSet = new HashSet<Vertex>();
		Queue<Vertex> testQueue = new LinkedList<Vertex>();
		
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
					} else if (this.wardenShore.contains(nextNode) || this.wardenFringe.contains(nextNode) || this.fixedWardenFringe.contains(nextNode)) {
						/* Both shore and fringe are part of wardens */
						testQueue.add(nextNode);
						visitedSet.add(nextNode);
					} else {
						/* The traffic can go through the separators... */
						System.out.println("Test Failed.");
						return false;
					}
				}
			}
		}
		return true;
	}
	
	private void printStatistics() {
		
		System.out.println("\nPrint Statistics:: ");
		int wardenCnt = (this.wardenShore.size()+this.wardenFringe.size()+this.fixedWardenFringe.size());
		int oppositeCnt = (this.oppositeShore.size()+this.oppositeFringe.size()+this.fixedOppositeFringe.size());
		System.out.println("Warden size : " + wardenCnt);
		System.out.println("Opposite size : " + oppositeCnt);
		System.out.println("Separator size : " + this.separatorSet.size());
		System.out.println("Sum size : " + (wardenCnt+oppositeCnt+this.separatorSet.size()) + "\n");
	}
}
