package sim;

import java.io.IOException;

public class FindSeparator {
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		
		GraphPartitioning engine = new GraphPartitioning();
		long startTime, endTime;
		startTime = System.currentTimeMillis();
		
		if (args.length == 4) {
			/* multiple trials */
			
			System.out.println(args[1] + ", " + args[2]);
			engine.multipleRuns(args[0], args[1], args[2], Integer.valueOf(args[3]));
			
		} else if (args.length == 3) {
			/* single trial */
			
			engine.singleRun(args[0], args[1], args[2]);
			
		} else {
			System.out.println("Usage: ./Find-separator <wardenFile> <wardenMode> <oppositeMode> <trials> " +
					"OR Usage: ./Find-separator <wardenFile> <wardenMode> <oppositeMode>");
		}
		
		endTime = System.currentTimeMillis();
		System.out.println("\nAll separators found, this took: " + (endTime - startTime) / 1000 + " seconds, "
				+ (endTime - startTime) / 60000 + " minutes.");
	}
	
}
