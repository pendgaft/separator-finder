package graph;

import java.util.*;


public class Vertex extends AbstractVertex{
        
        private HashSet<Edge> edgeSet;
        private HashSet<Vertex> neighborSet;

        public Vertex(int idValue) {
                super(idValue);
                this.edgeSet = new HashSet<Edge>();
                this.neighborSet = new HashSet<Vertex>();
        }
        
        public Vertex(int idValue, Object supData){
                super(idValue, supData);
                this.edgeSet = new HashSet<Edge>();
        }
        
        public void addNeighbor(Vertex newNeighbor) {
        	this.neighborSet.add(newNeighbor);
        }
        
        public HashSet<Vertex> getAllNeighbors() {
        	return this.neighborSet;
        }
        
        public void addEdge(Edge newEdge){
                this.edgeSet.add(newEdge);
        }
        
        public Collection<Edge> getEdges(){
                return this.edgeSet;
        }
        
        public Collection<Integer> getAdjecentVertexIDs(){
                HashSet<Integer> idSet = new HashSet<Integer>();
                
                for(Edge tEdge: this.edgeSet){
                        idSet.add(tEdge.getOtherVertex(this).getVertexID());
                }
                
                return idSet;
        }
}
