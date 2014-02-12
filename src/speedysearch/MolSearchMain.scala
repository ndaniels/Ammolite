package speedysearch

import org.rogach.scallop._


object MolSearchMain{
		
	def main(args: Array[String]){
		
		val opts = new ScallopConf(args){
			guessOptionName = true
			version("MolSearch 0.0.0")

			footer("\nHope you found your molecule!")
			
			val verbosity = opt[Int]("verbosity", default=Some(1), descr="Ste the verbosity. 0 is quiet, 2 is verbose, etc")
			
			val compress = new Subcommand("compress"){
			  banner("Compress a database of SDF files")
			  val source = opt[String]("source", required=true, descr="File or folder to compress")
			  val target = opt[String]("target", required=true, descr="Name of the new compressed database")
			  val makeSDF = opt[String]("sdf", descr="Make an SDF file of the cluster representatives and a file with pointers")
			}
			val search = new Subcommand("search"){
			  
				val big  = opt[Boolean]("big",  descr="Search for all matches above a supplied threshold")
				val database = opt[String]("database", required=true, descr="Path to the database.")
				val queries = opt[String]("queries", required=true, descr="SDF file of queries.")
				val threshold = opt[Double]("threshold", descr="Threshold to use. Uses overlap coefficient by default.")
			    val tanimoto = opt[Boolean]("tanimoto", descr="Use tanimoto coefficients.", default=Some(false))
			    val target = opt[String]("target", required=true, descr="Make an SDF file of the search results. First molecule is the query, second is the match, third is the overlap.")
			    
			    codependent( big, threshold)
			    
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
		fmcs.Logger.setVerbosity(opts.verbosity())
		
		if( opts.subcommand == Some(opts.compress)){
		
		  val exemplar = new RingStruct()
		  val structFactory = new MoleculeStructFactory( exemplar)
		  val compressor = new StructCompressor( structFactory)
		  compressor.compress(opts.compress.source(), opts.compress.target())
		  if(opts.compress.makeSDF.isDefined ){
		    compressor.makeSDF( opts.compress.makeSDF())
		  }
		  
		} else if( opts.subcommand == Some(opts.search)){
		  val decompressor = new StructDatabaseDecompressor()
		  val database = decompressor.decompress(opts.search.database())
		  Logger.log("Database contains " + database.numReps() + " representatives",4)
		  val searcher = new ParallelSearcher( database, opts.search.tanimoto())
		  
		  if( opts.search.big() ){
		    Logger.log("Running a big search with threshold "+opts.search.threshold())
		    searcher.doBigSearch( opts.search.queries(), opts.search.target(), opts.search.threshold())
		    
		  } else {
		    Logger.log("Running a quick search")
		    searcher.doQuickSearch( opts.search.queries(), opts.search.target())
		  }
		  
		} else if( opts.subcommand ==Some(opts.mcs)){
		  Logger.log("Finding fmcs of two molecules")
		  fmcs.FMCS.doFMCS(opts.mcs.molecules(), opts.mcs.sdf())
		  
		} else if( opts.subcommand == Some( opts.dev)){
		  fmcs.FMCS.getCoeffs(opts.dev.a(), opts.dev.b())
		} 
	}

		
	
}

