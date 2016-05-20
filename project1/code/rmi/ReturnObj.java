/*
 * The class describing the return value and exception of remote method invocation (RMI)
 * @author:Kaiwen Sun
 */
package rmi;

import java.io.Serializable;

public class ReturnObj implements Serializable {
	private static final long serialVersionUID = 8997688101694000614L;
	private Object rtnValue = null;
	private Throwable throwable = null;
	
	public ReturnObj (Object rtnValue, Throwable throwable) {
		set(rtnValue,throwable);
	}
	public void set (Object rtnValue, Throwable throwable) {
		if(rtnValue!=null){
			this.rtnValue = rtnValue;
			this.throwable = null;
		}
		else{
			this.rtnValue = null;
			this.throwable = throwable;
		}
	}
	public Object getRtnValue(){
		return rtnValue;
	}
	public Throwable getThrowable(){
		return throwable;
	}
	public boolean isException(){
		return throwable!=null;
	}
}
