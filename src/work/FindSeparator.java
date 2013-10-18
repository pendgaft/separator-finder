package work;

import java.io.IOException;

public class FindSeparator {
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		
		if (args.length != 1) {
			System.out.println("Usage: ./Find-separator <wardenFile>");
			return;
		}
		
		long startTime, endTime;
		startTime = System.currentTimeMillis();
		
		GraphPetitioning engine = new GraphPetitioning();
		engine.Run(Constants.AS_REL_FILE, args[0]);
		
		endTime = System.currentTimeMillis();
		System.out.println("\nAll separators found, this took: " + (endTime - startTime) / 60000 + " minutes.");
	}
	
	
}
