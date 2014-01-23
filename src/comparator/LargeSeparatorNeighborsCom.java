package comparator;
import graph.Vertex;

import java.util.Comparator;

	/*
	 * Comparator in descending order, the null entry is also allowed in the comparison.
	 * 
	 * select the nodes with the max adjacent nodes that are in the same shore
	 */
public class LargeSeparatorNeighborsCom implements Comparator<Vertex> {
    @Override
    public int compare(Vertex v1, Vertex v2) {
    	if (v1 == null) return 1;
    	if (v2 == null) return -1;
        return (v1.getNumberOfSeparatorNeighbors() > v2.getNumberOfSeparatorNeighbors() ? 
        		-1 : (v1.getNumberOfSeparatorNeighbors() == v2.getNumberOfSeparatorNeighbors() ? 0 : 1));
    }
}
