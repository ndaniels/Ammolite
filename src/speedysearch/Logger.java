package speedysearch;

public class Logger {

	private static int verbosity = 1;// Normal level output, 0 is quiet, 2 slightly verbose, etc
	private static boolean showDebug = true;
	private static boolean experimenting = false;
	
	public static void bugOut(boolean _showDebug){
		showDebug = _showDebug;
	}
	
	public static void experimenting( boolean _experimenting){
		experimenting = _experimenting;
	}
	
	
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
	
	public static void debug(Object in){
		if( showDebug){
			System.out.println(in);
		}
	}
	
	public static void error(Object in){
		System.err.println(in);
	}
	
	public static void experiment(Object in){
		if( experimenting){
			System.out.println( in);
		}
	}
}
