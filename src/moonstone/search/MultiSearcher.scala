package moonstone.search

import java.io.File
import java.io.FileInputStream
import moonstone.compression.StructDatabase
import org.openscience.cdk.DefaultChemObjectBuilder
import scala.collection.mutable.ListBuffer
import org.openscience.cdk.io.SDFWriter
import java.io.BufferedWriter
import java.io.FileWriter
import speedysearch.IteratingSDFReader
import speedysearch.MoleculeSearcher
import speedysearch.MoleculeTriple

class MultiSearcher(val database: StructDatabase, val inFilename: String) {
  
    val molecules = new IteratingSDFReader( new FileInputStream( new File( inFilename)), DefaultChemObjectBuilder.getInstance());
	val out = new ListBuffer[MoleculeTriple]
	
	def bigSearch(threshold: Double, useTanimoto: Boolean){
	 
	  val searcher = new MoleculeSearcher( database, useTanimoto);
	  while( molecules.hasNext()){
		  val query = molecules.next();
		  out ++= searcher.bigSearch( query, threshold);
	  }
	}
	
	def quickSearch(){
	  val searcher = new MoleculeSearcher( database);
	  while( molecules.hasNext()) {
		  val query = molecules.next();
		  out ++= searcher.quickSearch( query);
	  }
	}
	
	def makeSDF( filename: String){
	  val writer = new SDFWriter(new BufferedWriter( new FileWriter( filename + ".sdf" )))
	  for( triple <- out){
	    writer.write(triple.getOverlap().get(0))
	    writer.write(triple.getQuery())
	    writer.write(triple.getMatch())
	  }
	  writer.close()
	  
	}
	
	def testPrint() {
	  for( triple <- out){
	    print( triple.getOverlap().get(0).getAtomCount() )
	    print(" ")
	    print( triple.getQuery().getAtomCount())
	    print(" ")
	    println( triple.getMatch().getAtomCount() )
	  }
	}
}