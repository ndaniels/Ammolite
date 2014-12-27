package edu.mit.csail.ammolite

import edu.mit.csail.ammolite.compression.CachingStructCompressor
import edu.mit.csail.ammolite.compression.MoleculeStructFactory
import edu.mit.csail.ammolite.compression.StructCompressor
import org.rogach.scallop._
import edu.mit.csail.ammolite.utils.Logger
import edu.mit.csail.ammolite.compression.CyclicStruct
import edu.mit.csail.ammolite.database.CompressionType
import edu.mit.csail.ammolite.database.StructDatabaseDecompressor
import edu.mit.csail.ammolite.database.SDFWrapper
import collection.mutable.Buffer
import collection.Seq
import edu.mit.csail.ammolite.utils.SDFUtils


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
			  val source = opt[List[String]]("source", required=true, descr="File or folder to compress")
			  val target = opt[String]("target", required=true, descr="Name of the new compressed database")
			  val simple = opt[Boolean]("simple", descr="Use simple structures instead of cyclic structures")
			  val labeled = opt[Boolean]("labeled", descr="Use fully labeled cyclic structures instead of cyclic structures")
			  val weighted = opt[Boolean]("weighted", descr="Use labeled-weighted structures instead of cyclic structures")
			  val threads = opt[Int]("threads", default=Some(-1), descr="Number of threads to use for compression")
			  val cache = opt[Boolean]("cache", descr="Dev")
			}
			val search = new Subcommand("search"){
			    
			}
			val devTestSearch = new Subcommand("test"){
			  val q = opt[List[String]]("queries", required=true, descr="SDF files of queries.")
			  val db = opt[String]("database")
			  val out = opt[String]("outName")
			  val description = opt[String]("description", required=false)
			  val t = opt[Double]("fine-threshold")
			  val p = opt[Double]("coarse-threshold")
			  val smsd = opt[Boolean]("SMSD", default=Some(false))
			  val amm = opt[Boolean]("Amm", default=Some(false))
			}
			val examine = new Subcommand("examine"){
			  val database = trailArg[String]()
			  // val database = opt[String]("database", required=true, descr="Path to the database.")
			  val table = opt[Boolean]("table", default=Some(false))
			} 
			
		}
		
		Logger.setVerbosity( opts.verbosity())
		
		if( opts.subcommand == Some(opts.compress)){
			  var compType = CompressionType.CYCLIC
			  if( opts.compress.simple()){
			    compType = CompressionType.BASIC 
			  } else if( opts.compress.labeled()){
			  	compType = CompressionType.FULLY_LABELED
			  } else if( opts.compress.weighted()){
			  	compType = CompressionType.WEIGHTED
			  }
			

			  if( opts.compress.cache()){
			  	val compressor = new CachingStructCompressor( compType )
			  	compressor.compress(java.util.Arrays.asList(opts.compress.source().toArray: _*), opts.compress.target(), opts.compress.threads())
			  }  else {
			  	val compressor = new StructCompressor( compType )
			  	compressor.compress(java.util.Arrays.asList(opts.compress.source().toArray: _*), opts.compress.target(), opts.compress.threads())
			  }
		} else if( opts.subcommand == Some(opts.search)){
		} else if( opts.subcommand == Some( opts.devTestSearch)){

			  edu.mit.csail.ammolite.tests.SearchTest.testSearch(java.util.Arrays.asList(opts.devTestSearch.q().toArray: _*), opts.devTestSearch.db(), opts.devTestSearch.out(), opts.devTestSearch.t(), opts.devTestSearch.p(), 
					  												opts.devTestSearch.amm(),
					  												opts.devTestSearch.smsd(),
					  												opts.devTestSearch.description())
		  
		  
		} else if( opts.subcommand == Some( opts.examine)){
			val db = StructDatabaseDecompressor.decompress( opts.examine.database())
		    Logger.log(db.info())
			if(opts.examine.table()){  
		    	Logger.log(db.asTable())
		    }
		} 
	}

		
	
}

