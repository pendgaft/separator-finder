package graph;

public abstract class AbstractVertex {

        private Object suplementalData;
        private int vertexID;
        
        public AbstractVertex(int newID) {
                this.suplementalData = null;
                this.vertexID = newID;
        }
        
        public AbstractVertex(int newID, Object suppData){
                this.vertexID = newID;
                this.suplementalData = suppData;
        }

        public void updateSuplementalData(Object supData){
                this.suplementalData = supData;
        }
        
        public Object fetchSuplementalData(){
                return this.suplementalData;
        }
        
        public int getVertexID(){
                return this.vertexID;
        }
        
        public int hashCode(){
                return this.vertexID;
        }
        
        public boolean equals(Object rhs){
                AbstractVertex rhsVert = (AbstractVertex)rhs;
                return rhsVert.vertexID == this.vertexID;
        }
}
