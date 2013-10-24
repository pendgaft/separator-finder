package sim;

import java.io.IOException;

public class FindSeparator {
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		
		if (args.length != 2 && args.length != 1) {
			System.out.println("Usage: ./Find-separator <wardenFile> <trials> OR Usage: ./Find-separator <wardenFile>");
			return;
		}
		long startTime, endTime;
		startTime = System.currentTimeMillis();

		GraphPartitioning engine = new GraphPartitioning();
		if (args.length == 2) {
			engine.mutitpleRuns(Constants.AS_REL_FILE, args[0], Integer.valueOf(args[1]));
		} else {
			engine.singleRun(Constants.AS_REL_FILE, args[0]);
		}
		endTime = System.currentTimeMillis();
		System.out.println("\nAll separators found, this took: " + (endTime - startTime) / 1000 + " seconds, "
				+ (endTime - startTime) / 60000 + " minutes.");
	}	
	
}
