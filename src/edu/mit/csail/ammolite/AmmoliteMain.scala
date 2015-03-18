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
			  // val simple = opt[Boolean]("simple", descr="Use simple structures instead of cyclic structures")
			  // val labeled = opt[Boolean]("labeled", descr="Use labeled structures instead of cyclic structures")

			  // val connection_2 = opt[Boolean]("connection-two", descr="Use connection two structures instead of cyclic structures")
			  // val connection_3 = opt[Boolean]("connection-three", descr="Use connection three structures instead of cyclic structures")
			  // val connection_4 = opt[Boolean]("connection-four", descr="Use connection four structures instead of cyclic structures")
			  // val connection_5 = opt[Boolean]("connection-five", descr="Use connection five structures instead of cyclic structures")
			  // val connection_6 = opt[Boolean]("connection-six", descr="Use connection six structures instead of cyclic structures")

			  // val overlap_4 = opt[Boolean]("overlap-four", descr="Use overlap five structures instead of cyclic structures")
			  // val overlap_5 = opt[Boolean]("overlap-five", descr="Use overlap six structures instead of cyclic structures")
			  // val overlap_6 = opt[Boolean]("overlap-six", descr="Use overlap seven structures instead of cyclic structures")
			  // val overlap_7 = opt[Boolean]("overlap-seven", descr="Use overlap seven structures instead of cyclic structures")
			  // val overlap_8 = opt[Boolean]("overlap-eight", descr="Use overlap seven structures instead of cyclic structures")
			  // val overlap_9 = opt[Boolean]("overlap-nine", descr="Use overlap seven structures instead of cyclic structures")

			  // val binary_overlap_4 = opt[Boolean]("binary-overlap-four", descr="Use overlap five structures instead of cyclic structures")
			  // val binary_overlap_5 = opt[Boolean]("binary-overlap-five", descr="Use overlap six structures instead of cyclic structures")
			  // val binary_overlap_6 = opt[Boolean]("binary-overlap-six", descr="Use overlap seven structures instead of cyclic structures")

			  // val weighted = opt[Boolean]("weighted", descr="Use labeled-weighted structures instead of cyclic structures")
			  // val iterated = opt[Boolean]("iterated", descr="Dev")
			  val threads = opt[Int]("threads", default=Some(-1), descr="Number of threads to use for compression. Leave blank to use half the available processing cores.")
			  // val cache = opt[Boolean]("cache", descr="Dev")
			}
			val search = new Subcommand("search"){
			  
				val database = opt[String]("database", required=true, descr="Path to the database.")
				val queries = opt[List[String]]("queries", required=true, descr="SDF files of queries.")
				val threshold = opt[Double]("threshold", descr="Threshold to use. Uses overlap coefficient by default.")
				val probability = opt[Double]("probability", descr="Probability of finding a result over a certain threshold")
			    val tanimoto = opt[Boolean]("tanimoto", descr="Use tanimoto coefficients.", default=Some(false))
			    val target = opt[String]("target", required=true, descr="Make an SDF file of the search results. First molecule is the query, second is the match, third is the overlap.")
			    
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

			
			
		}
		
		Logger.setVerbosity( 1)
		
		if( opts.subcommand == Some(opts.compress)){
			var compType = CompressionType.FULLY_LABELED

			val compressor = new CachingStructCompressor( compType )
			compressor.compress(java.util.Arrays.asList(opts.compress.source().toArray: _*), opts.compress.target(), opts.compress.threads())

			// if( opts.compress.simple()){
			// compType = CompressionType.BASIC 
			// } else if( opts.compress.labeled()){
			// 	compType = CompressionType.FULLY_LABELED
			// } else if( opts.compress.weighted()){
			// 	compType = CompressionType.WEIGHTED
			// } else if( opts.compress.connection_2()){
			// 	compType = CompressionType.CONNECTION_2

			// } else if( opts.compress.connection_3()){
			// 	compType = CompressionType.CONNECTION_3

			// } else if( opts.compress.connection_4()){
			// 	compType = CompressionType.CONNECTION_4

			// } else if( opts.compress.connection_5()){
			// 	compType = CompressionType.CONNECTION_5

			// } else if( opts.compress.connection_6()){
			// 	compType = CompressionType.CONNECTION_6
			// }

			// else if( opts.compress.overlap_4()){
			// 	compType = CompressionType.OVERLAP_4

			// } else if( opts.compress.overlap_5()){
			// 	compType = CompressionType.OVERLAP_5

			// } else if( opts.compress.overlap_6()){
			// 	compType = CompressionType.OVERLAP_6

			// } else if( opts.compress.overlap_7()){
			// 	compType = CompressionType.OVERLAP_7

			// } else if( opts.compress.overlap_8()){
			// 	compType = CompressionType.OVERLAP_8

			// } else if( opts.compress.overlap_9()){
			// 	compType = CompressionType.OVERLAP_9

			// } 

			// else if( opts.compress.binary_overlap_4()){
			// 	compType = CompressionType.BINARY_OVERLAP_4

			// } else if( opts.compress.binary_overlap_5()){
			// 	compType = CompressionType.BINARY_OVERLAP_5

			// } else if( opts.compress.binary_overlap_6()){
			// 	compType = CompressionType.BINARY_OVERLAP_6

			// }



		  
		} else if( opts.subcommand == Some(opts.search)){
			Logger.log("Option unsupported in beta version. Please use \'test\' subcommand for search. See README.")
		  
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
		} 
	}

		
	
}

