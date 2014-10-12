package edu.mit.csail.ammolite.mcs;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class VertexMap {
    Graph g1;
    Graph g2;
    Set<Vertex> verts1 = new HashSet<Vertex>();
    Set<Vertex> verts2 = new HashSet<Vertex>();
    Map<Vertex,Vertex> vertexMapping1to2 = new HashMap<Vertex,Vertex>();
    Map<Vertex,Vertex> vertexMapping2to1 = new HashMap<Vertex,Vertex>();
    

    public VertexMap(Graph g1, Graph g2) {
        this.g1 = g1;
        this.g2 = g2;
    }

    public boolean contains(Vertex n, Graph g) {
        if(g == g1){
            return verts1.contains(n);
        } else {
            return verts2.contains(n);
        }
    }

    public void addMatch(Vertex v1, Vertex v2) {
        if( !g1.contains(v1) || !g2.contains(v2)){
            if(g1.contains(v2)){
                throw new RuntimeException("reverse");
            }
            throw new RuntimeException("no contain");
        }
        verts1.add(v1);
        verts2.add(v2);
        vertexMapping1to2.put(v1,v2);
        vertexMapping1to2.put(v2,v1);
    }

    public void removeMatch(Vertex v1, Vertex v2) {
        verts1.remove(v1);
        verts2.remove(v2);
        vertexMapping1to2.remove(v1);
        vertexMapping2to1.remove(v2);
        
    }

    public Vertex[] getAdjacent(Vertex v, Graph g) {
        Vertex[] allAdj = g.getConnected(v);
        boolean[] in = new boolean[allAdj.length];
        int nAdjInMap = 0;
        for(int i=0; i<allAdj.length; i++){
            Vertex a = allAdj[i];
            if(this.contains(a, g)){
                nAdjInMap++;
                in[i] = true;
            }
        }
        Vertex[] mapAdj = new Vertex[nAdjInMap];
        int mapInd = 0;
        for(int i=0; i< allAdj.length; i++){
            if( in[i]){
                mapAdj[ mapInd] = allAdj[i];
                mapInd++;
            }
        }
        return mapAdj;
    }
    
    public Vertex getCounterpart(Vertex v, Graph g){
        if( g == g1){
            return this.vertexMapping1to2.get(v);
        } else {
            return this.vertexMapping2to1.get(v);
        }
    }

    public int mapSize() {
        return verts1.size();
    }

}
