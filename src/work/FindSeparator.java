package work;

import java.io.IOException;

public class FindSeparator {

	private static final String AS_REL_FILE = "as-rel.txt";
	public static final boolean TEST = false;
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		
		long startTime, endTime;
		startTime = System.currentTimeMillis();
		
		GraphPetitioning engine = new GraphPetitioning();
		engine.Run(FindSeparator.AS_REL_FILE, args[0]);
		
		endTime = System.currentTimeMillis();
		System.out.println("\nAll work done, this took: " + (endTime - startTime) / 60000 + " minutes.");
	}
	
	
}
