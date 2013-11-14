package comparator;
import graph.Vertex;

import java.util.Comparator;

	/*
	 * Comparator in increasing order, the null entry is also allowed in the comparison.
	 * 
	 * select the nodes with the max adjacent nodes that are in the same shore
	 */
public class InwardSmallDegreeBasedCom implements Comparator<Vertex> {
    @Override
    public int compare(Vertex v1, Vertex v2) {
    	if (v1 == null) return -1;
    	if (v2 == null) return 1;
        return (v1.getNumberOfBlackNeighbors() > v2.getNumberOfBlackNeighbors() ? 
        		1 : (v1.getNumberOfBlackNeighbors() == v2.getNumberOfBlackNeighbors() ? 0 : -1));
    }
}
