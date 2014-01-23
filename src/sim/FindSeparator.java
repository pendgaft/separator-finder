package sim;

import java.io.IOException;

public class FindSeparator {
	private static final String SEPARATOR_MODE = "sep";
	private static final String OPTIMIZE_MODE = "opt";
	
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		
		
		long startTime, endTime;
		startTime = System.currentTimeMillis();
		GraphPartitioning partitionEngine = new GraphPartitioning();
		if (args[0].equalsIgnoreCase(FindSeparator.SEPARATOR_MODE)) {
			
			if (args.length == 5) {
				/* multiple trials */
				
				System.out.println(args[2] + ", " + args[3]);
				partitionEngine.multipleRuns(args[1], args[2], args[3], Integer.valueOf(args[4]));
				
			} else if (args.length == 4) {
				/* single trial */
				
				partitionEngine.singleRun(args[1], args[2], args[3]);
				
			} else {
				System.out.println("Separator Mode Usage: ./Find-separator <sep> <wardenFile> <wardenMode> <oppositeMode> <trials>\n" +
						"OR Usage: ./Find-separator <sep> <wardenFile> <wardenMode> <oppositeMode>");
			}
		
		
		} else if (args[0].equalsIgnoreCase(FindSeparator.OPTIMIZE_MODE)) {
			if (args.length != 5) {
				System.out.println("Optimize Mode Usage: ./Find-separator <opt> <wardenFile> <wardenMode> <oppositeMode> <threshold>");
				return;
			}
			if (!partitionEngine.singleRun(args[1], args[2], args[3])) {
				System.out.println("Wrong separators!!!");
				return;
			}
			/* pass separators, warden shore, opposite shore, warden set, and a threshold as parameters. */
			OptimizeSeparator optimizeEngine = new OptimizeSeparator(partitionEngine.getSeparators(), 
					partitionEngine.getWardenShore(), partitionEngine.getOppositeShore(), 
					partitionEngine.getWardens(), Integer.valueOf(args[4]));
			optimizeEngine.simulate();
			
		} else {
			/* mode under construction */
			System.out.println("Mode: <sep>, <opt>");
		}
		endTime = System.currentTimeMillis();
		System.out.println("\nAll separators found, this took: " + (endTime - startTime) / 1000 + " seconds, "
				+ (endTime - startTime) / 60000 + " minutes.");
	}
	
}
