/*
 * ThreadListener is the listening thread at server side, accepting connections from client
 * @author: Kaiwen Sun and Wenjia Ouyang
 */
package rmi;

import java.net.*;
import java.util.LinkedList;
import java.util.ListIterator;

import java.io.*;
import rmi.Skeleton;

public class ThreadListener <T>extends Thread {
    private ServerSocket listenerSocket;
    private Socket clientSocket;
    private Skeleton<T> mySkeleton;
    private int num_service_ever_created;
    
    private LinkedList<ThreadHandler<T>> threadHandlers = new LinkedList<ThreadHandler<T>>();

    //boolean flag;

    public ThreadListener(Skeleton<T> skeleton, ServerSocket listenerSocket) {
        this.listenerSocket = listenerSocket;
        mySkeleton = skeleton;
        num_service_ever_created = 0;
    }

    public void run() {
    	//listening
		synchronized (mySkeleton.sockaddr){
        while (mySkeleton.stopFlag.getStatus()==StopFlag.RUNNING) {
            try {
                clientSocket = listenerSocket.accept();
            } catch (IOException e) {

            	if(mySkeleton.stopFlag.getStatus()==StopFlag.RUNNING){
            		boolean cont = mySkeleton.listen_error(e);
            		if(cont)
            			continue;
            		else{
            			mySkeleton.stopFlag.setStatus(StopFlag.STOPPED);
                    	mySkeleton.stopped(e);
            		}
            	}
             	return;
            }
            if(mySkeleton.stopFlag.getStatus()!=StopFlag.RUNNING){
            	break;
            }
            cleanThreadList();
            ThreadHandler<T> response = new ThreadHandler<T>( mySkeleton,mySkeleton.myServer, clientSocket);
			synchronized(threadHandlers){
	            threadHandlers.add(response);
			}
            response.start();
        }
		}
    }
    
    /**
     * Terminate a Threadlistener thread.
     * 1. close the listenerSocket (which will cause the blocked listenerSocket.accept() to throw an exception)
     * 2. wait all ThreadHandler threads to finish execution (?? shall I spontaneously stop those threads?)
     */
    public synchronized void terminate(){
    	try {
			listenerSocket.close();
		} catch (IOException e) {
			/*
			 * These lines are commentized because I think we should always terminate all threads gracefully, even if there is an Exception.
			boolean cont = mySkeleton.listen_error(e);
			if(cont==false)
				return;
			*/
		}

		synchronized(threadHandlers){
			if(threadHandlers!=null){
		    	for(ThreadHandler<T> threadHandler : threadHandlers){
		    		try {
						threadHandler.join();
					} catch (InterruptedException e) {
					}
		    	}
			}
    	}    	
    }
    
    /**
     * Periodically remove inactive threads from the list of threadHandlers.
     */
    private synchronized void cleanThreadList(){
		synchronized(threadHandlers){
    	if(num_service_ever_created%10==0){
	    		ListIterator<ThreadHandler<T>> iterator = threadHandlers.listIterator();
	    		while(iterator.hasNext()){
	    			ThreadHandler<T> threadHandler = iterator.next();
	    			if(threadHandler.isAlive()==false)
	    				iterator.remove();
	    		}
	    	}
		}
    }

}


