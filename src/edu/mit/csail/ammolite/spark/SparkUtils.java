package edu.mit.csail.ammolite.spark;

import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.openscience.cdk.interfaces.IAtomContainer;

import scala.Tuple2;
import edu.mit.csail.ammolite.utils.SDFUtils;

public class SparkUtils {
    private static final String SDF_SEPARATOR = "$$$$";
    
    public static JavaRDD<IAtomContainer> sdfFilesToMols(String path, JavaSparkContext ctx){
        JavaPairRDD<String, String> sdfFiles = ctx.wholeTextFiles(path);
        
        FlatMapFunction<Tuple2<String,String>,IAtomContainer> sdfBlockBuilder = new FlatMapFunction<Tuple2<String,String>,IAtomContainer>(){

            public Iterable<IAtomContainer> call(Tuple2<String,String> sdfFile) throws Exception {
                   return SDFUtils.parseSDF(sdfFile._2());
            }

            
        };
        
        JavaRDD<IAtomContainer> molecules = sdfFiles.flatMap(sdfBlockBuilder);
        
        return molecules;
    }

}
