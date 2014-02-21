package edu.mit.csail.ammolite

import edu.mit.csail.ammolite.compression.FragStruct
import edu.mit.csail.ammolite.compression.MoleculeStructFactory
import edu.mit.csail.ammolite.compression.StructCompressor
import org.rogach.scallop._
import edu.mit.csail.ammolite.search.SearchHandler
import edu.mit.csail.ammolite.compression.CyclicStruct


object AmmoliteMain{
		
	def main(args: Array[String]){
		
		val opts = new ScallopConf(args){
			guessOptionName = true
			banner("Welcome to Ammolite Aligned Molecule Matching!")
			version("Version 0.0.0")

			footer("\nHope you find your molecule!")
			
			val verbosity = opt[Int]("verbosity", default=Some(1), descr="Set the verbosity. 0 is quiet, 2 is verbose, etc")
			
			val compress = new Subcommand("compress"){
			  banner("Compress a database of SDF files")
			  val source = opt[String]("source", required=true, descr="File or folder to compress")
			  val target = opt[String]("target", required=true, descr="Name of the new compressed database")
			  val makeSDF = opt[String]("sdf", descr="Make an SDF file of the cluster representatives and a file with pointers")
			}
			val search = new Subcommand("search"){
			  
				val database = opt[String]("database", required=true, descr="Path to the database.")
				val queries = opt[String]("queries", required=true, descr="SDF file of queries.")
				val threshold = opt[Double]("threshold", descr="Threshold to use. Uses overlap coefficient by default.")
				val probability = opt[Double]("probability", descr="Probability of finding a result over a certain threshold")
			    val tanimoto = opt[Boolean]("tanimoto", descr="Use tanimoto coefficients.", default=Some(false))
			    val target = opt[String]("target", required=true, descr="Make an SDF file of the search results. First molecule is the query, second is the match, third is the overlap.")
			    
			}
			val mcs = new Subcommand("mcs"){
			  val molecules = opt[String]("molecules", required=true, descr="sdf file containing the two molecules you want the max common subgraph of")
			  val sdf = opt[String]("sdf", required=true, descr="Name of the file where you want the overlap results")
			}
			val dev = new Subcommand("dev"){
			  val a = opt[String]("a")
			  val b = opt[String]("b")
			}
		}
		
		Logger.setVerbosity( opts.verbosity())
		edu.mit.csail.fmcsj.Logger.setVerbosity(opts.verbosity())
		
		if( opts.subcommand == Some(opts.compress)){
		
		  val exemplar = new CyclicStruct()
		  val structFactory = new MoleculeStructFactory( exemplar)
		  val compressor = new StructCompressor( structFactory)
		  compressor.compress(opts.compress.source(), opts.compress.target())
		  if(opts.compress.makeSDF.isDefined ){
		    compressor.makeSDF( opts.compress.makeSDF())
		  }
		  
		} else if( opts.subcommand == Some(opts.search)){
			val searchHandler = new SearchHandler( opts.search.database(), opts.search.queries(), opts.search.target(),
													opts.search.threshold(), opts.search.probability(), opts.search.tanimoto())
			searchHandler.handleSearch();
		  
		} else if( opts.subcommand ==Some(opts.mcs)){
		  Logger.log("Finding edu.mit.csail.fmcsj of two molecules")
		  edu.mit.csail.fmcsj.FMCS.doFMCS(opts.mcs.molecules(), opts.mcs.sdf())
		  
		} else if( opts.subcommand == Some( opts.dev)){
		  edu.mit.csail.fmcsj.FMCS.getCoeffs(opts.dev.a(), opts.dev.b())
		} 
	}

		
	
}
