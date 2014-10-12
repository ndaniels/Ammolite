package edu.mit.csail.ammolite.mcs;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;

public class MyMCS implements IMCS{
    
    private static final int MINIMUM_BOUNDARY = 0;
    VertexMap curMap;
    int sizeOfBestMap = 0;
    Graph g1;
    Graph g2;
    
    public MyMCS(IAtomContainer m1, IAtomContainer m2){
        this( new Graph(AtomContainerManipulator.removeHydrogens(m1)), 
                new Graph(AtomContainerManipulator.removeHydrogens(m2)));
    }

    public MyMCS(Graph g1, Graph g2) {
        if(g1.allVertices().length < g2.allVertices().length){
            this.g1 = g1;
            this.g2 = g2;
            curMap = new VertexMap(g1,g2);
        } else {
            this.g1 = g2;
            this.g2 = g1;
            curMap = new VertexMap(g2,g1);
        }

    }
    
    public long calculate(){
        long startTime = System.currentTimeMillis();
        grow(g1.allVertices(), g2.allVertices());
        return System.currentTimeMillis() - startTime;
    }
    
    int[] getDegreesInMap(Vertex[] vertices, Graph g){
        int[] out = new int[vertices.length];
        for(int i=0; i<vertices.length; i++){
            Vertex v = vertices[i];
            for(Vertex n: g.getConnected(v)){
                if( curMap.contains(n,g)){
                    out[i]++;
                }
            }
        }
        return out;
        
    }
    
    int upperBoundFromDegrees(int[] degV1,  int[] degV2){
        // degrees in molecules are usually small, this should work 
        // most of the time.
        
        int maxAllowed = 10;
        
        int[] count1 = new int[maxAllowed];
        int[] count2 = new int[maxAllowed];
        
        for(int deg: degV1){
            if( deg < maxAllowed){
                count1[deg]++;
            }
        }
        for(int deg: degV2){
            if( deg < maxAllowed){
                count2[deg]++;
            }
        }
        
        int uBound = 0;
        for(int i=0; i<maxAllowed; i++){
            if(count1[i] <= count2[i]){
                uBound += count1[i];
            } else {
                uBound += count2[i];
            }
        }
        return uBound;
    }
    
    void grow(Vertex[] iV1, Vertex[] iV2){
        Vertex[] v1 = iV1;
        Vertex[] v2= iV2;
        int[] degV1 = getDegreesInMap( v1, g1);
        int[] degV2 = getDegreesInMap( v2, g2);
        int uBound = upperBoundFromDegrees(degV1, degV2);
        if( uBound < MINIMUM_BOUNDARY){
            return;
        }
        while(true){
           
            if( v1.length == 0 || v2.length == 0){
                int curMapSize = curMap.mapSize();
                if( curMapSize > sizeOfBestMap){
                    sizeOfBestMap = curMapSize;
                }
                return;
            }
            
            Vertex top = bestCandidate(v1, degV1);
            
            Vertex[] removed = new Vertex[v1.length - 1];
            int[] remDeg = new int[degV1.length - 1];
            int i=0;
            for(int j=0; j<v1.length; j++){
                if(v1[j] != top){
                    removed[i] = v1[j];
                    remDeg[i] = degV1[j];
                    i++;
                }
            }
            v1 = removed;
            degV1 = remDeg;
            for(Vertex other: v2){
                if( top.matches( other) ){
                    boolean compatible = compatible(top, other);
//                    System.out.println(compatible);
                    if( compatible){
                        System.out.println("growing map...");
                        curMap.addMatch(top, other);
                        System.out.println("Exploring "+top.label()+" ("+g1.getConnected(top).length+" knex), "+ other.label()+" ("+g2.getConnected(other).length+" knex)");
                        Vertex[] v2Copy = getWithoutEl(other,v2);
                        grow( v1, v2Copy);
                        curMap.removeMatch(top,other);
                    }
                }
            }

        }
        
        
        
    }
    
    private Vertex[] getWithoutEl(Vertex el, Vertex[] verts){
        Vertex[] removed = new Vertex[verts.length -1];
        int i=0;
        for(Vertex v: verts){
            if(v != el){
                removed[i] = v;
                i++;
            }
        }
        return removed;
    }

    private Vertex bestCandidate(Vertex[] v1, int[] degV1) {
        Vertex best=v1[0];
        Vertex candidate = null;
        
        for(int i=0; i<v1.length; i++){
            Vertex contender = v1[i];
            
            if( g1.getConnected(contender).length > g1.getConnected(best).length){
                best = contender;
            }
            for(Vertex contenderNeighbour: g1.getConnected(contender)){
                if( curMap.contains(contenderNeighbour, g1)){
                    if(     (candidate == null) || 
                            ( g1.getConnected(contender).length > g1.getConnected(best).length)){
                        candidate = contender;
                        break;
                    }
                }
            }
        }
        if(candidate == null){
            return best;
        } else {
            return candidate;
        }
            
    }

    private boolean compatible(Vertex top, Vertex other) {
        Vertex[] n1 = curMap.getAdjacent(top, g1);
        Vertex[] n2 = curMap.getAdjacent(other, g2);
        
        if(n1.length != n2.length){
            System.out.println("lengths do not match");
            return false;
        } else if( n1.length == 0 && curMap.mapSize() > 0){
            // only permit one connected component
            System.out.println("only allow one connected component");
            return false;
        }
        
        String[] n1Labels = new String[n1.length];
        String[] n2Labels = new String[n2.length];
        for(int i=0; i<n1.length; i++){
            n1Labels[i] = n1[i].label();
            n2Labels[i] = n2[i].label();
        }
        Arrays.sort(n1Labels);
        Arrays.sort(n2Labels);
        for(int i=0; i<n1.length; i++){
            if(n1Labels[i] != n2Labels[i]){
                System.out.println("neighbours do not match");
                return false;
            }
        }
        
        for(Vertex alpha: n1){
            Vertex omega = curMap.getCounterpart(alpha, g1);
            int bondOrder1 = g1.weight(top, alpha);
            int bondOrder2 = g2.weight(other, omega);
            if( bondOrder1 != bondOrder2){ 
                System.out.println("bond orders do not match");
                return false;
            }
        }
        return true;
    }

    @Override
    public int size() {
        return this.sizeOfBestMap;
    }

}
