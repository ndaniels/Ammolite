package speedysearch

import org.rogach.scallop._

object MolSearchMain extends App{
		
		
		val opts = new ScallopConf(args){
			guessOptionName = true
			version("MolSearch 0.0.0")
			banner("""Usage: test [OPTION]... [pet-name]
					  Welcome To MolSearch!""".stripMargin)

			footer("\nHope you found your molecule!")
			
			val compress = new Subcommand("compress"){
			  banner("Compress a database of SDF files")
			  val source = opt[String]("source", required=true, descr="File or folder to compress")
			  val target = opt[String]("target", required=true, descr="Name of the new compressed database")
			}
			val search = new Subcommand("search"){
			  
				val big  = opt[Boolean]("big",  descr="Search for all matches above a supplied threshold")
				val database = opt[String]("database", required=true, descr="Path to the database.")
				val queries = opt[String]("queries", required=true, descr="SDF file of queries.")
				val coef = opt[Double]("coef", descr="Coefficient to use. Uses overlap coefficient by default.")
			    val tanimoto = opt[Boolean]("tanimoto", descr="Use tanimoto coefficients.")
			    
			    codependent( big, coef)
			    
			}
		}
		
		if( opts.subcommand == Some(opts.compress)){
		  val exemplar = new RingStruct()
		  val structFactory = new MoleculeStructFactory( exemplar)
		  val compressor = new StructCompressor( structFactory)
		  compressor.compress(opts.compress.source(), opts.compress.target())
		  
		} else if( opts.subcommand == Some(opts.search)){
		  val decompressor = new StructDatabaseDecompressor
		  val database = decompressor.decompress(opts.search.database())
		  val searcher = new MoleculeSearcher( database)
		  
		  if( opts.search.big() ){
		    println("big search")
		  } else {
		    println("quick search")
		  }
		}

		
	
}

