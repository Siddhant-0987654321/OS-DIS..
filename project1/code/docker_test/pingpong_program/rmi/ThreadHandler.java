/*
 * ThreadHandler is the service thread reading method's info,
 * executing method at server side, and returning result to
 * client.
 * @author: Kaiwen Sun and Wenjia Ouyang
 */
package rmi;

import java.lang.reflect.*;
import java.net.*;
import java.io.*;

public class ThreadHandler<T> extends Thread {
    private Socket clientSocket;
    private Skeleton<T> mySkeleton;
    private T myServer;
    private ObjectInputStream inStream;
    private ObjectOutputStream outStream;
    
    

    public ThreadHandler( Skeleton<T> mySkeleton, T myServer, Socket clientSocket) {
        this.myServer = myServer;
        this.mySkeleton = mySkeleton;
        this.clientSocket = clientSocket;
    }
    

    private boolean initIOStream(){
    	
    	try {
			outStream = new ObjectOutputStream(clientSocket.getOutputStream());
			outStream.flush();
		} catch (IOException e1) {
			RMIException rmiExp = new RMIException("Can't create ObjectOutputStream instance.",e1);
			//closeSocket(clientSocket);
			mySkeleton.service_error(rmiExp);
			return false;
		}
    	
    	try {
    		InputStream tmpInputStream = clientSocket.getInputStream();
			inStream = new ObjectInputStream(tmpInputStream);
		} catch (IOException e) {
			RMIException rmiExp = new RMIException("Can't create ObjectInputStream instance.",e);
			mySkeleton.service_error(rmiExp);
			return false;
		}
    	return true;
    }
    public void run() {
    	if(mySkeleton.stopFlag.getStatus()!=StopFlag.RUNNING || clientSocket==null || clientSocket.isClosed())
    		return;
    	MethodCall methodCall = null;
    	ReturnObj rtnObj = null;
    	
    	initIOStream();
    	if(inStream!=null)
    		methodCall = receiveMethodCall();
    	if(outStream!=null)
    		rtnObj = invokeMethod(methodCall);
    	reply(rtnObj);
    	cleanStream();
    }
    
    /**
     * close possibly opened Object I/O streams and client socket
     */
    private void cleanStream(){
    	try{
    		this.inStream.close();
    	}
    	catch(IOException | NullPointerException e){    		
    	}
    	try{
    		this.outStream.close();
    	}
    	catch(IOException | NullPointerException e){    		
    	}
    	try{
    		this.clientSocket.close();
    	}
    	catch(IOException | NullPointerException e){    		
    	}
    }
    
    private MethodCall receiveMethodCall(){
    	MethodCall methodCall = null;
    	
    	try {
			methodCall = (MethodCall)inStream.readObject();
		} catch (ClassNotFoundException e1) {
			RMIException rmiExp = new RMIException("Can't convert inStream's input to MethodCall instance.",e1);
			mySkeleton.service_error(rmiExp);
			return null;
		} catch (IOException e1) {
			RMIException rmiExp = new RMIException("Can't readObj from inStream.",e1);
			mySkeleton.service_error(rmiExp);
			return null;
		}
    	
    	return methodCall;
    }
    private ReturnObj invokeMethod(MethodCall methodCall){
    	if(methodCall==null)
    		return null;
    	ReturnObj rtnObject = new ReturnObj(null, new RMIException("ReturnObj is not initinalized"));
    	String methodName = methodCall.methodName;
    	Class<?>[] types = methodCall.types;
    	Object[] args = methodCall.args;
    	Method method = null;
    	try {
    		//if the method is not implemented, call service_error and reply client with an RMIException.
    		try{
    			method = myServer.getClass().getMethod(methodName, types);
    			mySkeleton.c.getMethod(methodName, types);//for security reason. method not in the interface should not be invoked, even if it throws RMIException
    		}
    		catch(NoSuchMethodException e){
    			String msg = "Method "+methodName+"(";
    			for (Class<?> type : types){
    				msg += type.getName()+" ";
    			}
    			msg+=") is not implemented.";
    			RMIException rmiExp = new RMIException(msg,e);
    			mySkeleton.service_error(rmiExp);
    			rtnObject.set(null, rmiExp);
    			return rtnObject;
    		}
    		
    		//check whether the 
    		
        	
    		//if the method invocation throws an exception, reply the exception to client. otherwise, reply the return value to client.
            try{
    			method.setAccessible(true);
            	Object rtnValue = method.invoke(myServer, args);
            	rtnObject.set(rtnValue, null);
            }
            catch(InvocationTargetException invoExp){
            	rtnObject.set(null, invoExp.getTargetException());
            }
        } catch (Exception e) {
        	e.printStackTrace();        	
        	RMIException rmiexp = new RMIException("Fail to invoke method",e);
        	mySkeleton.service_error(rmiexp);
            rtnObject.set(null,rmiexp);
        }
    	return rtnObject;
    }
    private void reply(ReturnObj rtnObject){
    	if(rtnObject==null)
    		return;
    	
    	try {
    		outStream.writeObject(rtnObject);
    		outStream.flush();
		} catch (IOException e1) {
			RMIException rmiExp = new RMIException("Can't write or flush return value through ObjectOutputStream.",e1);
			mySkeleton.service_error(rmiExp);
		}
    }
}
