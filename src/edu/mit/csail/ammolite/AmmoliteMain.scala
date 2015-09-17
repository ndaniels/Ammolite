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
import edu.mit.csail.ammolite.search.SearchHandler

import edu.mit.csail.ammolite.spark.SparkSearchHandler

import collection.mutable.Buffer
import collection.Seq
import edu.mit.csail.ammolite.utils.SDFUtils
import edu.mit.csail.ammolite.utils.MCSTableMaker


object AmmoliteMain{
		
	def main(args: Array[String]){
		
		val opts = new ScallopConf(args){
			guessOptionName = true
			banner("Welcome to Ammolite Aligned Molecule Matching!")
			version("Beta Version")

			footer("\nHope you find your molecule!")
			
			
			val compress = new Subcommand("compress"){
			  banner("Compress a database of SDF files")
			  val source = opt[List[String]]("source", required=true, descr="File or folder to compress")
			  val target = opt[String]("target", required=true, descr="Name of the new compressed database")
			  val threads = opt[Int]("threads", default=Some(-1), descr="Number of threads to use for compression. Leave blank to use half the available processing cores.")
			}
			val search = new Subcommand("search"){
			  
				val database = opt[List[String]]("database", required=true, descr="Path to the database. If using linear search this may include multiple files and sdf files.")
				val queries = opt[List[String]]("queries", required=true, descr="SDF files of queries.")
				val threshold = opt[Double]("threshold", required=true, descr="Matching threshold to use.")
			    val writeSDF = opt[Boolean]("write-sdfs", descr="Make a SDF files of the search results.")
			    val linear = opt[Boolean]("linear-search", descr="Search the database exhaustively.")
			    val out = opt[String]("out-file", default=Some("-"), descr="Where search results should be written to. Std out by default.")
			    
			}
			val mcs = new Subcommand("mcs"){
			  val sdfA = opt[String]("a", required=true, descr="First file for MCS table")
			  val sdfB = opt[String]("b", required=true, descr="Second file for MCS table")
			}


			val devTestSearch = new Subcommand("test"){
			  val q = opt[List[String]]("queries", required=true, descr="SDF files of queries.")
			  val db = opt[String]("database", required=true, descr="Path to the database.")
			  val out = opt[String]("outName", required=true, descr="Where search results should be written to.")
			  val description = opt[String]("description", default=Some(""), descr="A brief description of the test.")
			  val t = opt[Double]("threshold")
			  val c = opt[Double]("coarse-threshold")
			  val smsd = opt[Boolean]("SMSD", default=Some(false), descr="Search the database using SMSD")
			  val par = opt[Boolean]("Parallel", default=Some(false), descr="Development Only.")
			  val fmcs = opt[Boolean]("FMCS", default=Some(false), descr="Development Only.")
			  val amm = opt[Boolean]("Ammolite", default=Some(false), descr="Search the database using Ammolite")
			  val ammSMSD = opt[Boolean]("AmmSMSD", default=Some(false), descr="Development Only.")
			  val queryComp = opt[Boolean]("QueryCompression", default=Some(false), descr="Development Only.")
			  val useCaching = opt[Boolean]("Caching", default=Some(false), descr="Development Only.")
			}
			val examine = new Subcommand("examine"){
			  val database = trailArg[String]()
			  val table = opt[Boolean]("table", default=Some(false), descr="Show a table of the compressed database structure. Not reccomended for databases over 1K molecules.")
			} 
			val spark = new Subcommand("spark"){
				val database = opt[List[String]]("database", required=true, descr="Path to the database. If using linear search this may include multiple files and sdf files.")
				val queries = opt[List[String]]("queries", required=true, descr="SDF files of queries.")
				val threshold = opt[Double]("threshold", required=true, descr="Matching threshold to use.")
			    val writeSDF = opt[Boolean]("write-sdfs", descr="Make a SDF files of the search results.")
			    val linear = opt[Boolean]("linear-search", descr="Search the database exhaustively.")
			    val out = opt[String]("out-file", default=Some("-"), descr="Where search results should be written to. Std out by default.")
			}

			
			
		}
		
		Logger.setVerbosity( 1)
		
		if( opts.subcommand == Some(opts.compress)){
			var compType = CompressionType.FULLY_LABELED

			val compressor = new CachingStructCompressor( compType )
			compressor.compress(java.util.Arrays.asList(opts.compress.source().toArray: _*), opts.compress.target(), opts.compress.threads())
  
		} else if( opts.subcommand == Some(opts.search)){

			SearchHandler.handleSearch(	java.util.Arrays.asList(opts.search.queries().toArray: _*), 
										java.util.Arrays.asList(opts.search.database().toArray: _*), 
										opts.search.out(),
										opts.search.threshold(), 
										opts.search.writeSDF(),
										opts.search.linear())
		  
		} else if( opts.subcommand ==Some(opts.mcs)){
		  edu.mit.csail.ammolite.utils.MCSTableMaker.printMCSTable( opts.mcs.sdfA(), opts.mcs.sdfB())
		  
		} else if( opts.subcommand == Some( opts.devTestSearch)){

			  edu.mit.csail.ammolite.tests.SearchTest.testSearch(java.util.Arrays.asList(opts.devTestSearch.q().toArray: _*), opts.devTestSearch.db(), opts.devTestSearch.out(), opts.devTestSearch.t(), opts.devTestSearch.c(), 
					  												opts.devTestSearch.amm(),
					  												opts.devTestSearch.par(),
					  												opts.devTestSearch.queryComp(),
					  												opts.devTestSearch.smsd(),
					  												opts.devTestSearch.fmcs(),
					  												opts.devTestSearch.ammSMSD(),
					  												opts.devTestSearch.useCaching(),
					  												opts.devTestSearch.description())
		  
		  
		} else if( opts.subcommand == Some( opts.examine)){
			val db = StructDatabaseDecompressor.decompress( opts.examine.database())
		    Logger.log(db.info())
			if(opts.examine.table()){  
		    	Logger.log(db.asTable())
		    }
		} else if( opts.subcommand == Some( opts.spark)){
			SparkSearchHandler.handleDistributedSearch(java.util.Arrays.asList(opts.search.queries().toArray: _*), 
														java.util.Arrays.asList(opts.search.database().toArray: _*), 
														opts.search.out(),
														opts.search.threshold(), 
														opts.search.writeSDF(),
														opts.search.linear())
		}
	}

		
	
}

