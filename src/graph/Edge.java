package graph;

public class Edge {

        protected Vertex lhsVertex;
        protected Vertex rhsVertex;

        public Edge(Vertex node1, Vertex node2) {
                this.lhsVertex = node1;
                this.rhsVertex = node2;
        }

        public Vertex[] getVerticies() {
                Vertex[] retArray = new Vertex[2];
                retArray[0] = this.lhsVertex;
                retArray[1] = this.rhsVertex;
                return retArray;
        }

        public Vertex getOtherVertex(Vertex avoidVertex) {
                if (this.lhsVertex.equals(avoidVertex)) {
                        return this.rhsVertex;
                }

                if (this.rhsVertex.equals(avoidVertex)) {
                        return this.lhsVertex;
                }

                throw new IllegalArgumentException(
                                "Ask to get other vertex, but the vertex provided to avoid isn't on the edge!");
        }

        public int hashCode() {
                return this.lhsVertex.hashCode() + this.rhsVertex.hashCode();
        }

        public boolean equals(Object rhsObject) {
                Edge rhsEdge = (Edge) rhsObject;
                return (this.lhsVertex.equals(rhsEdge.lhsVertex) && this.rhsVertex.equals(rhsEdge.rhsVertex))
                                || (this.rhsVertex.equals(rhsEdge.lhsVertex) && this.lhsVertex.equals(rhsEdge.rhsVertex));
        }

}
