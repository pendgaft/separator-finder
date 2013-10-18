package work;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.StringTokenizer;

import sim.Constants;
import graph.Vertex;

public class GraphPetitioning {

	/** store initial wardens */
	private Set<Vertex> wardenSet;
	/** store the extending vertexes that are adjacent to wardens, which would become separators */
	private List<Vertex> wardenAdjacentSet;
	/** store vertexes that must be in opposite side */
	private Set<Vertex> oppositeSet;
	/** store the extending vertexes of the opposite side, might include those will be in the opposite set */
	private List<Vertex> oppositeExtendingSet;
	/** store vertexes that are neither in warden side nor opposite side yet */
	private HashMap<Integer, Vertex> neutralVertexMap;
	/** store both separators and interior vertexes of the warden side, 
	 * and the interior vertexes will be filtered */
	private Set<Vertex> possibleSeparatorSet;
	/** store real separators */
	private Set<Vertex> separatorSet;
	/** make sure if warden side and opposite side extend one and only one vertex each time */
	private boolean oppositeAddedInThisTurn; 
	
	public GraphPetitioning() {
		this.wardenSet = new HashSet<Vertex>();
		this.wardenAdjacentSet = new ArrayList<Vertex>();
		this.oppositeSet = new HashSet<Vertex>();
		this.oppositeExtendingSet = new ArrayList<Vertex>();
		
		this.neutralVertexMap = new HashMap<Integer, Vertex>();
		this.possibleSeparatorSet = new HashSet<Vertex>();
		this.separatorSet = new HashSet<Vertex>();
	}
	
	public void Run(String asRelFile, String wardenFile) throws IOException {
		this.generateGraph(asRelFile, wardenFile);
		
		this.petitionGraph();
		this.showResult();
	}
	
	/**
	 * petition the graph and find the separators
	 * using oppositeAddInThisTurn to make sure in each turn, both sides
	 * add one and only one vertex if any is available.
	 */
	private void petitionGraph() {
		int cntWardenValidAdjacent;
		this.createWardenAdjacentSet();
		this.randomSelectNextSeed();
		this.oppositeAddedInThisTurn = true;
		
		while (true) {
			cntWardenValidAdjacent = randomExtendWardenAdjacent();
			if (!this.oppositeAddedInThisTurn) {
				randomExtendOppositeAdjacent();
			}
			/* 
			 * when there is no vertex to extend for warden set,
			 * all possible separators are found. 
			 */
			if (cntWardenValidAdjacent == 0) {
				break;
			}
			/*
			 * if a current search for opposite set cannot extend,
			 * generate a new seed for another extend.
			 * if no seed is available, all possible separators are found.
			 */
			if (!this.oppositeAddedInThisTurn){
				if (this.randomSelectNextSeed() == -1)
					break;
			}
			this.oppositeAddedInThisTurn = false;
			if (Constants.DEBUG) {
				System.out.println("******");
			}
		}
		
		this.selectSeparatorSet();
		this.filterSeparatorSet();
	}
	
	/**
	 * randomly select ONE extendible vertex from warden adjacent set
	 * put into warden set, relax its neighbors and update warden 
	 * adjacent set for the next round.
	 * @return
	 */
	private int randomExtendWardenAdjacent() {
		int randomIndex;
		Random randomNext = new Random();
		List<Vertex> extendibleAdjacentSet = new ArrayList<Vertex>();
		while (true) {
			/* if cannot find any vertex to extend, petitioning finishes! */
			if (this.wardenAdjacentSet.isEmpty()) 
				return 0;
			
			randomIndex = randomNext.nextInt(this.wardenAdjacentSet.size());
			Vertex relaxedVertex = this.wardenAdjacentSet.get(randomIndex);
			
			for (Vertex relaxedAdj : relaxedVertex.getAllNeighbors()) {
				if (this.neutralVertexMap.containsKey(relaxedAdj.getVertexID())) {
					extendibleAdjacentSet.add(relaxedAdj);
				}
			}
			/* 
			 * if the vertex is not extendible, move into possible separator set
			 * as potential possible separator. 
			 */
			if (extendibleAdjacentSet.isEmpty()) {
				this.possibleSeparatorSet.add(relaxedVertex);				
				this.wardenAdjacentSet.remove(randomIndex);
				continue;
			}
			
			/* 
			 * there existing extendible vertexes, then randomly pick one from 
			 * the possible neighbors to extend the extending set for next round.
			 */
			randomIndex = randomNext.nextInt(extendibleAdjacentSet.size());
			Vertex extendedVertex = extendibleAdjacentSet.get(randomIndex);
			this.wardenAdjacentSet.add(extendedVertex);
			this.neutralVertexMap.remove(extendedVertex.getVertexID());
			if (Constants.DEBUG) {
				System.out.println("warden set added: " + extendedVertex.getVertexID());
			}
			break;
		}
		return this.wardenAdjacentSet.size();
	}
	
	/**
	 * randomly select ONE extendible vertex that adjacent to the vertexes
	 * in opposite extending set.
	 * 
	 * if a vertex in opposite extending set is not extendible, put it into
	 * opposite set.
	 * @return
	 */
	private void randomExtendOppositeAdjacent() {
		
		int randomIndex;
		Random randomNext = new Random();
		List<Vertex> extendibleAdjacentSet = new ArrayList<Vertex>();
		while (true) {
			/* if cannot find any vertex to extend, petitioning finishes! */
			if (this.oppositeExtendingSet.isEmpty()) 
				break;
			
			randomIndex = randomNext.nextInt(this.oppositeExtendingSet.size());
			Vertex relaxedVertex = this.oppositeExtendingSet.get(randomIndex);
			
			for (Vertex relaxedAdj : relaxedVertex.getAllNeighbors()) {
				if (this.neutralVertexMap.containsKey(relaxedAdj.getVertexID())) {
					extendibleAdjacentSet.add(relaxedAdj);
				}
			}
			/* 
			 * if the vertex is not extendible, move into opposite set
			 * as opposite vertexes that will not be searched again. 
			 */
			if (extendibleAdjacentSet.isEmpty()) {
				this.oppositeSet.add(relaxedVertex);
				this.oppositeExtendingSet.remove(randomIndex);
				continue;
			}
			
			/* 
			 * there existing extendible vertexes, then randomly pick one from 
			 * the possible neighbors to extend the extending set for next round.
			 */
			randomIndex = randomNext.nextInt(extendibleAdjacentSet.size());
			Vertex extendedVertex = extendibleAdjacentSet.get(randomIndex);
			this.oppositeExtendingSet.add(extendedVertex);
			this.neutralVertexMap.remove(extendedVertex.getVertexID());
			if (Constants.DEBUG) {
				System.out.println("opposite set added: " + extendedVertex.getVertexID());
			}
			this.oppositeAddedInThisTurn = true;
			break;
		}
	}
	
	/**
	 * if a possible separator is adjacent to an opposite vertex,
	 * then it is a true separator, not an interior vertex of warden set. 
	 */
	private void selectSeparatorSet() {
		Set<Vertex> tempSet = this.possibleSeparatorSet;
		tempSet.addAll(this.wardenAdjacentSet);
		for (Vertex vertex : tempSet) {
			boolean trueSeparator = false;
			for (Vertex neighbor : vertex.getAllNeighbors()) {
				if (this.oppositeSet.contains(neighbor) || this.oppositeExtendingSet.contains(neighbor)) {
					trueSeparator = true;
					break;
				}
			}
			if (trueSeparator) {
				this.separatorSet.add(vertex);
			}
		}
	}
	
	/**
	 * remove the separator that only connects to other separators 
	 * and opposite vertexes.
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
				if (!(this.separatorSet.contains(neighbor) || this.oppositeExtendingSet.contains(neighbor)
						|| this.oppositeSet.contains(neighbor))) {
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
	 */
	private void createWardenAdjacentSet() {
		for (Vertex wardenVertex : this.wardenSet) {
			for (Vertex wardenNeighbor : wardenVertex.getAllNeighbors()) {
				if (!this.wardenSet.contains(wardenNeighbor) && !this.wardenAdjacentSet.contains(wardenNeighbor)) {
					this.wardenAdjacentSet.add(wardenNeighbor);
					this.neutralVertexMap.remove(wardenNeighbor.getVertexID());
				}
			}
		}
	}
	
	/** 
	 * select a random vertex from neutral vertex set,
	 * and put it into the opposite extending set for a new round.
	 * 
	 * @return -1 if cannot find any vertex to extend for the next round
	 * 			1 if randomly find a vertex to extend for the next round
	 */
	private int randomSelectNextSeed() {
		/* random select a seed for the next round */
		Random randomNext = new Random();
		/* graph petitioning process finishes */
		if (this.neutralVertexMap.size() == 0)
			return -1;
		
		Object[] keyArray = this.neutralVertexMap.keySet().toArray();
		int randomIndex = randomNext.nextInt(keyArray.length);
		int randomVertex = (Integer)keyArray[randomIndex];
		this.oppositeExtendingSet.add(this.neutralVertexMap.get(randomVertex));
		this.oppositeSet.add(this.neutralVertexMap.get(randomVertex));
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
	private void generateGraph(String asRelFile, String wardenFile)
			throws IOException {

		String pollString;
		StringTokenizer pollToks;
		int lhsASN, rhsASN, rel;

		System.out.println(asRelFile);

		BufferedReader fBuff = new BufferedReader(new FileReader(asRelFile));
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
		
		/*
		 * read the warden AS file, add wardens into warden set
		 */
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

	private void showResult() {
		System.out.println("The number of the separator is " + this.separatorSet.size());
		
		if (Constants.DEBUG) {
			System.out.println("separators: ");
			for (Vertex vertex : this.separatorSet)
				System.out.print(vertex.getVertexID() + ", ");
			
			this.possibleSeparatorSet.removeAll(this.separatorSet);
			System.out.println("\nwarden set: ");
			for (Vertex vertex : this.possibleSeparatorSet)
				System.out.print(vertex.getVertexID() + ", ");
			
			System.out.println("\nopposite set: ");
			for (Vertex vertex : this.oppositeSet)
				System.out.print(vertex.getVertexID() + ", ");
			System.out.println("\nopposite extending set: ");
			for (Vertex vertex : this.oppositeExtendingSet)
				System.out.print(vertex.getVertexID() + ", ");
		}
	}
}
