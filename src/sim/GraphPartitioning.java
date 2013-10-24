package sim;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.StringTokenizer;

import graph.Vertex;

public class GraphPartitioning {

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
	/** make sure if warden side and opposite side extend one and only one vertex each time */
	private boolean oppositeExtendedInThisTurn; 
	private BufferedWriter Out;
	Random randomNext;
	
	public GraphPartitioning() throws IOException {
		this.wardenSet = new HashSet<Vertex>();
		this.wardenBlack = new ArrayList<Vertex>();
		this.wardenGray = new ArrayList<Vertex>();
		this.oppositeBlack = new HashSet<Vertex>();
		this.oppositeGray = new ArrayList<Vertex>();
		this.neutralVertexMap = new HashMap<Integer, Vertex>();
		this.separatorSet = new HashSet<Vertex>();
		
		this.randomNext = new Random();
	}
	
	public void Run(String asRelFile, String wardenFile, int trials) throws IOException {
		
		this.Out = new BufferedWriter(new FileWriter(Constants.OUTPUT_FILE + "-" + wardenFile));
		//this.Out = new BufferedWriter(new FileWriter("tmp.txt"));
		this.generateGraph(asRelFile, wardenFile);
		HashMap<Integer, Vertex> tempMap = new HashMap<Integer, Vertex>();
		for (int key: this.neutralVertexMap.keySet()) {
			tempMap.put(key, this.neutralVertexMap.get(key));
		}
		
		for (int i = 0; i < trials; ++i) {
			if (i % (trials/10) == 0) {
				System.out.println(100*i/trials + "% done!");
				
			}
			this.reset(tempMap);
			this.randomRandomPartitioning();
			this.printResults();
		}
		this.Out.close();
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
		for (Vertex warden : this.wardenSet) {
			this.wardenBlack.add(warden);
		}
		for (int key : neutralVertexMap.keySet()) {
			this.neutralVertexMap.put(key, neutralVertexMap.get(key));
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
		this.randomSelectNextSeed();
		this.oppositeExtendedInThisTurn = true;
		
		while (true) {

			/* 
			 * when there is no vertex to extend for warden gray set,
			 * all possible separators are found. 
			 */
			if (randomExtendWardenGray())
				break;
			
			if (!this.oppositeExtendedInThisTurn) {
				randomExtendOppositeGray();
			}
			
			/*
			 * if a current search for opposite set cannot extend,
			 * generate a new seed for another extend.
			 * if no seed is available, all possible separators are found.
			 */
			if (!this.oppositeExtendedInThisTurn){
				if (this.randomSelectNextSeed() == -1)
					break;
			}			
			this.oppositeExtendedInThisTurn = false;


			if (Constants.DEBUG) {
				System.out.println("******");
			}
		}
		
		this.checkWardenGray();
	}

	/**
	 * randomly select ONE extendible vertex from warden adjacent set
	 * put into warden set, relax its neighbors and update warden 
	 * adjacent set for the next round.
	 * @return 	true   if warden gray set is empty
	 * 			false  if warden gray set is not empty
	 */
	private boolean randomExtendWardenGray() {
		int randomIndex, cntToBeGray;
		boolean isSeparator, notFindNextWarden = true;
		while (notFindNextWarden) {
			/* if cannot find any vertex to extend, partitioning finishes! */
			if (this.wardenGray.isEmpty()) 
				return true;
			
			randomIndex = this.randomNext.nextInt(this.wardenGray.size());
			Vertex relaxingVertex = this.wardenGray.get(randomIndex);
			
			cntToBeGray = 0;
			isSeparator = false;
			for (Vertex neighbor : relaxingVertex.getAllNeighbors()) {
				/* if a node in gray set is adjacent to opposite , it is a separator */
				if (this.oppositeGray.contains(neighbor) || this.oppositeBlack.contains(neighbor)) {
					isSeparator = true;
					continue;
				}
				if (this.neutralVertexMap.containsKey(neighbor.getVertexID())) {
					this.wardenGray.add(neighbor);
					this.neutralVertexMap.remove(neighbor.getVertexID());
					++cntToBeGray;
					
					if (Constants.DEBUG) {
						System.out.println("warden set added: " + neighbor.getVertexID());
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
				this.separatorSet.add(relaxingVertex);
				this.wardenGray.remove(relaxingVertex);
				if (cntToBeGray != 0) {
					notFindNextWarden = false;
				}
			} else if (cntToBeGray == 0) {
				this.wardenBlack.add(relaxingVertex);
				this.wardenGray.remove(relaxingVertex);
			} else {
				notFindNextWarden = false;
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
	private void randomExtendOppositeGray() {
		
		int randomIndex, cntToBeGray;
		boolean notFindNextOpposite = true;
		while (notFindNextOpposite) {
			/* if cannot find any vertex to extend, petitioning finishes! */
			if (this.oppositeGray.isEmpty()) 
				break;
			
			randomIndex = this.randomNext.nextInt(this.oppositeGray.size());
			Vertex relaxingVertex = this.oppositeGray.get(randomIndex);
			
			cntToBeGray = 0;
			for (Vertex vertexToBeGray : relaxingVertex.getAllNeighbors()) {
				if (this.neutralVertexMap.containsKey(vertexToBeGray.getVertexID())) {
					this.oppositeGray.add(vertexToBeGray);
					this.neutralVertexMap.remove(vertexToBeGray.getVertexID());
					++cntToBeGray;
					
					if (Constants.DEBUG) {
						System.out.println("opposit set added: " + vertexToBeGray.getVertexID());
					}
				}
			}
			if (cntToBeGray == 0) {
				this.oppositeBlack.add(relaxingVertex);
				this.oppositeGray.remove(relaxingVertex);
				continue;
			}
			notFindNextOpposite = false;
			this.oppositeExtendedInThisTurn = true;
		}
	}
	
	/**
	 * Select separators from warden gray set
	 * which must not be adjacent to opposite gray or black   
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
	 * create an adjacent set for the initial warden set, using which to 
	 * start the algorithm.
	 */
	private void createWardenAdjacentSet() {
		for (Vertex wardenVertex : this.wardenSet) {
			for (Vertex wardenNeighbor : wardenVertex.getAllNeighbors()) {
				if (!this.wardenSet.contains(wardenNeighbor) && !this.wardenGray.contains(wardenNeighbor)) {
					this.wardenGray.add(wardenNeighbor);
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
		/* graph petitioning process finishes */
		if (this.neutralVertexMap.size() == 0)
			return -1;
		
		Object[] keyArray = this.neutralVertexMap.keySet().toArray();
		int randomIndex = this.randomNext.nextInt(keyArray.length);
		int randomVertex = (Integer)keyArray[randomIndex];
		this.oppositeGray.add(this.neutralVertexMap.get(randomVertex));
		this.oppositeBlack.add(this.neutralVertexMap.get(randomVertex));
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
	
	public Set<Vertex> getSeparatorSet() {
		return this.separatorSet;
	}
	public int getSeparatorSize() {
		return this.separatorSet.size();
	}
	
	private void printResults() throws IOException {
		
		this.Out.write(this.separatorSet.size()+"\n");
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
