/*
    A helper class implemented InvocationHandler
    By Wenjia Ouyang
*/
package rmi;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

import javax.swing.text.rtf.RTFEditorKit;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;

import java.io.*;

public class MyProxy<T> implements InvocationHandler, Serializable {
	private static final long serialVersionUID = 1936019582148187934L;
	private Class<T> c;
	private InetAddress address;
	private int port;
  
	public MyProxy(InetSocketAddress sockaddr, Class<T> c) {
		this.c = c;
		port = sockaddr.getPort();
		address = sockaddr.getAddress();
	}
 
	public MyProxy(Skeleton<T> skeleton, Class<T> c) {
		this(skeleton.sockaddr, c);
	}

	/**
	 * check if two stubProxys are equal by comparing their classtype, server address and server port.
	 * assisted by computeStubTostring()
	 * @param proxy
	 * @param method
	 * @param args
	 * @return true if equal, false if not equal.
	 */
	private Boolean computeStubEqual(Object proxy, Method method, Object[] args){
		if (args[0] instanceof Proxy) {
			try{
				return computeStubTostring(proxy).equals(computeStubTostring(args[0]));
			}
			catch(IllegalArgumentException e){
				//proxy is null, or not a proxy instance
			}
		}
		return false;
	}
	
	/**
	 * compute stubProxy's hashcode. Based on classname, address and port. Assisted by method computeStubTostring.
	 * @param proxy
	 * @return the hashCode() of the concatenated classname, address and port.
	 * @throws IllegalArgumentException  if the argument is not a proxy instance
	 */
	private Integer computeStubHashcode(Object proxy) throws IllegalArgumentException{
		return computeStubTostring(proxy).hashCode();
	}
	
	/**
	 * Convert a stub proxy to a string of classtype, server address, port.
	 * @param proxy
	 * @return a string representing the stub proxy
	 * @throws IllegalArgumentException  if the argument is not a proxy instance
	 */
	private String computeStubTostring(Object proxy) throws IllegalArgumentException{
		@SuppressWarnings("unchecked")
		MyProxy<T> stubProxy = (MyProxy<T>) Proxy.getInvocationHandler(proxy);	//may throw IllegalArgumentException
		return stubProxy.c.getName() +" " + stubProxy.address.toString() + " " + Integer.toString(stubProxy.port);
	}

	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		if (proxy ==null || method ==null)
			return false;//??false? return Object? or throw an exception?

		String methodName = method.getName();
		String returnType = method.getReturnType().getName();
		
		//override three methods
		//boolean equals(Object obj)
		if (methodName.equals("equals")
				&& returnType.equals("boolean")
				&& method.getParameterTypes().length==1
				&& (args[0]==null || method.getParameterTypes()[0].getName().equals("java.lang.Object"))) {
			return computeStubEqual(proxy, method, args);
		}		
		
		//int hashCode()
		if ( methodName.equals("hashCode") 
				&& returnType.equals("int")
				&& method.getParameterTypes().length==0
				) {
			return computeStubHashcode(proxy);
		}
		
		//String toString()
		if ( methodName.equals("toString")
				&& returnType.equals("java.lang.String")
				&& method.getParameterTypes().length==0) {
			return computeStubTostring(proxy);
		}
		
		//normal remote method invocation (RMI)
		ReturnObj returnObj = sendAndReceive(method, args);
		return extractReturnObj(returnObj);
	}
	
	/**
	 * If returnObj indicates an exception, throw that exception; if returnObj indicates a regular return value, return that value.
	 * @param returnObj ReturnObj received from skeleton
	 * @return return value from skeleton
	 * @throws Throwable whatever thrown by server implementation or skeleton's crash
	 */
	Object extractReturnObj(ReturnObj returnObj) throws Throwable{
		if(returnObj==null)
			return null;
		if(returnObj.isException()){
			throw returnObj.getThrowable();	
		}
		else {
			return returnObj.getRtnValue();
		}
	}
	
	/**
	 * send method and parameters to skeleton, receive return-value or exception from skeleton.
	 * open stream with skeleton for method call invocation and response
	 * @param method RMI
	 * @param parameters parameters of RMI
	 * @return ReturnObj describing return-value or exception
	 */
	private ReturnObj sendAndReceive(Method method,Object[] args) throws RMIException{
		Socket clientSocket = null;
		ObjectOutputStream outStream = null;
		ObjectInputStream inStream = null;
		ReturnObj returnObj = null;

		//Initialization sending
		try {

			clientSocket = new Socket(address, port);
			outStream = new ObjectOutputStream(clientSocket.getOutputStream());
			outStream.flush();
		} catch (IOException e) {
			closeSocket(clientSocket);
			throw new RMIException("Can't connect to skeleton.",e);
		}

		//send
		try {
			outStream.writeObject(new MethodCall(method, args));
			outStream.flush();
		} catch (IOException e) {
			closeSocket(clientSocket);
			throw new RMIException("Can't writeObject to outStream.",e);
		}
			
		//Initialization receiving
		try {
			inStream = new ObjectInputStream(clientSocket.getInputStream());
		} catch (IOException e1) {
			closeSocket(clientSocket);
			throw new RMIException("Can't create ObjectInputStream instance when receive from skeleton."+e1.getMessage(),e1);
		}
		
		//receive
		try {
			returnObj = (ReturnObj)inStream.readObject();
		} catch (ClassNotFoundException e1) {
			closeSocket(clientSocket);
			throw new RMIException("Can't convert received object to ReturnObj.",e1);
		} catch (IOException e1) {
			closeSocket(clientSocket);
			throw new RMIException("Can't read from inStream when receive from skeleton.",e1);
		}
		
		
		//cleaning
		try {
			outStream.close();
			inStream.close();
			clientSocket.close();
		} catch (Exception e) {
			throw new RMIException("can't close network I/O stream or client socket.");
		}
		
		return returnObj;
	}
	
	/**
	 * close socket and suppress potential exception
	 * @param socket
	 */
	private static void closeSocket(Socket socket){
		if(socket==null)
			return;
		try {socket.close();}
		catch (IOException e) {}
	}
}
