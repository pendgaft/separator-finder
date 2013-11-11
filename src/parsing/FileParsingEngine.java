package parsing;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FileParsingEngine {

	private String parsingFile;
	private String outputFile;
	private List<Double> cntList;
	/**
	 * @param args
	 */
	public static void main(String args[]) throws IOException {
		if (args.length != 1) {
			System.out.println("Usage: ./file-parsing-engine <parsingFile>");
			return;
		}
		long startTime, endTime;
		startTime = System.currentTimeMillis();
		
		FileParsingEngine self = new FileParsingEngine(args[0]);
		self.Run();
		
		endTime = System.currentTimeMillis();
		System.out.println("\nAll separators found, this took: " + (endTime - startTime) / 1000 + " seconds, "
				+ (endTime - startTime) / 60000 + " minutes.");
	}
	
	public FileParsingEngine(String parsingFile) {
		this.parsingFile = parsingFile;
		this.outputFile = "CDF.txt";
		this.cntList = new ArrayList<Double>();
	}
	
	private void Run() throws IOException {
		this.readFile();
		this.printCDF(this.cntList, this.outputFile);
	}
	
	private void readFile() throws IOException {
		String pollString;
		BufferedReader inFile = new BufferedReader(new FileReader(this.parsingFile));
		while (inFile.ready()) {
			pollString = inFile.readLine().trim();
			if (pollString.length() > 0) {
				this.cntList.add(Double.parseDouble(pollString));
			}
		}
		inFile.close();
	}

	/**
	 * Function that dumps a CDF of the supplied list of doubles to a file
	 * specified by a string.
	 * 
	 * @param vals
	 *            - a non-empty list of doubles
	 * @param fileName
	 *            - the file the CDF will be writen to in CSV format
	 * @throws IOException
	 *             - if there is an error writting to the file matching the file
	 *             name
	 */
	private static void printCDF(List<Double> origVals, String fileName) throws IOException {
		/*
		 * CDFs over empty lists don't really make sense
		 */
		if (origVals.size() == 0) {
			throw new RuntimeException("Asked to build CDF of an empty list!");
		}

		/*
		 * Clone the list to avoid the side effect of sorting the original list
		 */
		List<Double> vals = new ArrayList<Double>(origVals.size());
		for (double tDouble : origVals) {
			vals.add(tDouble);
		}

		Collections.sort(vals);
		double fracStep = 1.0 / (double) vals.size();
		double currStep = 0.0;

		BufferedWriter outFile = new BufferedWriter(new FileWriter(fileName));

		for (int counter = 0; counter < vals.size(); counter++) {
			currStep += fracStep;
			if (counter >= vals.size() - 1 || vals.get(counter) != vals.get(counter + 1)) {
			//if (vals.get(counter) != vals.get(counter + 1) && counter <= vals.size() + 1) {
				outFile.write("" + currStep + "," + vals.get(counter) + "\n");
				System.out.print("" + currStep + "," + vals.get(counter) + "\n");
			}
		}

		outFile.close();
	}
}
