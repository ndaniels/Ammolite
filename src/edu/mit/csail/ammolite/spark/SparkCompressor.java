package edu.mit.csail.ammolite.spark;

import java.util.List;

import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.function.Function;

import edu.mit.csail.ammolite.compression.IMolStruct;
import edu.mit.csail.ammolite.compression.LabeledVF2IsomorphismTester;
import edu.mit.csail.ammolite.utils.MolUtils;
import edu.mit.csail.ammolite.utils.StructID;
import org.apache.spark.api.java.JavaSparkContext;
import edu.ucla.sspace.graph.isomorphism.AbstractIsomorphismTester;

public class SparkCompressor {
    
    
    public static StructID distributedIsomorphism(final IMolStruct structure, List<IMolStruct> potentialMatches, JavaSparkContext sparkCTX){
        JavaRDD<IMolStruct> distPotentials = sparkCTX.parallelize(potentialMatches);
        
        JavaRDD<IMolStruct> isomorphic = distPotentials.filter(new Function<IMolStruct, Boolean>(){
            public Boolean call(IMolStruct candidate){
                AbstractIsomorphismTester iso_tester = new LabeledVF2IsomorphismTester();
                return candidate.isIsomorphic(structure, iso_tester);
            }
        });
        
        List<IMolStruct> localIso = isomorphic.collect(); // There will only very rarely be more than one item in this list (b/c of timeouts)
        if(localIso.size() == 0){
            return null;
        } else {
            return MolUtils.getStructID(localIso.get(0));
        }
    }
}
