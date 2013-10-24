package sim;

import java.io.IOException;

public class FindSeparator {
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		
		if (args.length != 2) {
			System.out.println("Usage: ./Find-separator <wardenFile> <trials>");
			return;
		}
		long startTime, endTime;
		startTime = System.currentTimeMillis();
		
		GraphPartitioning engine = new GraphPartitioning();
		engine.Run(Constants.AS_REL_FILE, args[0], Integer.valueOf(args[1]));
		
		endTime = System.currentTimeMillis();
		System.out.println("\nAll separators found, this took: " + (endTime - startTime) / 1000 + " seconds, "
				+ (endTime - startTime) / 60000 + " minutes.");
	}	
	
}
