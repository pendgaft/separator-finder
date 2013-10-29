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
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.Stack;
import java.util.StringTokenizer;

import graph.Vertex;

public class GraphPartitioning {

	private static final String RAND_MODE = "random";
	private static final String DFS_MODE = "dfs";
	
	/** store initial wardens */
	private Set<Vertex> wardenSet;
	/** store vertexes that must be in warden side */
	private List<Vertex> wardenBlack;
	/** store the extending vertexes that are adjacent to wardens, which would become separators */
	private List<Vertex> wardenGray;
	/** store vertexes that must be in opposite side */
	private Set<Vertex> oppositeBlack;
	/** store the extending vertexes of the opposite side, might include those will be in the opposite set */
	private List<Vertex> oppositeGray;
	/** store vertexes that are neither in warden side nor opposite side yet */
	private HashMap<Integer, Vertex> neutralVertexMap;
	/** store real separators */
	private Set<Vertex> separatorSet;
	/** write the number of separators into a file */
	private BufferedWriter separatorOut;
	/** write the number of wardens in the warden set into a file */
	private BufferedWriter wardenOut;
	Random randomNext;
	
	private Stack<Vertex> wardenStack;
	private Stack<Vertex> oppositeStack;
	
	private String wardenMode;
	private String oppositeMode;
	
	public GraphPartitioning() throws IOException {
		this.wardenSet = new HashSet<Vertex>();
		this.wardenBlack = new ArrayList<Vertex>();
		this.wardenGray = new ArrayList<Vertex>();
		this.oppositeBlack = new HashSet<Vertex>();
		this.oppositeGray = new ArrayList<Vertex>();
		this.neutralVertexMap = new HashMap<Integer, Vertex>();
		this.separatorSet = new HashSet<Vertex>();
		
		this.randomNext = new Random();
		
		this.wardenStack = new Stack<Vertex>();
		this.oppositeStack = new Stack<Vertex>();
	}
	
	public void multipleRuns(String wardenFile, String wardenMode, String oppositeMode, int trials) throws IOException {
		this.wardenMode = wardenMode;
		this.oppositeMode = oppositeMode;
		//this.separatorOut = new BufferedWriter(new FileWriter(Constants.SEPARATOR_OUTPUT_FILE + "-" + wardenFile));
		//this.wardenOut = new BufferedWriter(new FileWriter(Constants.WARDEN_OUTPUT_FILE + "-" + wardenFile));
		this.separatorOut = new BufferedWriter(new FileWriter("multiSeparatorCnt.txt"));
		this.wardenOut = new BufferedWriter(new FileWriter("multiSeparatorCnt.txt"));
		
		this.generateGraph(wardenFile);
		HashMap<Integer, Vertex> tempMap = new HashMap<Integer, Vertex>();
		for (int key: this.neutralVertexMap.keySet()) {
			tempMap.put(key, this.neutralVertexMap.get(key));
		}
		
		for (int i = 0; i < 10; ++i) {
			if (i % (trials/10) == 0) {
				System.out.println(100*i/trials + "% done!");				
			}
			this.reset(tempMap);
			if (this.wardenMode.equalsIgnoreCase(GraphPartitioning.RAND_MODE)
					&& this.oppositeMode.equalsIgnoreCase(GraphPartitioning.RAND_MODE)) {
				this.randomRandomPartitioning();
			} else if (this.wardenMode.equalsIgnoreCase(GraphPartitioning.DFS_MODE)
					&& this.oppositeMode.equalsIgnoreCase(GraphPartitioning.DFS_MODE)) {
				this.DFSDFSPartitioning();
			} else {
				/* under construction.. */
			}
			this.printResults();
		}
		this.separatorOut.close();
		this.wardenOut.close();
	}
	
	public void singleRun(String wardenFile, String wardenMode, String oppositeMode) throws IOException {
		
		this.wardenMode = wardenMode;
		this.oppositeMode = oppositeMode;
		this.separatorOut = new BufferedWriter(new FileWriter(Constants.SEPARATOR_OUTPUT_FILE + "-" + wardenFile));
		this.wardenOut = new BufferedWriter(new FileWriter(Constants.WARDEN_OUTPUT_FILE + "-" + wardenFile));
		//this.separatorOut = new BufferedWriter(new FileWriter("tmp1.txt"));
		//this.wardenOut = new BufferedWriter(new FileWriter("tmp2.txt"));
		this.generateGraph(wardenFile);
		
		if (this.wardenMode.equalsIgnoreCase(GraphPartitioning.RAND_MODE)
				&& this.wardenMode.equalsIgnoreCase(GraphPartitioning.RAND_MODE)) {
			this.randomRandomPartitioning();
		} else if (this.oppositeMode.equalsIgnoreCase(GraphPartitioning.DFS_MODE)
				&& this.oppositeMode.equalsIgnoreCase(GraphPartitioning.DFS_MODE)) {
			for (Vertex node : this.neutralVertexMap.values()) {
				node.createAvailableNeighborList();
			}
			this.DFSDFSPartitioning();
		} else {
			/* under construction.. */
		}
		this.printResults();
		this.separatorOut.close();
		this.wardenOut.close();
	}
		
	private void reset(HashMap<Integer, Vertex> neutralVertexMap) {
		if (Constants.DEBUG) {
			System.out.println(this.wardenSet.size() + ", " + this.oppositeBlack.size() + ", " 
					+ this.wardenGray.size() + ", " + this.oppositeGray.size() + ", " +
					", " + this.separatorSet.size());
		}
		this.wardenBlack.clear();
		this.wardenGray.clear();
		this.oppositeBlack.clear();
		this.oppositeGray.clear();
		this.separatorSet.clear();
		this.neutralVertexMap.clear();
		
		this.oppositeStack.clear();
		for (int key : neutralVertexMap.keySet()) {
			this.neutralVertexMap.put(key, neutralVertexMap.get(key));
			/* available neighbor list is used for random dfs search */
			this.neutralVertexMap.get(key).createAvailableNeighborList();
		}
	}
	
	/**
	 * petition the graph and find the separators
	 * using oppositeAddInThisTurn to make sure in each turn, both sides
	 * add one and only one vertex if any is available.
	 * 
	 * both the ways warden and opposite sides extend the set randomly
	 */
	private void randomRandomPartitioning() {
		this.createWardenAdjacentSet();
		boolean partitioningDone = false;
		while (!partitioningDone) {

			/* 
			 * when there is no vertex to extend for warden gray set,
			 * all possible separators are found. 
			 */
			if (this.randomExtendWardenShore()) {
				partitioningDone = true;
			}
			
			if (this.randomExtendOppositeShore()) {
				if (this.randomSelectNextSeed() == -1) {
					partitioningDone = true;
				}
			}
			if (Constants.DEBUG) {
				System.out.println("******");
			}
		}
		
		this.checkWardenGray();
		this.filterSeparatorSet();
	}

	/**
	 * randomly select ONE extendible vertex from warden adjacent set
	 * put into warden set, relax its neighbors and update warden 
	 * adjacent set for the next round.
	 * @return 	true   if warden gray set is empty
	 * 			false  if warden gray set is not empty
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
				/* if a node in gray set is adjacent to opposite , it is a separator */
				if (this.oppositeGray.contains(nextNode) || this.oppositeBlack.contains(nextNode)) {
					isSeparator = true;
				}
				if (this.neutralVertexMap.containsKey(nextNode.getVertexID())) {
					this.wardenGray.add(nextNode);
					this.neutralVertexMap.remove(nextNode.getVertexID());
					++cntToBeGray;
					
					if (Constants.DEBUG) {
						System.out.println("warden set added: " + nextNode.getVertexID());
					}
				}
			}
			/*
			 * if it is an interior separator, need to keep on finding next one to extend;
			 * if it is not an interior separator, stop finding next one, and finish this turn
			 * 
			 * if it is not a separator
			 * 		if it is an interior node, keep finding, otherwise, stop and return
			 */
			if (isSeparator) {
				this.separatorSet.add(currentNode);
				this.wardenGray.remove(currentNode);
				if (cntToBeGray != 0) {
					notFindNextNode = false;
				}
			} else if (cntToBeGray == 0) {
				this.wardenBlack.add(currentNode);
				this.wardenGray.remove(currentNode);
			} else {
				notFindNextNode = false;
			}
		}
		return false;
	}
	
	/**
	 * randomly select ONE extendible vertex that adjacent to the vertexes
	 * in opposite extending set.
	 * 
	 * if a vertex in opposite extending set is not extendible, put it into
	 * opposite set.
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
					
					if (Constants.DEBUG) {
						System.out.println("opposit set added: " + nextNode.getVertexID());
					}
				}
			}
			if (cntToBeGray == 0) {
				this.oppositeBlack.add(currentNode);
				this.oppositeGray.remove(currentNode);
				continue;
			}
			notFindNextOpposite = false;
		}
		return false;
	}
	
	public void singleDFSRun(String asRelFile, String wardenFile) throws IOException {
		
		//this.separatorOut = new BufferedWriter(new FileWriter("DFSDFS_" + Constants.SEPARATOR_OUTPUT_FILE + "-" + wardenFile));
		//this.wardenOut = new BufferedWriter(new FileWriter("DFSDFS_" + Constants.WARDEN_OUTPUT_FILE + "-" + wardenFile));
		this.separatorOut = new BufferedWriter(new FileWriter("singleSeparatorCnt.txt"));
		this.wardenOut = new BufferedWriter(new FileWriter("singleWardenCnt.txt"));
		
		this.generateGraph(wardenFile);
		for (Vertex node : this.neutralVertexMap.values()) {
			node.createAvailableNeighborList();
		}
		this.DFSDFSPartitioning();
		this.printResults();
		
		this.separatorOut.close();
		this.wardenOut.close();
	}
	
	private void DFSDFSPartitioning() {
		this.createWardenAdjacentSet();
		boolean partitioningDone = false;
		while (!partitioningDone) {
			/* extend a node from warden shore */
			if (this.randomDFSExtendWardenShore()) {
				partitioningDone = true;
			}
			
			/* extend a node from opposite shore */
			if (this.randomDFSExtendOppositeShore()) {
				if (this.randomSelectNextSeed() == -1) {
					partitioningDone = true;
				}					
			}
			
			if (Constants.DEBUG) {
				System.out.println("******");
			}
		}
		this.filterSeparatorSet();
	}
	
	/**
	 * every time traverse to a same node again, it has to scan all its
	 * neighbors. Not efficient, but this will avoid extra storage for 
	 * the searching space and avoid reinitialize that storage for all
	 * the nodes which might be expensive. A little tradeoff?? 
	 * @return  true   if search is  finished.
	 * 			false  if search is not finished.
	 */
	private boolean randomDFSExtendWardenShore() {		
		boolean thisRoundDone = false;		
		while (!thisRoundDone) {
			/* when the stack is empty, partitioning is done. */
			if (this.wardenStack.empty()) {
				return true;
			}
			boolean currentSearchingSpaceEmpty = false;
			Vertex nextNode = null;
			Vertex currentNode = this.wardenStack.peek();
			/* randomly select a neighbor for the next node */
			do {
				nextNode = currentNode.randomSelectANeighbor(this.randomNext);
				if (nextNode == null) {
					currentSearchingSpaceEmpty = true;
					break;
				}
			} while (!this.neutralVertexMap.containsKey(nextNode.getVertexID()));
						
			/* 
			 * if cannot extend from the current node, 
			 * pop it out of the stack, and check the next 
			 * node on the top of the stack 
			 */
			if (currentSearchingSpaceEmpty) {
				this.wardenStack.pop();
				/* check if current is a separator, and put it in the right place */
				if (separatorCheck(currentNode)) {
					this.separatorSet.add(currentNode);
					
					if (Constants.DEBUG) {
						System.out.println("@dfs add separator " + currentNode.getVertexID());
					}
				} else {
					this.wardenBlack.add(currentNode);
				}
			} else {
				this.neutralVertexMap.remove(nextNode.getVertexID());
				this.wardenStack.push(nextNode);
				thisRoundDone = true;
				
				if (Constants.DEBUG) {
					System.out.println("warden add " + nextNode.getVertexID());
				}
			}
		}
		return false;
	}
	
	/**
	 * @return  true 	if not successfully extend a next node, 
	 * 					and need to randomly select a seed.
	 * 		 	false	if successfully extend a next node,
	 */
	private boolean randomDFSExtendOppositeShore() {		
		boolean thisRoundDone = false;
		/* if the stack is empty, needs to select next random seed */
		while (!thisRoundDone) {
			if (this.oppositeStack.empty()) {
				return true;
			}
			boolean currentSearchingSpaceEmpty = false;
			Vertex nextNode = null;
			Vertex currentNode = this.oppositeStack.peek();
			/* randomly select a neighbor for the next node */
			do {
				nextNode = currentNode.randomSelectANeighbor(this.randomNext);
				if (nextNode == null) {
					currentSearchingSpaceEmpty = true;
					break;
				}
			} while (!this.neutralVertexMap.containsKey(nextNode.getVertexID()));

			if (currentSearchingSpaceEmpty) {
				this.oppositeStack.pop();
				this.oppositeBlack.add(currentNode);
			} else {
				this.neutralVertexMap.remove(nextNode.getVertexID());
				this.oppositeStack.push(nextNode);
				thisRoundDone = true;
				if (Constants.DEBUG) {
					System.out.println("opposite add " + nextNode.getVertexID());
				}
			}
		}
		return false;
	}
	
	/**
	 * pre assumption is that the given node is a part of warden set,
	 * if one of its neighbor is in the opposite shore, it is a separator.
	 * and return true. If none of its neighbor is, return false.
	 * @param vertex
	 * @return
	 */
	private boolean separatorCheck(Vertex vertex) {
		for (Vertex neighbor : vertex.getAllNeighbors()) {
			if (this.oppositeGray.contains(neighbor) || this.oppositeBlack.contains(neighbor) 
					|| this.oppositeStack.contains(neighbor)) {
				return true;
			}
		}
		return false;
	}
		
	/**
	 * Select separators from warden gray set
	 * which must not be adjacent to opposite gray or black   
	 * 
	 * for random - random search
	 */
	private void checkWardenGray() {
		
		for (Vertex gray : this.wardenGray) {
			for (Vertex neighbor : gray.getAllNeighbors()) {
				if (this.oppositeGray.contains(neighbor) || this.oppositeBlack.contains(neighbor)) {
					this.separatorSet.add(gray);
					break;
				}					
			}
		}
	}
	
	/**
	 * remove the separator that only connects to other separators 
	 * and opposite vertexes.
	 * 
	 * for random - random search
	 */
	private void filterSeparatorSet() {
		Set<Vertex> removedSeparator = new HashSet<Vertex>();
		for (Vertex vertex : this.separatorSet) {
			boolean toRemove = true;
			for (Vertex neighbor : vertex.getAllNeighbors()) {
				/*
				 * if connect to a vertex that is not in separator set or opposite (extending) set
				 * then it is a real separator, and don't put into removedSeparator set
				 */
				if (!(this.separatorSet.contains(neighbor) || this.oppositeGray.contains(neighbor)
						|| this.oppositeBlack.contains(neighbor) || this.oppositeStack.contains(neighbor))) {
					toRemove = false;
					break;
				}				
			}
			if (toRemove) {
				removedSeparator.add(vertex);
			}
		}
		if (!removedSeparator.isEmpty()) {
			this.separatorSet.removeAll(removedSeparator);
		}
	}
	
	/**
	 * create an adjacent set for the initial warden set, using which to 
	 * start the algorithm.
	 * 
	 * for both random - random search and DFS - DFS search
	 */
	private void createWardenAdjacentSet() {
		for (Vertex wardenVertex : this.wardenSet) {
			for (Vertex wardenNeighbor : wardenVertex.getAllNeighbors()) {
				if (!this.wardenSet.contains(wardenNeighbor) && !this.wardenGray.contains(wardenNeighbor)) {
					/* create avaiable neighbor list for dfs search */
					wardenNeighbor.createAvailableNeighborList();
					this.wardenGray.add(wardenNeighbor);
					this.wardenStack.push(wardenNeighbor);
					
					this.neutralVertexMap.remove(wardenNeighbor.getVertexID());
				}
			}
		}
		/* randomize the warden stack */
		Collections.shuffle(this.wardenStack);
	}
	
	/** 
	 * select a random vertex from neutral vertex set,
	 * and put it into the opposite extending set for a new round.
	 * 
	 * @return -1 if cannot find any vertex to extend for the next round
	 * 			1 if randomly find a vertex to extend for the next round
	 */
	private int randomSelectNextSeed() {
		/* graph petitioning process finishes */
		if (this.neutralVertexMap.size() == 0)
			return -1;
		
		Object[] keyArray = this.neutralVertexMap.keySet().toArray();
		int randomIndex = this.randomNext.nextInt(keyArray.length);
		int randomVertex = (Integer)keyArray[randomIndex];
		this.oppositeGray.add(this.neutralVertexMap.get(randomVertex));
		
		this.oppositeStack.push(this.neutralVertexMap.get(randomVertex));
		
		this.neutralVertexMap.remove(randomVertex);
		if (Constants.DEBUG) {
			System.out.println("opposite RANDOM select " + randomVertex);
		}
		
		return 1;
	}
	
	/**
	 * read data from the files, create a neutral vertex set
	 * then create a warden vertex set which is not included in
	 * the neutral vertex set.
	 * @param asRelFile
	 * @param wardenFile
	 * @throws IOException
	 */
	private void generateGraph(String wardenFile)
			throws IOException {

		String pollString;
		StringTokenizer pollToks;
		int lhsASN, rhsASN, rel;

		//if (Constants.DEBUG) {
			System.out.println("as file " + Constants.AS_REL_FILE);
		//}
		BufferedReader fBuff = new BufferedReader(new FileReader(Constants.AS_REL_FILE));
		while (fBuff.ready()) {
			pollString = fBuff.readLine().trim();
			if (Constants.DEBUG) {
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
			 * create vertex object if we've never encountered it before
			 * and create neighbor relation for both vertexes
			 */
			
			if (!this.neutralVertexMap.containsKey(lhsASN)) {
				this.neutralVertexMap.put(lhsASN, new Vertex(lhsASN));
			}
			if (!this.neutralVertexMap.containsKey(rhsASN)) {
				this.neutralVertexMap.put(rhsASN, new Vertex(rhsASN));
			}
			this.neutralVertexMap.get(lhsASN).addNeighbor(this.neutralVertexMap.get(rhsASN));
			this.neutralVertexMap.get(rhsASN).addNeighbor(this.neutralVertexMap.get(lhsASN));
		}
		fBuff.close();
		if (Constants.DEBUG) {
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
	
	public Set<Vertex> getSeparatorSet() {
		return this.separatorSet;
	}
	public int getSeparatorSize() {
		return this.separatorSet.size();
	}
	
	/**
	 * fetch only the wardens in the warden shore after the algorithm,
	 * @return
	 */
	public Set<Vertex> getWardenSet() {
		return this.wardenSet;
	}
	
	/**
	 * fetch the vertexes in the black warden set, wardens are not included
	 * @return
	 */
	public List<Vertex> getBlackWardenSet() {
		return this.wardenBlack;
	}
	
	private void printResults() throws IOException {
		
		this.separatorOut.write(this.separatorSet.size()+"\n");
		this.wardenOut.write((this.wardenBlack.size() + this.wardenSet.size()) +"\n");
		System.out.println(this.separatorSet.size() + ", " + this.wardenBlack.size());
		if (Constants.DEBUG) {
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
