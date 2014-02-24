package sim;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

import comparator.LargeSeparatorNeighborsCom;
import comparator.SmallSeparatorNeighborsCom;

import graph.Vertex;

public class OptimizeSeparator {

	private int threshold;
	private String filePath;
	private Set<Vertex> separatorSet;
	private Set<Vertex> wardenSet;
	/** warden shore contains wardens */
	private Set<Vertex> wardenShore;
	private Set<Vertex> oppositeShore;
	
	private PriorityQueue<Vertex> wardenFringe;
	private PriorityQueue<Vertex> oppositeFringe;
	private PriorityQueue<Vertex> bothFringes;
	/** fixed fringes store the nodes only have one connection to the separator set */
	private Set<Vertex> fixedWardenFringe;
	private Set<Vertex> fixedOppositeFringe;
	
	private String wardenType;
	private String oppositeType;
	
	private static final String LargeToSmall = "lts";
	private static final String smallToLarge = "stl";

	public OptimizeSeparator(Set<Vertex> separators, Set<Vertex> wardenShore,
			Set<Vertex> oppositeShore, Set<Vertex> wardens, String warden, String oppoiste, 
			int threshold, String filePath) {
		this.filePath = filePath;
		this.wardenType = warden;
		this.oppositeType = oppoiste;
		this.separatorSet = separators;
		this.wardenSet = wardens;
		this.wardenShore = wardenShore;
		this.oppositeShore = oppositeShore;
		this.threshold = threshold;
		this.bothFringes = new PriorityQueue<Vertex>(1, new LargeSeparatorNeighborsCom());
		
		/* to be initialized */
		if (this.wardenType.equalsIgnoreCase(OptimizeSeparator.LargeToSmall)) {
			this.wardenFringe = new PriorityQueue<Vertex>(1, new LargeSeparatorNeighborsCom());
		} else if (this.wardenType.equalsIgnoreCase(OptimizeSeparator.smallToLarge)) {
			this.wardenFringe = new PriorityQueue<Vertex>(1, new SmallSeparatorNeighborsCom());			
		} else {
			/**
			 * under construction
			 */
		}
		
		if (this.oppositeType.equalsIgnoreCase(OptimizeSeparator.LargeToSmall)) {
			this.oppositeFringe = new PriorityQueue<Vertex>(1, new LargeSeparatorNeighborsCom());
		} else if (this.oppositeType.equalsIgnoreCase(OptimizeSeparator.smallToLarge)) {
			this.oppositeFringe = new PriorityQueue<Vertex>(1, new SmallSeparatorNeighborsCom());		
		} else {
		/**
		 * under construction
		 */
		}
		
		this.fixedWardenFringe = new HashSet<Vertex>();
		this.fixedOppositeFringe = new HashSet<Vertex>();
	}
	
	public void simulate(int runs, BufferedWriter writeOutRatio) throws IOException {
		
		System.out.println("Before simulation..");
		this.printStatistics();
		
		if (Constants.OPT_DEBUG) {
			if (this.testResults()) {
				System.out.println("Test Passed Before Getting Optimizing.");
			} else {
				System.out.println("Test Failed Before Getting Optimizing.");
				return;
			}
		}
		
		this.runOptimization(runs);
		
	
		if (this.testResults()) {
			System.out.println("The Final Result Passed the Test.\n");
			int wardenCnt = this.wardenFringe.size() + this.fixedWardenFringe.size() + this.wardenShore.size();
			int oppositeCnt = this.oppositeFringe.size() + this.fixedOppositeFringe.size() + this.oppositeShore.size();
			writeOutRatio.write(Double.toString(1.0 * wardenCnt / oppositeCnt) + "\n");
			
		} else {
			System.out.println("The Final Result Failed to Pass the Test...");
		}
		
		System.out.println("After simulation..");
		this.printStatistics();
	}

	private void runOptimization(int runs) throws IOException {
		boolean done = false;
		BufferedWriter separatorOut = new BufferedWriter(new FileWriter(this.filePath + "/separatorTrend" + runs + ".txt"));
		this.createFringeSets();
		
		if (Constants.OPT_DEBUG) {
			if (!this.testResults()) {
				System.out.println("Test Failed After Fringes are Created.");
				return;
			} else {
				System.out.println("Test Passed After Fringes are Created.\n");
			}
		}
		
		System.out.println("Running Optimization ... Runs " + runs + ".");
		for (int i = 0; i < this.threshold && !done; ++i) {			
			
			if (i % 10 == 0) {
				System.out.println(i + "% is done..");
			}
			separatorOut.write(this.separatorSet.size() + "," + i + "\n");
			
			if (Constants.MERGEFRINGES) {
				if (this.runMergedFringe(i)) {
					break;
				}
			} else {
				if (this.runSeparatedFringes(i)) {
					break;
				}
			}
			
			if (Constants.OPT_DEBUG) {
				System.out.println("********************************************************");
			}
		}
		separatorOut.close();
	}
	
	private boolean runSeparatedFringes(int run) {
		if (Constants.OPT_DEBUG) {
			System.out.println("Run: " + (run+1));
			System.out.println("Warden Fringe " + this.wardenFringe.size() + ", Opposite Fringe " 
					+ this.oppositeFringe.size() + ", Separators " + this.separatorSet.size() + ".");
			
			System.out.println("Warden Shore Starts .. ");
		}
		
		/* Optimize warden shore. */
		if (this.optimizeWardenShore()) {
			return true;
		}
		
		if (Constants.OPT_DEBUG) {
			if (!this.testResults()) {
				System.out.println("Test Failed during swapping warden nodes in Run " + (run+1) + ".");
				return true;
			}				
			System.out.println("Opposite Shore Starts .. ");
		}			
		
		/* Optimize opposite shore. */
		if (this.optimizeOppositeShore()) {
			return true;
		}
		
		if (Constants.OPT_DEBUG) {
			if (!this.testResults()) {
				System.out.println("Test Failed during swapping opp nodes. in Run " + (run+1) + ".\n\n");
				return true;
			}
		}
		return false;
	}
	
	/**
	 * 
	 * @param run
	 * @return return true if there is an error or cannot proceed.
	 */
	private boolean runMergedFringe(int run) {
		Vertex node = this.bothFringes.peek();
		/*
		 * if is warden fringe, do warden swap,
		 * else do opp swap
		 */
		if (node.isInWardenFringe()) {
			return this.optimizeWardenShore();
		} else if (node.isInOppositeFringe()) {
			return this.optimizeOppositeShore();
		} else {
			System.out.println("An Error Because of This Merged Fringe..");
			return true;
		}
	}
	
	
	/**
	 * When a node in the warden shore in pushed into the separator set, 
	 * all its neighbors have to be modified accordingly.
	 * 
	 * @return true if cannot find a candidate to swap into separator.
	 */
	private boolean optimizeWardenShore() {
		if (this.isTheFringeEmpty(true)) {
			System.out.println("Stops because the warden fringe is empty.......");
			return true;
		}
		
		Vertex currentNode = this.pollFromOneFringe(true);
		currentNode.unsetFringesFlag();
		/* when it only has one separator neighbor, it is not worth to swap anymore */
		if (currentNode.getNumberOfSeparatorNeighbors() == 1) {
			System.out.println("Stops because the warden shore is not worth of swapping...........");
			this.putIntoOneFringe(true, currentNode);
			currentNode.setInWardenFringe();
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
					neighbor.setInOppositeFringe();
				}
				
			} else if (this.wardenShore.contains(neighbor)) {
				
				/* Wardens cannot be swapped to separators.. */
				if (!this.wardenSet.contains(neighbor)) {
					this.wardenShore.remove(neighbor);
					this.putIntoWardenFringeWithoutConditions(neighbor);
					neighbor.setInWardenFringe();
				}
				
			} else if (this.isInTheFringe(true, neighbor)) {
				this.removeFromOneFringe(true, neighbor);
				this.putIntoWardenFringeWithoutConditions(neighbor);
				neighbor.setInWardenFringe();

			} else if (this.fixedWardenFringe.contains(neighbor)) {
				this.fixedWardenFringe.remove(neighbor);
				this.updateSeparatorNeighborNumber(neighbor);
				this.putIntoOneFringe(true, neighbor);
				neighbor.setInWardenFringe();

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
		//if (this.oppositeFringe.isEmpty()) {
		if (this.isTheFringeEmpty(false)) {
			System.out.println("Stops because the oppoiste fringe is empty.............");
			return true;
		}
		
		Vertex currentNode = this.pollFromOneFringe(false);
		currentNode.unsetFringesFlag();
		if (currentNode.getNumberOfSeparatorNeighbors() == 1) {
			this.putIntoOneFringe(false, currentNode);
			currentNode.setInOppositeFringe();
			System.out.println("Stops because the opposite shore is not worth of swapping.........");
			return true;
		}
		
		this.separatorSet.add(currentNode);
		for (Vertex neighbor: currentNode.getAllNeighbors()) {
			
			if (this.separatorSet.contains(neighbor)) {
				if (this.getOppositeNeighborNumber(neighbor) == 0) {
					this.separatorSet.remove(neighbor);
					this.putIntoWardenFringeAndUpdateFringe(neighbor);
					neighbor.setInWardenFringe();
				}
				
			} else if (this.oppositeShore.contains(neighbor)) {
				this.oppositeShore.remove(neighbor);
				this.putIntoOppositeFringeWithoutConditions(neighbor);
				neighbor.setInOppositeFringe();
				
			} else if (this.isInTheFringe(false, neighbor)) {	
				this.removeFromOneFringe(false, neighbor);
				this.putIntoOppositeFringeWithoutConditions(neighbor);
				neighbor.setInOppositeFringe();
				
			} else if (this.fixedOppositeFringe.contains(neighbor)) {
				this.fixedOppositeFringe.remove(neighbor);
				this.updateSeparatorNeighborNumber(neighbor);
				this.putIntoOneFringe(false, neighbor);
				neighbor.setInOppositeFringe();
				
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
	
	private Vertex pollFromOneFringe(boolean fromWardenFringe) {
		if (Constants.MERGEFRINGES) {
			return this.bothFringes.poll();
		} else {
			if (fromWardenFringe) {
				return this.wardenFringe.poll();
			} else {
				return this.oppositeFringe.poll();
			}
		}
	}
	
	private void putIntoOneFringe(boolean toWardenFringe, Vertex node) {
		if (Constants.MERGEFRINGES) {
			this.bothFringes.add(node);
		} else {
			if (toWardenFringe) {
				this.wardenFringe.add(node);
			} else {
				this.oppositeFringe.add(node);
			}
		}
	}
	
	private void removeFromOneFringe(boolean fromWardenFringe, Vertex node) {
		if (Constants.MERGEFRINGES) {
			this.bothFringes.remove(node);
		} else {
			if (fromWardenFringe) {
				this.wardenFringe.remove(node);
			} else {
				this.oppositeFringe.remove(node);
			}
		}
	}
	
	private boolean isInTheFringe(boolean checkWardenFringe, Vertex node) {
		if (Constants.MERGEFRINGES) {
			return this.bothFringes.contains(node);
		} else {
			if (checkWardenFringe) {
				return this.wardenFringe.contains(node);
			} else {
				return this.oppositeFringe.contains(node);
			}
		}
	}
	
	private boolean isTheFringeEmpty(boolean checkWardenFringe) {
		if (Constants.MERGEFRINGES) {
			return this.bothFringes.isEmpty();
		} else {
			if (checkWardenFringe) {
				return this.wardenFringe.isEmpty();
			} else {
				return this.oppositeFringe.isEmpty();
			}
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
			if (this.isInTheFringe(true, neighbor) || this.wardenShore.contains(neighbor)
					|| this.wardenSet.contains(neighbor) || this.fixedWardenFringe.contains(neighbor)) {
				++wardenCnt;
			}
		}
		return wardenCnt;
	}
	
	private int getOppositeNeighborNumber(Vertex node) {
		int oppositeCnt = 0;
		for (Vertex neighbor: node.getAllNeighbors()) {
			if (this.isInTheFringe(false, neighbor) || this.oppositeShore.contains(neighbor) 
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
				this.putIntoOneFringe(true, node);
				node.setInWardenFringe();
			} else {
				this.fixedWardenFringe.add(node);
			}
			return true;
		}
		return false;
	}
	
	private void putIntoWardenFringeWithoutConditions(Vertex node) {
		node.setInWardenFringe();
		this.putIntoOneFringe(true, node);
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
		node.setInWardenFringe();
		this.putIntoOneFringe(true, node);
		for (Vertex neighbor: node.getAllNeighbors()) {
			/* 
			 * Because the given node has just been put into warden fringe,
			 * the number of separator neighbor then should be zero
			 */
			if (this.isInTheFringe(true, neighbor) && this.getSeparatorNeighborNumber(neighbor) == 0) {
				neighbor.unsetFringesFlag();
				this.removeFromOneFringe(true, neighbor);
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
				node.setInOppositeFringe();
				this.putIntoOneFringe(false, node);
			} else {
				this.fixedOppositeFringe.add(node);
			}
			return true;
		}
		return false;
	}
	
	private void putIntoOppositeFringeWithoutConditions(Vertex node) {
		node.setInOppositeFringe();
		this.putIntoOneFringe(false, node);
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
		
		node.setInOppositeFringe();
		this.oppositeFringe.add(node);
		for (Vertex neighbor: node.getAllNeighbors()) {
			/* 
			 * Because the given node has just been put into opposite fringe,
			 * the number of separator neighbor then should be zero
			 */
			if (this.isInTheFringe(false, neighbor) && this.getSeparatorNeighborNumber(neighbor) == 0) {
				neighbor.unsetFringesFlag();
				this.removeFromOneFringe(false, neighbor);
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
					} else if (this.wardenShore.contains(nextNode) || this.isInTheFringe(true, nextNode) || this.fixedWardenFringe.contains(nextNode)) {
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
		
		System.out.println("Print Statistics:: ");
		int wardenCnt = (this.wardenShore.size()+this.wardenFringe.size()+this.fixedWardenFringe.size());
		int oppositeCnt = (this.oppositeShore.size()+this.oppositeFringe.size()+this.fixedOppositeFringe.size());
		System.out.println("Warden size : " + wardenCnt);
		System.out.println("Opposite size : " + oppositeCnt);
		System.out.println("Separator size : " + this.separatorSet.size());
		System.out.println("Sum size : " + (wardenCnt+oppositeCnt+this.separatorSet.size()) + "\n\n");
	}
}
