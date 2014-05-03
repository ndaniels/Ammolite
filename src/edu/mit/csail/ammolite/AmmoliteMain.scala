package edu.mit.csail.ammolite

import edu.mit.csail.ammolite.compression.FragStruct
import edu.mit.csail.ammolite.compression.MoleculeStructFactory
import edu.mit.csail.ammolite.compression.StructCompressor

import org.rogach.scallop._

import edu.mit.csail.ammolite.search.SearchHandler
import edu.mit.csail.ammolite.utils.Logger;
import edu.mit.csail.ammolite.compression.CyclicStruct
import edu.mit.csail.ammolite.database.CompressionType
import edu.mit.csail.ammolite.database.StructDatabaseDecompressor
import edu.mit.csail.ammolite.aggregation.Aggregator
import edu.mit.csail.ammolite.aggregation.AggregateSearcher
import edu.mit.csail.ammolite.aggregation.ClusterDatabaseDecompressor


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
			  val simple = opt[Boolean]("simple", descr="Use simple structures instead of cyclic structures")
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
			val examine = new Subcommand("examine"){
			  val database = opt[String]("database", required=true, descr="Path to the database.") 
			} 
			val aggexamine = new Subcommand("aggexamine"){
			  val database = opt[String]("database", required=true, descr="Path to the database.") 
			} 

		  val aggcompress = new Subcommand("aggcompress"){
			val source = opt[String]("source", required=true, descr="File or folder to compress")
		    val target = opt[String]("target", required=true, descr="Name of the new compressed database")
		    val repbound = opt[Double]("rep-boundary", default=Some(0.5), descr="how similar clusters have to be")
		    val linearizeClustering = opt[Boolean]("linear-cluster", default=Some(false), descr="development...")
		  }
		  val aggsearch = new Subcommand("aggsearch"){
		    val cluster = opt[String]("cluster", required=true, descr="Path to the cluster database you want to search.")
			val queries = opt[String]("queries", required=true, descr="SDF file of queries.")
			val threshold = opt[Double]("threshold", descr="Threshold to use. Uses overlap coefficient by default.")
		    val tanimoto = opt[Boolean]("tanimoto", descr="Use tanimoto coefficients.", default=Some(false))
		    val target = opt[String]("target", required=true, descr="Make an SDF file of the search results. First molecule is the query, second is the match, third is the overlap.")
		    val searchbound = opt[Double]("search-boundary", default=Some(0.5), descr="how similar clusters have to be")
		  }
			
		}
		
		Logger.setVerbosity( opts.verbosity())
		
		if( opts.subcommand == Some(opts.compress)){
		  var compType = CompressionType.CYCLIC
		  if( opts.compress.simple()){
		    compType = CompressionType.BASIC 
		  } 
			  
		  val compressor = new StructCompressor( compType )
		  
		  compressor.compress(opts.compress.source(), opts.compress.target())
		  
		} else if( opts.subcommand == Some(opts.search)){
			val searchHandler = new SearchHandler( opts.search.database(), opts.search.queries(), opts.search.target(),
													opts.search.threshold(), opts.search.probability(), opts.search.tanimoto())
			searchHandler.handleSearch();
		  
		} else if( opts.subcommand ==Some(opts.mcs)){
		  Logger.log("Finding edu.mit.csail.fmcsj of two molecules")
		  edu.mit.csail.ammolite.utils.UtilFMCS.doFMCS(opts.mcs.molecules(), opts.mcs.sdf())
		  
		} else if( opts.subcommand == Some( opts.dev)){
		  edu.mit.csail.ammolite.utils.UtilFMCS.getCoeffs(opts.dev.a(), opts.dev.b())
		} else if( opts.subcommand == Some( opts.examine)){
			val db = StructDatabaseDecompressor.decompress( opts.examine.database())
		    Logger.log(db.info())
		} else if( opts.subcommand == Some( opts.aggexamine)){
		  val db = ClusterDatabaseDecompressor.decompress( opts.aggexamine.database())
		  Logger.log(db.info())
		} else if(opts.subcommand == Some( opts.aggcompress)){

		    val agg = new Aggregator( opts.aggcompress.source(), opts.aggcompress.repbound())
		    agg.aggregate(opts.aggcompress.target(), opts.aggcompress.linearizeClustering())
		} else if( opts.subcommand == Some( opts.aggsearch)){

		    val aggSearcher = new AggregateSearcher(opts.aggsearch.cluster(), opts.aggsearch.searchbound())
		    
		    aggSearcher.doSearch(opts.aggsearch.queries(), opts.aggsearch.target(), opts.aggsearch.threshold(), opts.aggsearch.tanimoto())
		}
	}

		
	
}

