package edu.mit.csail.ammolite.aggregation;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

public class ParallelUtils {
	
		private static ExecutorService getExecutorService(){
			int numThreads = Runtime.getRuntime().availableProcessors();
			if( numThreads > 12){
				numThreads -= 4;
			}
			return Executors.newFixedThreadPool(numThreads);
		}
		
		public static <T> List<T> parallelFullExecution(List<Callable<T>> callables){
			
			ExecutorService service = getExecutorService();
			List<Future<T>> futures = new ArrayList<Future<T>>( callables.size());
			
			for(Callable<T> callable: callables){
				futures.add( service.submit( callable));
			}
			
			List<T> results = new ArrayList<T>();
			for( Future<T> future: futures){
				try {
					results.add( future.get());
				} catch (InterruptedException ie) {
					ie.printStackTrace();
					System.exit(1);
				} catch (ExecutionException ee) {
					ee.printStackTrace();
					System.exit(1);
				}
			}
			
			service.shutdown();
			return results;
		}
		
		public static <T> T parallelSingleExecution(List<Callable<T>> callList){
			CompletionService<T> ecs = new ExecutorCompletionService<T>( getExecutorService());
			List<Future<T>> futures = new ArrayList<Future<T>>( callList.size());
			T result = null;
			try{
				for(Callable<T> task: callList){
					futures.add(ecs.submit(task));
				}
				for(int i=0; i<callList.size(); ++i){
					try{
						T r = ecs.take().get();
						if( r != null){
							result = r;
							break;
						}
					} catch (ExecutionException ignore) {	
					} catch (InterruptedException ignore) {}
				}
				
			} finally {
				for( Future<T> f: futures){
					f.cancel(true);
				}
			}
			return result;
		}
		
	
}
