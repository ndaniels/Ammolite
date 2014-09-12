package edu.mit.csail.ammolite


import edu.mit.csail.ammolite.compression.MoleculeStructFactory
import edu.mit.csail.ammolite.compression.StructCompressor
import org.rogach.scallop._
import edu.mit.csail.ammolite.utils.Logger
import edu.mit.csail.ammolite.compression.CyclicStruct
import edu.mit.csail.ammolite.database.CompressionType
import edu.mit.csail.ammolite.database.StructDatabaseDecompressor
import edu.mit.csail.ammolite.aggregation.Aggregator
import edu.mit.csail.ammolite.aggregation.AggregateSearcher
import edu.mit.csail.ammolite.aggregation.ClusterDatabaseDecompressor
import edu.mit.csail.ammolite.database.SDFWrapper
import edu.mit.csail.ammolite.compression.DatabaseCompression
import collection.mutable.Buffer
import collection.Seq


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
			  val threads = opt[Int]("threads", default=Some(-1), descr="Number of threads to use for compression")
			}
			val merge = new Subcommand("merge-databases"){
			  banner("Compress a database of SDF files")
			  val d1 = opt[String]("d1", required=true, descr="First database")
			  val d2 = opt[String]("d2", required=true, descr="Second database")
			  val target = opt[String]("target", required=true, descr="Name of the new compressed database")
			}
			
			val organize = new Subcommand("organize-database"){
			  banner("Organize a database")
			  val db = opt[String]("database", required=true, descr="database to be organized")
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
			val draw = new Subcommand("draw"){
			  val f = opt[String]("filename")
			  val s = opt[Boolean]("struct")
			}
			val structs = new Subcommand("as-structures"){
			  val i = opt[String]("in")
			  val o = opt[String]("out")

			}
			val devTestSDF = new Subcommand("test-sdf"){
			  val sdf = opt[String]("filename")
			}
			val devTestMCS = new Subcommand("test-mcs"){
			  val sdf = opt[String]("filename")
			}
			val devTestSearch = new Subcommand("test-ammolite"){
			  val q = opt[String]("queries")
			  val db = opt[String]("database")
			  val out = opt[String]("outName")
			  val t = opt[Double]("threshold")
			  val p = opt[Double]("prob")
			  val smsd = opt[Boolean]("SMSD", default=Some(false))
			  val par = opt[Boolean]("Parallel", default=Some(false))
			  val fmcs = opt[Boolean]("FMCS", default=Some(false))
			  val amm = opt[Boolean]("Amm", default=Some(false))
			  val ammSMSD = opt[Boolean]("AmmSMSD", default=Some(false))
			  val queryComp = opt[Boolean]("QueryCompression", default=Some(false))
			  val useCaching = opt[Boolean]("Caching", default=Some(false))
			}
			val devTestAggSearch = new Subcommand("test-agg"){
			  val q = opt[String]("queries")
			  val db = opt[String]("database")
			  val f = opt[Double]("fine")
			  val c = opt[Double]("coarse")
			  val a = opt[Double]("agg")
			}
			val devTestSMSD = new Subcommand("test-smsd"){
			   val mols = opt[String]("molecules")
			}
			val devTestFMCS = new Subcommand("test-fmcs"){
			  val mols = opt[String]("molecules")

			}
			val devTestAmmCoarse = new Subcommand("test-ammolite-coarse"){
			  val mols = opt[String]("molecules")

			}
			val examine = new Subcommand("examine"){
			  val database = trailArg[String]()
			  // val database = opt[String]("database", required=true, descr="Path to the database.")
			  val table = opt[Boolean]("table", default=Some(false))
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
		    val searchbound = opt[Double]("search-boundary", default=Some(0.5), descr="how similar molecules have to be to clusters to match")
		  }
			
		}
		
		Logger.setVerbosity( opts.verbosity())
		
		if( opts.subcommand == Some(opts.compress)){
		  var compType = CompressionType.CYCLIC
		  if( opts.compress.simple()){
		    compType = CompressionType.BASIC 
		  } 
			  
		  val compressor = new StructCompressor( compType )
		  java.util.Arrays.asList(opts.compress.source().toArray: _*)
		  compressor.compress(java.util.Arrays.asList(opts.compress.source().toArray: _*), opts.compress.target(), opts.compress.threads())
		  
		} else if( opts.subcommand == Some(opts.merge)){
		  edu.mit.csail.ammolite.compression.DatabaseCompression.mergeDatabases(opts.merge.d1(), opts.merge.d2(), opts.merge.target())
		  
		} else if( opts.subcommand == Some(opts.search)){
			
		  
		} else if( opts.subcommand ==Some(opts.mcs)){
		  Logger.log("Finding mcs of two molecules")
		  edu.mit.csail.ammolite.utils.MCSUtils.doFMCS(opts.mcs.molecules(), opts.mcs.sdf())
		  
		} else if( opts.subcommand == Some( opts.devTestMCS)){
		  edu.mit.csail.ammolite.tests.MCSTest.testMCS(opts.devTestMCS.sdf())
		}  else if( opts.subcommand == Some( opts.devTestSDF)){
		  val s = new SDFWrapper( opts.devTestSDF.sdf())
		} else if( opts.subcommand == Some( opts.devTestSearch)){

			  edu.mit.csail.ammolite.tests.SearchTest.testSearch(opts.devTestSearch.q(), opts.devTestSearch.db(), opts.devTestSearch.out(), opts.devTestSearch.t(), opts.devTestSearch.p(), 
					  												opts.devTestSearch.amm(),
					  												opts.devTestSearch.par(),
					  												opts.devTestSearch.queryComp(),
					  												opts.devTestSearch.smsd(),
					  												opts.devTestSearch.fmcs(),
					  												opts.devTestSearch.ammSMSD(),
					  												opts.devTestSearch.useCaching())
		  
		  
		} else if( opts.subcommand == Some( opts.devTestAggSearch)){
		  edu.mit.csail.ammolite.tests.AggregateSearchTest.testAggSearch(opts.devTestAggSearch.db(), opts.devTestAggSearch.q(), opts.devTestAggSearch.f(), opts.devTestAggSearch.c(), opts.devTestAggSearch.a())
		  
		} else if( opts.subcommand == Some( opts.devTestSMSD)){
		  edu.mit.csail.ammolite.tests.SearchTest.testSMSD(opts.devTestSMSD.mols())
		}else if( opts.subcommand == Some( opts.devTestFMCS)){
		  edu.mit.csail.ammolite.tests.SearchTest.testFMCS(opts.devTestFMCS.mols())
		} else if( opts.subcommand == Some( opts.devTestAmmCoarse)){
		  edu.mit.csail.ammolite.tests.SearchTest.testAmmoliteCoarse(opts.devTestAmmCoarse.mols())
		} else if( opts.subcommand == Some( opts.examine)){
			val db = StructDatabaseDecompressor.decompress( opts.examine.database())
		    Logger.log(db.info())
			if(opts.examine.table()){  
		    	Logger.log(db.asTable())
		    }
		} else if( opts.subcommand == Some( opts.draw)){
		  if(opts.draw.s()){
		    edu.mit.csail.ammolite.MolDrawer.drawAsStruct(opts.draw.f())
		  }
		  edu.mit.csail.ammolite.MolDrawer.draw(opts.draw.f())
		} else if( opts.subcommand == Some( opts.structs)){
		  edu.mit.csail.ammolite.utils.DevUtils.makeStructFile(opts.structs.i(), opts.structs.o())
		} else if(opts.subcommand == Some( opts.aggcompress)){

		    val agg = new Aggregator( opts.aggcompress.source(), opts.aggcompress.repbound())
		    agg.aggregate(opts.aggcompress.target(), opts.aggcompress.linearizeClustering())
		} else if( opts.subcommand == Some( opts.aggsearch)){

		    val aggSearcher = new AggregateSearcher(opts.aggsearch.cluster(), opts.aggsearch.searchbound())
		    
		    aggSearcher.doSearch(opts.aggsearch.queries(), opts.aggsearch.target(), opts.aggsearch.threshold(), opts.aggsearch.tanimoto())
		} else if( opts.subcommand == Some( opts.organize)){
			DatabaseCompression.organizeDatabase(opts.organize.db())
		}
	}

		
	
}

