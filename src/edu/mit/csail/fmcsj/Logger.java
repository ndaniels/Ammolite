package edu.mit.csail.fmcsj;

public class Logger {

	private static int verbosity = 1;// Normal level output, 0 is quiet, 2 slightly verbose, etc
	private static boolean silenced = false;
	

	public static void setVerbosity( int _verbosity){
		verbosity = _verbosity;
	}
	
	public static void log(Object in){
		log(in,1);
	}
	
	public static void indentedLog(Object in, int ind){
		StringBuilder sb = new StringBuilder();
		for(int arb=0; arb<ind; arb++){
			sb.append("\t");
		}
		System.out.println(sb.toString()+in);
	}
	
	public static void log(Object in, int level){
		if( level <= verbosity && !silenced){
			System.out.println(in);
		}
	}
}
