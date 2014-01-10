package speedysearch;

public class Logger {

	private static int verbosity = 1;// Normal level output, 0 is quiet, 2 slightly verbose, etc
	
	public static void setVerbosity( int _verbosity){
		verbosity = _verbosity;
	}
	
	public static void log(Object in){
		log(in,1);
	}
	
	public static void log(Object in, int level){
		if( level <= verbosity){
			System.out.println(in);
		}
	}
}
