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

public class ParallelWorkerPool {
    private static ExecutorService service;
    
    public ParallelWorkerPool(int numThreads){
        if( service == null){
            service = Executors.newFixedThreadPool(numThreads);
        }
    }
    
    public ParallelWorkerPool(){
        this(Runtime.getRuntime().availableProcessors());
    }
    
    /**
     * Runs a set of callables in parallel and returns the results in the list.
     * 
     * The ExecutorService is not shut down automatically.
     * 
     * Shuts down on encountering an exception
     * 
     * @param callList
     * @param service
     * @return 
     */
    public <T> List<T> parallelFullExecution(List<Callable<T>> callList){
        
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

        return results;
    
    }
    
    /**
     * Runs a set of callables in parallel until one returns a non-null result which is the final return value.
     * 
     * Ignores exceptions in threads.
     * 
     * @param callList
     * @return The first non null result or null if no non null result was obtained.
     */
    public static <T> T parallelSingleExecution(List<Callable<T>> callList){
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
        return result;
    }
    
    /**
     * Runs a list of callables in parallel until one returns a non-null result
     * 
     * @param callList
     * @param timeoutInMillis
     * @return
     */
    public static <T> T parallelTimedSingleExecution(List<Callable<T>> callList, long timeoutInMillis){
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

          return result;
      }

    
    
    public void shutdown(){
        service.shutdown();
        service = null;
    }
}
