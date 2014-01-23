package sim;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.Stack;
import java.util.StringTokenizer;

import comparator.*;

import graph.Vertex;

public class GraphPartitioning {

	private static final String RAND_MODE = "random";
	private static final String DFS_MODE = "dfs";
	private static final String BFS_MODE = "bfs";
	private static final String OUTWARD_LARGE_MODE = "outwardlarge";
	private static final String OUTWARD_SMALL_MODE = "outwardsmall";
	private static final String OUTWARD_MODE = "outward";
	private static final String INWARD_LARGE_MODE = "inwardlarge";
	private static final String INWARD_SMALL_MODE = "inwardsmall";
	private static final String INWARD_MODE = "inward";
	private static final String DEGREE_LARGE_MODE = "dgrlarge";
	private static final String DEGREE_SMALL_MODE = "dgrsmall";
	private static final String DEGREE_MODE = "dgr";

	/** store initial wardens */
	private Set<Vertex> wardenSet;
	/** store vertexes that must be in warden side */
	private List<Vertex> wardenBlack;
	/**
	 * store the extending vertexes that are adjacent to wardens, which would
	 * become separators
	 */
	private List<Vertex> wardenGray;
	/** store vertexes that must be in opposite side */
	private Set<Vertex> oppositeBlack;
	/**
	 * store the extending vertexes of the opposite side, might include those
	 * will be in the opposite set
	 */
	private List<Vertex> oppositeGray;
	/** store vertexes that are neither in warden side nor opposite side yet */
	private HashMap<Integer, Vertex> neutralVertexMap;
	/** a copy of neutralVertexMap */
	private HashMap<Integer, Vertex> neutralVertexCopy;
	/** store real separators */
	private Set<Vertex> separatorSet;
	/** write the number of separators into a file */
	private BufferedWriter separatorOut;
	/** write the number of wardens in the warden set into a file */
	private BufferedWriter wardenOut;
	Random randomNext;

	private Stack<Vertex> wardenStack;
	private Stack<Vertex> oppositeStack;
	private Queue<Vertex> wardenQueue;
	private Queue<Vertex> oppositeQueue;
	private PriorityQueue<Vertex> wardenInwardPriorityQueue;
	private PriorityQueue<Vertex> oppositeInwardPriorityQueue;
	private PriorityQueue<Vertex> wardenOutwardPriorityQueue;
	private PriorityQueue<Vertex> oppositeOutwardPriorityQueue;
	private PriorityQueue<Vertex> wardenDegreePriorityQueue;
	private PriorityQueue<Vertex> oppositeDegreePriorityQueue;

	private String wardenMode;
	private String oppositeMode;
	/* nodes to be put into opposite shore */
	private Set<Vertex> filteredSeparators;

	private Set<Vertex> validSeparators;
	private Set<Vertex> validWardenShore;

	public GraphPartitioning() throws IOException {
		this.wardenSet = new HashSet<Vertex>();
		this.wardenBlack = new ArrayList<Vertex>();
		this.wardenGray = new ArrayList<Vertex>();
		this.oppositeBlack = new HashSet<Vertex>();
		this.oppositeGray = new ArrayList<Vertex>();
		this.neutralVertexMap = new HashMap<Integer, Vertex>();
		this.neutralVertexCopy = new HashMap<Integer, Vertex>();
		this.separatorSet = new HashSet<Vertex>();
		this.filteredSeparators = new HashSet<Vertex>();
		this.validSeparators = new HashSet<Vertex>();
		this.validWardenShore = new HashSet<Vertex>();

		this.randomNext = new Random();

		this.wardenStack = new Stack<Vertex>();
		this.oppositeStack = new Stack<Vertex>();
		this.wardenQueue = new LinkedList<Vertex>();
		this.oppositeQueue = new LinkedList<Vertex>();

		/*
		 * initialize the priority queues for an arbitrary type at the
		 * beginning, which will be reinitialized later if it is any type of
		 * degree based search
		 */
		this.wardenInwardPriorityQueue = new PriorityQueue<Vertex>(1,
				new InwardLargeDegreeBasedCom());
		this.wardenOutwardPriorityQueue = new PriorityQueue<Vertex>(1,
				new OutwardLargeDegreeBasedCom());
		this.wardenDegreePriorityQueue = new PriorityQueue<Vertex>(1,
				new LargeDegreeBasedCom());
		this.oppositeOutwardPriorityQueue = new PriorityQueue<Vertex>(1,
				new OutwardLargeDegreeBasedCom());
		this.oppositeInwardPriorityQueue = new PriorityQueue<Vertex>(1,
				new InwardLargeDegreeBasedCom());
		this.oppositeDegreePriorityQueue = new PriorityQueue<Vertex>(1,
				new LargeDegreeBasedCom());
	}

	/**
	 * six types totally, divided by the type of priority, increasing or
	 * decreasing; and the type of degree based search, inward, outward or
	 * total.
	 */
	private void initializePriorityQueue() {
		if (this.wardenMode
				.equalsIgnoreCase(GraphPartitioning.INWARD_LARGE_MODE)) {
			this.wardenInwardPriorityQueue = new PriorityQueue<Vertex>(1,
					new InwardLargeDegreeBasedCom());
			this.wardenMode = GraphPartitioning.INWARD_MODE;
		} else if (this.wardenMode
				.equalsIgnoreCase(GraphPartitioning.INWARD_SMALL_MODE)) {
			this.wardenInwardPriorityQueue = new PriorityQueue<Vertex>(1,
					new InwardSmallDegreeBasedCom());
			this.wardenMode = GraphPartitioning.INWARD_MODE;
		} else if (this.wardenMode
				.equalsIgnoreCase(GraphPartitioning.OUTWARD_LARGE_MODE)) {
			this.wardenOutwardPriorityQueue = new PriorityQueue<Vertex>(1,
					new OutwardLargeDegreeBasedCom());
			this.wardenMode = GraphPartitioning.OUTWARD_MODE;
		} else if (this.wardenMode
				.equalsIgnoreCase(GraphPartitioning.OUTWARD_SMALL_MODE)) {
			this.wardenOutwardPriorityQueue = new PriorityQueue<Vertex>(1,
					new OutwardSmallDegreeBasedCom());
			this.wardenMode = GraphPartitioning.OUTWARD_MODE;
		} else if (this.wardenMode
				.equalsIgnoreCase(GraphPartitioning.DEGREE_LARGE_MODE)) {
			this.wardenDegreePriorityQueue = new PriorityQueue<Vertex>(1,
					new LargeDegreeBasedCom());
			this.wardenMode = GraphPartitioning.DEGREE_MODE;
		} else if (this.wardenMode
				.equalsIgnoreCase(GraphPartitioning.DEGREE_SMALL_MODE)) {
			this.wardenDegreePriorityQueue = new PriorityQueue<Vertex>(1,
					new SmallDegreeBasedCom());
			this.wardenMode = GraphPartitioning.DEGREE_MODE;
		} else
			;

		if (this.oppositeMode
				.equalsIgnoreCase(GraphPartitioning.INWARD_LARGE_MODE)) {
			this.oppositeInwardPriorityQueue = new PriorityQueue<Vertex>(1,
					new InwardLargeDegreeBasedCom());
			this.oppositeMode = GraphPartitioning.INWARD_MODE;
		} else if (this.oppositeMode
				.equalsIgnoreCase(GraphPartitioning.INWARD_SMALL_MODE)) {
			this.oppositeInwardPriorityQueue = new PriorityQueue<Vertex>(1,
					new InwardSmallDegreeBasedCom());
			this.oppositeMode = GraphPartitioning.INWARD_MODE;
		} else if (this.oppositeMode
				.equalsIgnoreCase(GraphPartitioning.OUTWARD_LARGE_MODE)) {
			this.oppositeOutwardPriorityQueue = new PriorityQueue<Vertex>(1,
					new OutwardLargeDegreeBasedCom());
			this.oppositeMode = GraphPartitioning.OUTWARD_MODE;
		} else if (this.oppositeMode
				.equalsIgnoreCase(GraphPartitioning.OUTWARD_SMALL_MODE)) {
			this.oppositeOutwardPriorityQueue = new PriorityQueue<Vertex>(1,
					new OutwardSmallDegreeBasedCom());
			this.oppositeMode = GraphPartitioning.OUTWARD_MODE;
		} else if (this.oppositeMode
				.equalsIgnoreCase(GraphPartitioning.DEGREE_LARGE_MODE)) {
			this.oppositeDegreePriorityQueue = new PriorityQueue<Vertex>(1,
					new LargeDegreeBasedCom());
			this.oppositeMode = GraphPartitioning.DEGREE_MODE;
		} else if (this.oppositeMode
				.equalsIgnoreCase(GraphPartitioning.DEGREE_SMALL_MODE)) {
			this.oppositeDegreePriorityQueue = new PriorityQueue<Vertex>(1,
					new SmallDegreeBasedCom());
			this.oppositeMode = GraphPartitioning.DEGREE_MODE;
		} else
			;
	}

	private void createNeutralVertexCopy() {
		for (int key : this.neutralVertexMap.keySet()) {
			this.neutralVertexCopy.put(key, this.neutralVertexMap.get(key));
		}
	}

	public void multipleRuns(String wardenFile, String wardenMode,
			String oppositeMode, int trials) throws IOException {
		this.wardenMode = wardenMode;
		this.oppositeMode = oppositeMode;
		this.separatorOut = new BufferedWriter(new FileWriter(wardenMode + "_"
				+ oppositeMode + "_SeparatorCnt.txt"));
		this.wardenOut = new BufferedWriter(new FileWriter(wardenMode + "_"
				+ oppositeMode + "_WardenCnt.txt"));
		this.initializePriorityQueue();
		this.generateGraph(wardenFile);
		this.createNeutralVertexCopy();

		for (int i = 0; i < trials; ++i) {
			if (i % (trials / 10) == 0) {
				System.out.println(100 * i / trials + "% done!");
			}
			this.reset();
			if (this.wardenMode.equalsIgnoreCase(GraphPartitioning.RAND_MODE)
					&& this.oppositeMode
							.equalsIgnoreCase(GraphPartitioning.RAND_MODE)) {
				this.randomRandomPartitioning();
			} else if ((this.wardenMode
					.equalsIgnoreCase(GraphPartitioning.DFS_MODE) || this.wardenMode
					.equalsIgnoreCase(GraphPartitioning.BFS_MODE))
					&& (this.oppositeMode
							.equalsIgnoreCase(GraphPartitioning.DFS_MODE))
					|| this.oppositeMode
							.equalsIgnoreCase(GraphPartitioning.BFS_MODE)) {
				this.searchingPartitioning();
			} else if ((this.wardenMode
					.equalsIgnoreCase(GraphPartitioning.INWARD_MODE)
					|| this.wardenMode
							.equalsIgnoreCase(GraphPartitioning.OUTWARD_MODE) || this.wardenMode
						.equalsIgnoreCase(GraphPartitioning.DEGREE_MODE))
					&& (this.oppositeMode
							.equalsIgnoreCase(GraphPartitioning.INWARD_MODE))
					|| this.oppositeMode
							.equalsIgnoreCase(GraphPartitioning.OUTWARD_MODE)
					|| this.oppositeMode
							.equalsIgnoreCase(GraphPartitioning.DEGREE_MODE)) {
				this.degreeBasedPartitioning();
			} else {
				/* under construction.. */
				System.out.println("Invalid Mode!!");
				return;
			}
			
			this.removeRedundantComponents();
			this.printResults();
			if (Constants.TEST) {
				if (!this.passSeparatorTest()) {
					return;
				}
			}
		}
		this.separatorOut.close();
		this.wardenOut.close();
	}

	public boolean singleRun(String wardenFile, String wardenMode,
			String oppositeMode) throws IOException {

		this.wardenMode = wardenMode;
		this.oppositeMode = oppositeMode;
		this.separatorOut = new BufferedWriter(new FileWriter(wardenMode + "_"
				+ oppositeMode + "_SeparatorCnt.txt"));
		this.wardenOut = new BufferedWriter(new FileWriter(wardenMode + "_"
				+ oppositeMode + "_WardenCnt.txt"));
		this.initializePriorityQueue();
		this.generateGraph(wardenFile);

		this.createNeutralVertexCopy();

		if (this.wardenMode.equalsIgnoreCase(GraphPartitioning.RAND_MODE)
				&& this.wardenMode
						.equalsIgnoreCase(GraphPartitioning.RAND_MODE)) {
			this.randomRandomPartitioning();
		} else if (this.oppositeMode.equalsIgnoreCase(GraphPartitioning.DFS_MODE)
				&& this.oppositeMode.equalsIgnoreCase(GraphPartitioning.DFS_MODE)) {
			for (Vertex node : this.neutralVertexMap.values()) {
				node.createAvailableNeighborList();
			}
			this.searchingPartitioning();
		} else if ((this.wardenMode.equalsIgnoreCase(GraphPartitioning.INWARD_MODE)
				|| this.wardenMode.equalsIgnoreCase(GraphPartitioning.OUTWARD_MODE) || this.wardenMode
					.equalsIgnoreCase(GraphPartitioning.DEGREE_MODE))
				&& (this.oppositeMode.equalsIgnoreCase(GraphPartitioning.INWARD_MODE))
				|| this.oppositeMode.equalsIgnoreCase(GraphPartitioning.OUTWARD_MODE)
				|| this.oppositeMode.equalsIgnoreCase(GraphPartitioning.DEGREE_MODE)) {
			this.degreeBasedPartitioning();
		} else {
			/* under construction.. */
			System.out.println("Invalid Mode!!");
			return false;
		}
		
		this.removeRedundantComponents();
		if (Constants.TEST) {
			if (!this.passSeparatorTest())
				return false;
		}
		this.printResults();
		this.separatorOut.close();
		this.wardenOut.close();
		
		return true;
	}

	private void reset() {
		if (Constants.SEP_DEBUG) {
			System.out.println(this.wardenSet.size() + ", "
					+ this.oppositeBlack.size() + ", " + this.wardenGray.size()
					+ ", " + this.oppositeGray.size() + ", " + ", "
					+ this.separatorSet.size());
		}

		this.validSeparators.clear();
		this.validWardenShore.clear();
		this.wardenBlack.clear();
		this.wardenGray.clear();
		this.oppositeBlack.clear();
		this.oppositeGray.clear();
		this.separatorSet.clear();
		this.neutralVertexMap.clear();

		this.oppositeStack.clear();
		this.oppositeQueue.clear();

		this.wardenInwardPriorityQueue.clear();
		this.wardenOutwardPriorityQueue.clear();
		this.wardenDegreePriorityQueue.clear();
		this.oppositeInwardPriorityQueue.clear();
		this.oppositeOutwardPriorityQueue.clear();
		this.oppositeDegreePriorityQueue.clear();
		for (int key : this.neutralVertexCopy.keySet()) {
			this.neutralVertexMap.put(key, this.neutralVertexCopy.get(key));
			/* available neighbor list is used for random dfs search */
			this.neutralVertexMap.get(key).createAvailableNeighborList();
		}
	}

	private void degreeBasedPartitioning() {
		this.createWardenAdjacentSet();
		boolean partitioningDone = false;
		while (!partitioningDone) {

			/*
			 * when there is no vertex to extend for opposite gray set, all
			 * possible separators are found.
			 */
			if (this.degreeBasedWardenShore()) {
				partitioningDone = true;
			}

			if (this.degreeBasedOppositeShore()) {
				if (this.randomSelectNextSeed() == -1) {
					partitioningDone = true;
				}
			}
			if (Constants.SEP_DEBUG) {
				System.out.println("******");
			}
		}
		this.classifyNodesInQueue();

		if (Constants.SEP_DEBUG) {
			System.out.println("before filte: " + this.getSeparatorSize());
		}
		// this.filterSeparatorSet();
		if (Constants.SEP_DEBUG) {
			System.out.println("after filte: " + this.getSeparatorSize());
		}
	}

	/**
	 * the nodes in the priority queue might be either a separator or a warden,
	 * so this function is to classify these nodes into two parts
	 */
	private void classifyNodesInQueue() {
		Queue<Vertex> Que = null;
		if (this.wardenMode.equalsIgnoreCase(GraphPartitioning.INWARD_MODE)) {
			Que = this.wardenInwardPriorityQueue;
		} else if (this.wardenMode
				.equalsIgnoreCase(GraphPartitioning.OUTWARD_MODE)) {
			Que = this.wardenOutwardPriorityQueue;
		} else if (this.wardenMode
				.equalsIgnoreCase(GraphPartitioning.DEGREE_MODE)) {
			Que = this.wardenDegreePriorityQueue;
		} else {
			/* invalid mode */
		}
		while (!Que.isEmpty()) {
			Vertex node = Que.poll();
			boolean isSeparator = false;
			for (Vertex neighbor : node.getAllNeighbors()) {
				if (this.neighborCheck(neighbor)) {
					this.separatorSet.add(node);
					isSeparator = true;
					break;
				}
			}
			if (!isSeparator) {
				this.wardenBlack.add(node);
			}
		}
	}

	/**
	 * petition the graph and find the separators using oppositeAddInThisTurn to
	 * make sure in each turn, both sides add one and only one vertex if any is
	 * available.
	 * 
	 * both the ways warden and opposite sides extend the set randomly
	 */
	private void randomRandomPartitioning() {
		this.createWardenAdjacentSet();
		boolean partitioningDone = false;
		while (!partitioningDone) {

			/*
			 * when there is no vertex to extend for warden gray set, all
			 * possible separators are found.
			 */
			if (this.randomExtendWardenShore()) {
				partitioningDone = true;
			}

			if (this.randomExtendOppositeShore()) {
				if (this.randomSelectNextSeed() == -1) {
					partitioningDone = true;
				}
			}
			if (Constants.SEP_DEBUG) {
				System.out.println("******");
			}
		}

		this.checkWardenGray();
	}

	/**
	 * randomly select ONE extendible vertex from warden adjacent set put into
	 * warden set, relax its neighbors and update warden adjacent set for the
	 * next round.
	 * 
	 * @return true if warden gray set is empty false if warden gray set is not
	 *         empty
	 */
	private boolean randomExtendWardenShore() {
		int randomIndex, cntToBeGray;
		boolean isSeparator, notFindNextNode = true;
		while (notFindNextNode) {
			/* if cannot find any vertex to extend, partitioning finishes! */
			if (this.wardenGray.isEmpty())
				return true;

			randomIndex = this.randomNext.nextInt(this.wardenGray.size());
			Vertex currentNode = this.wardenGray.get(randomIndex);

			cntToBeGray = 0;
			isSeparator = false;
			for (Vertex nextNode : currentNode.getAllNeighbors()) {
				/*
				 * if a node in gray set is adjacent to opposite , it is a
				 * separator
				 */
				if (this.oppositeGray.contains(nextNode)
						|| this.oppositeBlack.contains(nextNode)) {
					isSeparator = true;
				}
				if (this.neutralVertexMap.containsKey(nextNode.getVertexID())) {
					this.wardenGray.add(nextNode);
					this.neutralVertexMap.remove(nextNode.getVertexID());
					++cntToBeGray;

					if (Constants.SEP_DEBUG) {
						System.out.println("warden set added: "
								+ nextNode.getVertexID());
					}
				}
			}
			/*
			 * if it is an interior separator, need to keep on finding next one
			 * to extend; if it is not an interior separator, stop finding next
			 * one, and finish this turn
			 * 
			 * if it is not a separator if it is an interior node, keep finding,
			 * otherwise, stop and return
			 */
			/*
			 * if (isSeparator) { this.separatorSet.add(currentNode);
			 * this.wardenGray.remove(currentNode); if (cntToBeGray != 0) {
			 * notFindNextNode = false; } } else if (cntToBeGray == 0) {
			 * this.wardenBlack.add(currentNode);
			 * this.wardenGray.remove(currentNode); } else { notFindNextNode =
			 * false; }
			 */
			if (isSeparator) {
				this.separatorSet.add(currentNode);
				this.wardenGray.remove(currentNode);
				if (cntToBeGray != 0) {
					notFindNextNode = false;
				}
			} else {
				this.wardenBlack.add(currentNode);
				this.wardenGray.remove(currentNode);
				if (cntToBeGray != 0) {
					notFindNextNode = false;
				}
			}
		}
		return false;
	}

	/**
	 * randomly select ONE extendible vertex that adjacent to the vertexes in
	 * opposite extending set.
	 * 
	 * if a vertex in opposite extending set is not extendible, put it into
	 * opposite set.
	 * 
	 * @return
	 */
	private boolean randomExtendOppositeShore() {

		int randomIndex, cntToBeGray;
		boolean notFindNextOpposite = true;
		while (notFindNextOpposite) {
			/* if cannot find any vertex to extend, petitioning finishes! */
			if (this.oppositeGray.isEmpty())
				return true;

			randomIndex = this.randomNext.nextInt(this.oppositeGray.size());
			Vertex currentNode = this.oppositeGray.get(randomIndex);

			cntToBeGray = 0;
			for (Vertex nextNode : currentNode.getAllNeighbors()) {
				if (this.neutralVertexMap.containsKey(nextNode.getVertexID())) {
					this.oppositeGray.add(nextNode);
					this.neutralVertexMap.remove(nextNode.getVertexID());
					++cntToBeGray;

					if (Constants.SEP_DEBUG) {
						System.out.println("opposit set added: "
								+ nextNode.getVertexID());
					}
				}
			}
			if (cntToBeGray == 0) {
				this.oppositeBlack.add(currentNode);
				this.oppositeGray.remove(currentNode);
			} else {
				notFindNextOpposite = false;
			}
		}
		return false;
	}

	/**
	 * 
	 * @return true if the queue is empty which means the research is over and
	 *         all separators are found. false otherwise.
	 */
	private boolean degreeBasedWardenShore() {
		int cntNextNode;
		boolean isSeparator, notFindNextNode = true;
		while (notFindNextNode) {
			/* if cannot find any vertex to extend, partitioning finishes! */
			if (this.checkTermination(true))
				return true;

			cntNextNode = 0;
			isSeparator = false;
			Vertex currentNode = this.getCurrentNode(true);
			this.wardenBlack.add(currentNode);
			for (Vertex nextNode : currentNode.getAllNeighbors()) {
				/*
				 * if a node in gray set is adjacent to opposite , it is a
				 * separator
				 */
				if (neighborCheck(nextNode)) {
					isSeparator = true;
				}
				if (this.neutralVertexMap.containsKey(nextNode.getVertexID())) {
					nextNode.setNeighborSets(this.countBlackNeighborsNumber(
							nextNode, true));
					this.addToSearchingSpace(nextNode, true);
					this.neutralVertexMap.remove(nextNode.getVertexID());
					++cntNextNode;

					if (Constants.SEP_DEBUG) {
						System.out.println("warden set added: "
								+ nextNode.getVertexID());
					}
				}
			}

			/*
			 * if the current node is a separator, remove it from the black set,
			 * also, check if the number of extendible nodes is zero, if it is
			 * not zero, then finish this round.
			 */
			if (isSeparator) {
				this.separatorSet.add(currentNode);
				this.wardenBlack.remove(this.wardenBlack.size() - 1);
				if (cntNextNode != 0) {
					notFindNextNode = false;
				}
			} else {
				if (cntNextNode != 0) {
					notFindNextNode = false;
				}
			}
		}
		return false;
	}

	private boolean degreeBasedOppositeShore() {
		int cntNextNode;
		boolean notFindNextOpposite = true;
		while (notFindNextOpposite) {
			/* if cannot find any vertex to extend, petitioning finishes! */
			if (this.checkTermination(false))
				return true;

			cntNextNode = 0;
			Vertex currentNode = this.getCurrentNode(false);
			this.oppositeBlack.add(currentNode);
			for (Vertex nextNode : currentNode.getAllNeighbors()) {
				if (this.neutralVertexMap.containsKey(nextNode.getVertexID())) {
					nextNode.setNeighborSets(this.countBlackNeighborsNumber(
							nextNode, false));
					this.addToSearchingSpace(nextNode, false);
					this.neutralVertexMap.remove(nextNode.getVertexID());
					++cntNextNode;

					if (Constants.SEP_DEBUG) {
						System.out.println("opposit set added: "
								+ nextNode.getVertexID());
					}
				}
			}
			if (cntNextNode == 0) {
				this.oppositeBlack.add(currentNode);
			} else {
				notFindNextOpposite = false;
			}

		}
		return false;
	}

	/**
	 * dfs or bfs search
	 */
	private void searchingPartitioning() {
		this.createWardenAdjacentSet();
		boolean partitioningDone = false;
		while (!partitioningDone) {
			/* extend a node from warden shore */
			if (this.randomSearchingExtendWardenShore()) {
				partitioningDone = true;
			}

			/* extend a node from opposite shore */
			if (this.randomSearchingExtendOppositeShore()) {
				if (this.randomSelectNextSeed() == -1) {
					partitioningDone = true;
				}
			}

			if (Constants.SEP_DEBUG) {
				System.out.println("******");
			}
		}
	}

	/**
	 * every time traverse to a same node again, it has to scan all its
	 * neighbors. Not efficient, but this will avoid extra storage for the
	 * searching space and avoid reinitialize that storage for all the nodes
	 * which might be expensive. A little tradeoff??
	 * 
	 * @return true if search is finished. false if search is not finished.
	 */
	private boolean randomSearchingExtendWardenShore() {
		boolean thisRoundDone = false;
		while (!thisRoundDone) {
			/* when the stack/queue is empty, partitioning is done. */
			if (this.checkTermination(true)) {
				return true;
			}

			boolean currentSearchingSpaceEmpty = false;
			Vertex nextNode = null;
			Vertex currentNode = this.getCurrentNode(true);

			/* randomly select a neighbor for the next node */
			do {
				nextNode = currentNode.randomSelectANeighbor(this.randomNext);
				if (nextNode == null) {
					currentSearchingSpaceEmpty = true;
					break;
				}
			} while (!this.neutralVertexMap.containsKey(nextNode.getVertexID()));

			/*
			 * if cannot extend from the current node, pop it out of the stack,
			 * and check the next node on the top of the stack
			 */
			if (currentSearchingSpaceEmpty) {
				this.removeUnextendableNode(true);
				/*
				 * check if current is a separator, and put it in the right
				 * place
				 */
				if (separatorCheck(currentNode)) {
					this.separatorSet.add(currentNode);

					if (Constants.SEP_DEBUG) {
						System.out.println("@dfs add separator "
								+ currentNode.getVertexID());
					}
				} else {
					this.wardenBlack.add(currentNode);
				}
			} else {
				this.neutralVertexMap.remove(nextNode.getVertexID());
				this.addToSearchingSpace(nextNode, true);
				thisRoundDone = true;

				if (Constants.SEP_DEBUG) {
					System.out.println("warden add " + nextNode.getVertexID());
				}
			}
		}
		return false;
	}

	/**
	 * @return true if not successfully extend a next node, and need to randomly
	 *         select a seed. false if successfully extend a next node,
	 */
	private boolean randomSearchingExtendOppositeShore() {
		boolean thisRoundDone = false;
		/* if the stack is empty, needs to select next random seed */
		while (!thisRoundDone) {
			if (this.checkTermination(false)) {
				return true;
			}

			boolean currentSearchingSpaceEmpty = false;
			Vertex nextNode = null;
			Vertex currentNode = this.getCurrentNode(false);

			/* randomly select a neighbor for the next node */
			do {
				nextNode = currentNode.randomSelectANeighbor(this.randomNext);
				if (nextNode == null) {
					currentSearchingSpaceEmpty = true;
					break;
				}
			} while (!this.neutralVertexMap.containsKey(nextNode.getVertexID()));

			if (currentSearchingSpaceEmpty) {
				this.removeUnextendableNode(false);
				this.oppositeBlack.add(currentNode);
			} else {
				this.neutralVertexMap.remove(nextNode.getVertexID());
				this.addToSearchingSpace(nextNode, false);
				thisRoundDone = true;

				if (Constants.SEP_DEBUG) {
					System.out
							.println("opposite add " + nextNode.getVertexID());
				}
			}
		}
		return false;
	}

	/**
	 * check whether the search finishes, if the set is empty, then search is
	 * done.
	 * 
	 * @param wardenShore
	 * @return true, if search is done; false, if not
	 */
	private boolean checkTermination(boolean wardenShore) {
		if (wardenShore) {
			if (this.wardenMode.equalsIgnoreCase(GraphPartitioning.DFS_MODE)) {
				if (this.wardenStack.empty())
					return true;
			} else if (this.wardenMode
					.equalsIgnoreCase(GraphPartitioning.BFS_MODE)) {
				if (this.wardenQueue.isEmpty())
					return true;
			} else if (this.wardenMode
					.equalsIgnoreCase(GraphPartitioning.INWARD_MODE)) {
				if (this.wardenInwardPriorityQueue.isEmpty())
					return true;
			} else if (this.wardenMode
					.equalsIgnoreCase(GraphPartitioning.OUTWARD_MODE)) {
				if (this.wardenOutwardPriorityQueue.isEmpty())
					return true;
			} else if (this.wardenMode
					.equalsIgnoreCase(GraphPartitioning.DEGREE_MODE)) {
				if (this.wardenDegreePriorityQueue.isEmpty())
					return true;
			} else {
				/* invalid mode */
			}
		} else {
			if (this.oppositeMode.equalsIgnoreCase(GraphPartitioning.DFS_MODE)) {
				if (this.oppositeStack.empty())
					return true;
			} else if (this.oppositeMode
					.equalsIgnoreCase(GraphPartitioning.BFS_MODE)) {
				if (this.oppositeQueue.isEmpty())
					return true;
			} else if (this.oppositeMode
					.equalsIgnoreCase(GraphPartitioning.INWARD_MODE)) {
				if (this.oppositeInwardPriorityQueue.isEmpty())
					return true;
			} else if (this.oppositeMode
					.equalsIgnoreCase(GraphPartitioning.OUTWARD_MODE)) {
				if (this.oppositeOutwardPriorityQueue.isEmpty())
					return true;
			} else if (this.oppositeMode
					.equalsIgnoreCase(GraphPartitioning.DEGREE_MODE)) {
				if (this.oppositeDegreePriorityQueue.isEmpty())
					return true;
			} else {
				/* invalid mode */
			}
		}
		return false;
	}

	/**
	 * get the node to be extended in this round based on the searching mode. if
	 * dfs, get from the stack; if bfs, get from the queue.
	 * 
	 * @param wardenShore
	 * @return
	 */
	private Vertex getCurrentNode(boolean wardenShore) {
		if (wardenShore) {
			if (this.wardenMode.equalsIgnoreCase(GraphPartitioning.DFS_MODE)) {
				return this.wardenStack.peek();
			} else if (this.wardenMode
					.equalsIgnoreCase(GraphPartitioning.BFS_MODE)) {
				return this.wardenQueue.peek();
			} else if (this.wardenMode
					.equalsIgnoreCase(GraphPartitioning.INWARD_MODE)) {
				int updateCnt;
				Vertex currentNode = this.wardenInwardPriorityQueue.poll();
				while (currentNode.getNumberOfBlackNeighbors() != (updateCnt = this
						.countBlackNeighborsNumber(currentNode, true))) {
					currentNode.setNeighborSets(updateCnt);
					this.wardenInwardPriorityQueue.add(currentNode);
					currentNode = this.wardenInwardPriorityQueue.poll();
				}
				return currentNode;
			} else if (this.wardenMode
					.equalsIgnoreCase(GraphPartitioning.OUTWARD_MODE)) {
				int updateCnt;
				Vertex currentNode = this.wardenOutwardPriorityQueue.poll();
				while (currentNode.getNumberOfBlackNeighbors() != (updateCnt = this
						.countBlackNeighborsNumber(currentNode, true))) {
					currentNode.setNeighborSets(updateCnt);
					this.wardenOutwardPriorityQueue.add(currentNode);
					currentNode = this.wardenOutwardPriorityQueue.poll();
				}
				return currentNode;
			} else if (this.wardenMode
					.equalsIgnoreCase(GraphPartitioning.DEGREE_MODE)) {
				return this.wardenDegreePriorityQueue.poll();
			} else {
				/* invalid mode */
			}
		} else {
			if (this.oppositeMode.equalsIgnoreCase(GraphPartitioning.DFS_MODE)) {
				return this.oppositeStack.peek();
			} else if (this.oppositeMode
					.equalsIgnoreCase(GraphPartitioning.BFS_MODE)) {
				return this.oppositeQueue.peek();
			}
			if (this.oppositeMode
					.equalsIgnoreCase(GraphPartitioning.INWARD_MODE)) {
				int updateCnt;
				Vertex currentNode = this.oppositeInwardPriorityQueue.poll();
				while (currentNode.getNumberOfBlackNeighbors() != (updateCnt = this
						.countBlackNeighborsNumber(currentNode, true))) {
					currentNode.setNeighborSets(updateCnt);
					this.oppositeInwardPriorityQueue.add(currentNode);
					currentNode = this.oppositeInwardPriorityQueue.poll();
				}
				return currentNode;
			} else if (this.oppositeMode
					.equalsIgnoreCase(GraphPartitioning.OUTWARD_MODE)) {
				int updateCnt;
				Vertex currentNode = this.oppositeOutwardPriorityQueue.poll();
				while (currentNode.getNumberOfBlackNeighbors() != (updateCnt = this
						.countBlackNeighborsNumber(currentNode, true))) {
					currentNode.setNeighborSets(updateCnt);
					this.oppositeOutwardPriorityQueue.add(currentNode);
					currentNode = this.oppositeOutwardPriorityQueue.poll();
				}
				return currentNode;
			} else if (this.oppositeMode
					.equalsIgnoreCase(GraphPartitioning.DEGREE_MODE)) {
				return this.oppositeDegreePriorityQueue.poll();
			} else {
				/* invalid mode */
			}
		}
		return null;
	}

	/**
	 * when a node is not extendible, ie, it cannot do dfs or bfs from its
	 * adjacent node, then remove it from stack or queue.
	 * 
	 * @param wardenShore
	 */
	private void removeUnextendableNode(boolean wardenShore) {
		if (wardenShore) {
			if (this.wardenMode.equalsIgnoreCase(GraphPartitioning.DFS_MODE)) {
				this.wardenStack.pop();
			} else if (this.wardenMode
					.equalsIgnoreCase(GraphPartitioning.BFS_MODE)) {
				this.wardenQueue.poll();
			} else {
				/* invalid mode */
			}
		} else {
			if (this.oppositeMode.equalsIgnoreCase(GraphPartitioning.DFS_MODE)) {
				this.oppositeStack.pop();
			} else if (this.oppositeMode
					.equalsIgnoreCase(GraphPartitioning.BFS_MODE)) {
				this.oppositeQueue.poll();
			} else {
				/* invalid mode */
			}
		}
	}

	/**
	 * put the next node into corresponding search space, either the stack or
	 * the queue based on the searching type.
	 * 
	 * @param nextNode
	 * @param wardenShore
	 */
	private void addToSearchingSpace(Vertex nextNode, boolean wardenShore) {
		if (wardenShore) {
			if (this.wardenMode.equalsIgnoreCase(GraphPartitioning.DFS_MODE)) {
				this.wardenStack.push(nextNode);
			} else if (this.wardenMode
					.equalsIgnoreCase(GraphPartitioning.BFS_MODE)) {
				this.wardenQueue.add(nextNode);
			} else if (this.wardenMode
					.equalsIgnoreCase(GraphPartitioning.INWARD_MODE)) {
				this.wardenInwardPriorityQueue.add(nextNode);
			} else if (this.wardenMode
					.equalsIgnoreCase(GraphPartitioning.OUTWARD_MODE)) {
				this.wardenOutwardPriorityQueue.add(nextNode);
			} else if (this.wardenMode
					.equalsIgnoreCase(GraphPartitioning.DEGREE_MODE)) {
				this.wardenDegreePriorityQueue.add(nextNode);
			} else {
				/* invalid mode */
			}
		} else {
			if (this.oppositeMode.equalsIgnoreCase(GraphPartitioning.DFS_MODE)) {
				this.oppositeStack.push(nextNode);
			} else if (this.oppositeMode
					.equalsIgnoreCase(GraphPartitioning.BFS_MODE)) {
				this.oppositeQueue.add(nextNode);
			} else if (this.oppositeMode
					.equalsIgnoreCase(GraphPartitioning.INWARD_MODE)) {
				this.oppositeInwardPriorityQueue.add(nextNode);
			} else if (this.oppositeMode
					.equalsIgnoreCase(GraphPartitioning.OUTWARD_MODE)) {
				this.oppositeOutwardPriorityQueue.add(nextNode);
			} else if (this.oppositeMode
					.equalsIgnoreCase(GraphPartitioning.DEGREE_MODE)) {
				this.oppositeDegreePriorityQueue.add(nextNode);
			} else {
				/* invalid mode */
			}
		}
	}

	/**
	 * pre assumption is that the given node is a part of warden set, if one of
	 * its neighbor is in the opposite shore, it is a separator. and return
	 * true. If none of its neighbor is, return false.
	 * 
	 * @param vertex
	 * @return
	 */
	private boolean separatorCheck(Vertex vertex) {
		for (Vertex neighbor : vertex.getAllNeighbors()) {
			if (this.oppositeGray.contains(neighbor)
					|| this.oppositeBlack.contains(neighbor)
					|| this.oppositeStack.contains(neighbor)
					|| this.oppositeQueue.contains(neighbor)
					|| this.oppositeInwardPriorityQueue.contains(neighbor)
					|| this.oppositeOutwardPriorityQueue.contains(neighbor)
					|| this.oppositeDegreePriorityQueue.contains(neighbor)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * check if the given node is in the opposite shore, or in the expanding
	 * oppoiste shore
	 * 
	 * @return
	 */
	private boolean neighborCheck(Vertex neighbor) {

		if (this.oppositeInwardPriorityQueue.contains(neighbor)
				|| this.oppositeOutwardPriorityQueue.contains(neighbor)
				|| this.oppositeDegreePriorityQueue.contains(neighbor)
				|| this.oppositeBlack.contains(neighbor)
				|| this.oppositeGray.contains(neighbor)) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Select separators from warden gray set which must not be adjacent to
	 * opposite gray or black
	 * 
	 * for random - random search
	 */
	private void checkWardenGray() {

		for (Vertex gray : this.wardenGray) {
			for (Vertex neighbor : gray.getAllNeighbors()) {
				if (this.oppositeGray.contains(neighbor)
						|| this.oppositeBlack.contains(neighbor)) {
					this.separatorSet.add(gray);
					break;
				}
			}
		}
	}

	/**
	 * remove the separator that only connects to other separators and opposite
	 * vertexes.
	 * 
	 * for random - random search
	 */
	// private void filterSeparatorSet() {
	// Set<Vertex> removedSeparator = new HashSet<Vertex>();
	// for (Vertex vertex : this.separatorSet) {
	// boolean toRemove = true;
	// for (Vertex neighbor : vertex.getAllNeighbors()) {
	// /*
	// * if connect to a vertex that is not in separator set or opposite
	// (extending) set
	// * then it is a real separator, and don't put into removedSeparator set
	// */
	// if (!(this.separatorSet.contains(neighbor) ||
	// this.oppositeGray.contains(neighbor)
	// || this.oppositeBlack.contains(neighbor) ||
	// this.oppositeStack.contains(neighbor)
	// || this.oppositeQueue.contains(neighbor) ||
	// this.oppositeInwardPriorityQueue.contains(neighbor)
	// || this.oppositeDegreePriorityQueue.contains(neighbor)
	// || this.oppositeOutwardPriorityQueue.contains(neighbor))) {
	// toRemove = false;
	// break;
	// }
	// }
	// if (toRemove) {
	// removedSeparator.add(vertex);
	// }
	// }
	// if (!removedSeparator.isEmpty()) {
	// this.separatorSet.removeAll(removedSeparator);
	// this.filteredSeparators.addAll(removedSeparator);
	// }
	// }

	/**
	 * count the number of neighbors which are in the black warden set, which
	 * the search is based on, then update for that node and add into the
	 * priority queue
	 * 
	 * @param node
	 */
	private void priorityQueueAdd(Vertex node) {
		int blackCnt = this.countBlackNeighborsNumber(node, true);
		node.setNeighborSets(blackCnt);
		if (this.wardenMode.equalsIgnoreCase(GraphPartitioning.INWARD_MODE)) {
			this.wardenInwardPriorityQueue.add(node);
		} else if (this.wardenMode
				.equalsIgnoreCase(GraphPartitioning.OUTWARD_MODE)) {
			this.wardenOutwardPriorityQueue.add(node);
		} else if (this.wardenMode
				.equalsIgnoreCase(GraphPartitioning.DEGREE_MODE)) {
			this.wardenDegreePriorityQueue.add(node);
		} else {
			/* invalid mode */
		}
	}

	/**
	 * count the number of neighbor nodes which are in the black warden set.
	 * 
	 * @param Node
	 * @return
	 */
	private int countBlackNeighborsNumber(Vertex node, boolean isWarden) {
		int blackCnt = 0;
		if (isWarden) {
			for (Vertex neighbor : node.getAllNeighbors()) {
				if (this.wardenBlack.contains(neighbor)
						|| this.wardenSet.contains(neighbor)
						|| this.separatorSet.contains(neighbor)) {
					/*
					 * black cnt include nodes in the black set as well as the
					 * separator because these nodes cannot be extended any more
					 */
					++blackCnt;
				}
			}
		} else {
			for (Vertex neighbor : node.getAllNeighbors()) {
				if (this.oppositeBlack.contains(neighbor)
						|| this.separatorSet.contains(neighbor)) {
					++blackCnt;
				}
			}
		}
		return blackCnt;
	}

	/**
	 * create an adjacent set for the initial warden set, using which to start
	 * the algorithm.
	 * 
	 * for both random - random search and DFS - DFS search
	 */
	@SuppressWarnings("unchecked")
	private void createWardenAdjacentSet() {
		for (Vertex wardenVertex : this.wardenSet) {
			for (Vertex wardenNeighbor : wardenVertex.getAllNeighbors()) {
				if (!this.wardenSet.contains(wardenNeighbor)
						&& !this.wardenGray.contains(wardenNeighbor)) {
					/* create avaiable neighbor list for dfs search */
					wardenNeighbor.createAvailableNeighborList();
					this.wardenGray.add(wardenNeighbor);
					this.wardenStack.push(wardenNeighbor);
					this.wardenQueue.add(wardenNeighbor);
					this.priorityQueueAdd(wardenNeighbor);

					this.neutralVertexMap.remove(wardenNeighbor.getVertexID());
				}
			}
		}
		/* initialize the initial searching space */
		Collections.shuffle(this.wardenStack);
		Collections.shuffle((List<Vertex>) this.wardenQueue);
	}

	/**
	 * select a random vertex from neutral vertex set, and put it into the
	 * opposite extending set for a new round.
	 * 
	 * @return -1 if cannot find any vertex to extend for the next round 1 if
	 *         randomly find a vertex to extend for the next round
	 */
	private int randomSelectNextSeed() {
		/* graph petitioning process finishes */
		if (this.neutralVertexMap.size() == 0)
			return -1;

		Object[] keyArray = this.neutralVertexMap.keySet().toArray();
		int randomIndex = this.randomNext.nextInt(keyArray.length);
		int randomVertex = (Integer) keyArray[randomIndex];
		this.oppositeGray.add(this.neutralVertexMap.get(randomVertex));
		this.oppositeBlack.add(this.neutralVertexMap.get(randomVertex));

		this.oppositeStack.push(this.neutralVertexMap.get(randomVertex));
		this.oppositeQueue.add(this.neutralVertexMap.get(randomVertex));
		Vertex node = this.neutralVertexMap.get(randomVertex);
		node.setNeighborSets(this.countBlackNeighborsNumber(node, false));
		this.oppositeInwardPriorityQueue.add(node);
		this.oppositeOutwardPriorityQueue.add(node);
		this.oppositeDegreePriorityQueue.add(node);

		this.neutralVertexMap.remove(randomVertex);
		if (Constants.SEP_DEBUG) {
			System.out.println("opposite RANDOM select " + randomVertex);
		}

		return 1;
	}

	/**
	 * read data from the files, create a neutral vertex set then create a
	 * warden vertex set which is not included in the neutral vertex set.
	 * 
	 * @param asRelFile
	 * @param wardenFile
	 * @throws IOException
	 */
	private void generateGraph(String wardenFile) throws IOException {

		String pollString;
		StringTokenizer pollToks;
		int lhsASN, rhsASN, rel;

		if (Constants.SEP_DEBUG) {
			System.out.println("as file " + Constants.AS_REL_FILE);
		}
		BufferedReader fBuff = new BufferedReader(new FileReader(
				Constants.AS_REL_FILE));
		while (fBuff.ready()) {
			pollString = fBuff.readLine().trim();
			if (Constants.SEP_DEBUG) {
				System.out.println(pollString);
			}

			/*
			 * ignore blanks
			 */
			if (pollString.length() == 0) {
				continue;
			}

			/*
			 * Ignore comments
			 */
			if (pollString.charAt(0) == '#') {
				continue;
			}

			/*
			 * Parse line
			 */
			pollToks = new StringTokenizer(pollString, "|");
			lhsASN = Integer.parseInt(pollToks.nextToken());
			rhsASN = Integer.parseInt(pollToks.nextToken());
			rel = Integer.parseInt(pollToks.nextToken());

			/*
			 * create vertex object if we've never encountered it before and
			 * create neighbor relation for both vertexes
			 */

			if (!this.neutralVertexMap.containsKey(lhsASN)) {
				this.neutralVertexMap.put(lhsASN, new Vertex(lhsASN));
			}
			if (!this.neutralVertexMap.containsKey(rhsASN)) {
				this.neutralVertexMap.put(rhsASN, new Vertex(rhsASN));
			}
			this.neutralVertexMap.get(lhsASN).addNeighbor(
					this.neutralVertexMap.get(rhsASN));
			this.neutralVertexMap.get(rhsASN).addNeighbor(
					this.neutralVertexMap.get(lhsASN));
		}
		fBuff.close();
		if (Constants.SEP_DEBUG) {
			System.out.println(this.neutralVertexMap.size());
		}
		/*
		 * read the warden AS file, add wardens into warden set
		 */
		System.out.println("wardenFile " + wardenFile);
		fBuff = new BufferedReader(new FileReader(wardenFile));
		while (fBuff.ready()) {
			pollString = fBuff.readLine().trim();
			if (pollString.length() > 0) {
				int asn = Integer.parseInt(pollString);
				this.wardenSet.add(this.neutralVertexMap.get(asn));
				this.neutralVertexMap.remove(asn);
			}
		}
		fBuff.close();
	}

	/**
	 * using bfs for searching the whole warden shore nodes.
	 * 
	 * @return true if the vertex separators pass the test false if there are
	 *         bugs in the code..
	 */
	private boolean passSeparatorTest() {
		Set<Vertex> visitedSet = new HashSet<Vertex>();
		Set<Vertex> wardenShore = new HashSet<Vertex>(this.getRedundantWardenShore());
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
					} else if (wardenShore.contains(nextNode)) {
						testQueue.add(nextNode);
						visitedSet.add(nextNode);
					} else {
						System.out.println("Test Failed!!!");
						return false;
					}
				}
			}
		}
		if (!Constants.SEP_DEBUG) {
			System.out.println("Test Passed!!!");
		}
		return true;
	}

	/**
	 * remove the connected component of warden shore that do not contains any
	 * wardens, and remove the separators that are adjacent to them and if they
	 * are not adjacent to other warden shores?
	 */
	private void removeRedundantComponents() {
		// Set<Vertex> wardenShore = new HashSet<Vertex>(this.getWardenShore());
		Set<Vertex> visitedSet = new HashSet<Vertex>();
		boolean containsWarden;
		Queue<Vertex> que = new LinkedList<Vertex>();
		Set<Vertex> tempSet = new HashSet<Vertex>();
		Set<Vertex> tempSeparatorSet = new HashSet<Vertex>();

		if (!Constants.SEP_DEBUG) {
			System.out.println("all components: " + this.separatorSet.size()
					+ ", " + this.getRedundantWardenShore().size());
		}
		for (Vertex node : this.getRedundantWardenShore()) {
			if (!visitedSet.contains(node)) {
				/* reset helper variables */
				containsWarden = false;
				tempSet.clear();
				tempSeparatorSet.clear();

				visitedSet.add(node);
				que.add(node);
				tempSet.add(node);
				/* bfs search */
				while (!que.isEmpty()) {
					Vertex currentNode = que.poll();
					if (this.wardenSet.contains(currentNode)) {
						containsWarden = true;
					}
					for (Vertex neighbor : currentNode.getAllNeighbors()) {
						if (!visitedSet.contains(neighbor)) {
							visitedSet.add(neighbor);
							if (this.separatorSet.contains(neighbor)) {
								tempSeparatorSet.add(neighbor);
							} else {
								que.add(neighbor);
								tempSet.add(neighbor);
							}
						}
					}
				}
				/*
				 * add the components contain at least one warden into the final
				 * warden shore, and add their separators into the final
				 * separator set
				 */
				if (containsWarden) {
					this.validSeparators.addAll(tempSeparatorSet);
					this.validWardenShore.addAll(tempSet);
				}
			}
		}
	}

	/**
	 * fetch only the wardens in the warden shore after the algorithm,
	 * 
	 * @return
	 */
	public Set<Vertex> getWardens() {
		return this.wardenSet;
	}

	/**
	 * fetch the vertexes in the black warden set, wardens are not included.
	 * remove redundant operation is done.
	 * 
	 * @return
	 */
	public Set<Vertex> getWardenShore() {
		return this.validWardenShore;
	}

	/**
	 * fetch the vertexes in the black warden set, wardens are not included.
	 * this set is very redundant.
	 * 
	 * @return
	 */
	private Set<Vertex> getRedundantWardenShore() {
		Set<Vertex> wardenShore = new HashSet<Vertex>();
		wardenShore.addAll(this.wardenBlack);
		wardenShore.addAll(this.wardenSet);
		/* for random case */
		wardenShore.addAll(this.wardenGray);
		return wardenShore;
	}

	public Set<Vertex> getSeparators() {
		return this.validSeparators;
	}

	public int getSeparatorSize() {
		return this.validSeparators.size();
	}

	public Set<Vertex> getOppositeShore() {
		Set<Vertex> oppositeShore = new HashSet<Vertex>(
				this.neutralVertexCopy.values());
		oppositeShore.removeAll(this.validSeparators);
		oppositeShore.removeAll(this.validWardenShore);

		return oppositeShore;
	}

	private void printResults() throws IOException {

		this.separatorOut.write(this.validSeparators.size() + "\n");
		this.wardenOut.write(this.validWardenShore.size() + "\n");
		if (Constants.SEP_DEBUG) {
			System.out.println("separator size: " + this.validSeparators.size()
					+ ", warden size: " + this.validWardenShore.size());
			/*
			 * System.out.println("separator size: " + this.separatorSet.size()
			 * + ", warden size: " +
			 * (this.wardenSet.size()+this.wardenBlack.size()));
			 */
		}
		if (Constants.SEP_DEBUG) {
			System.out.println("\n###Separators:");
			for (Vertex v : this.separatorSet) {
				System.out.print(v.getVertexID() + ", ");
			}
			System.out.println("\nWarden Gray:");
			for (Vertex v : this.wardenGray) {
				System.out.print(v.getVertexID() + ", ");
			}
			System.out.println("\nWarden Black:");
			for (Vertex v : this.wardenBlack) {
				System.out.print(v.getVertexID() + ", ");
			}
			System.out.println("\nOpposite Gray:");
			for (Vertex v : this.oppositeGray) {
				System.out.print(v.getVertexID() + ", ");
			}
			System.out.println("\nOpposite Black:");
			for (Vertex v : this.oppositeBlack) {
				System.out.print(v.getVertexID() + ", ");
			}
		}
	}
}
