package edu.mit.csail.ammolite.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ParallelUtils {
	
		private static ExecutorService getExecutorService(){
			int numThreads = Runtime.getRuntime().availableProcessors();
			
			return Executors.newFixedThreadPool(numThreads);
		}
		
        private static ExecutorService getExecutorService(int numThreads){
            if(numThreads <= 0){
                return getExecutorService();
            }
            return Executors.newFixedThreadPool(numThreads);
        }
        
        public static <T> List<T> parallelFullExecution(List<Callable<T>> callList){
            return parallelFullExecution(callList, -1);
        }
		
		/**
		 * Runs a set of callables in parallel. 
		 * 
		 * Shuts down on encountering an exception
		 * 
		 * @param callables
		 * @return
		 */
		public static <T> List<T> parallelFullExecution(List<Callable<T>> callList, int numThreads){
			
			ExecutorService service = getExecutorService(numThreads);
			List<Future<T>> futures = new ArrayList<Future<T>>( callList.size());
			
			for(Callable<T> callable: callList){
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
		    return parallelSingleExecution(callList, -1);
		}
		
		/**
		 * Runs a set of callables in parallel until one returns a non-null result which is the final return value.
		 * 
		 * Ignores exceptions in threads.
		 * 
		 * @param callList
		 * @return The first non null result or null if no non null result was obtained.
		 */
		public static <T> T parallelSingleExecution(List<Callable<T>> callList, int numThreads){
			ExecutorService service = getExecutorService(numThreads);
			CompletionService<T> ecs = new ExecutorCompletionService<T>( service);
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
			service.shutdown();
			return result;
		}
		
		public static <T> T parallelTimedSingleExecution(List<Callable<T>> callList, long timeoutInMillis){
		    return parallelTimedSingleExecution(callList, timeoutInMillis, -1);
		}
		
		public static <T> T parallelTimedSingleExecution(List<Callable<T>> callList, long timeoutInMillis, int numThreads){
			ExecutorService service = getExecutorService(numThreads);
			CompletionService<T> ecs = new ExecutorCompletionService<T>( service);
			List<Future<T>> futures = new ArrayList<Future<T>>( callList.size());
			T result = null;
			try{
				for(Callable<T> task: callList){
					futures.add(ecs.submit(task));
				}
				for(int i=0; i<callList.size(); ++i){
					try{
						T r = ecs.take().get(timeoutInMillis, TimeUnit.MILLISECONDS);
						if( r != null){
							result = r;
							break;
						}
					} catch (ExecutionException ignore) {	
					} catch (InterruptedException ignore) {
					} catch (TimeoutException ignore) {}
				}
				
			} finally {
				for( Future<T> f: futures){
					f.cancel(true);
				}
			}
			service.shutdown();
			return result;
		}
		
		
		
	
}
