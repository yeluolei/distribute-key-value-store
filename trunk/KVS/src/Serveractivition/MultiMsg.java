package Serveractivition;

import java.io.Serializable;

public class MultiMsg implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 4285977738049056295L;
	private String ip;
	private int port;
	private String attr;
	
	public MultiMsg(String ip , int port , String attr){
		this.ip = ip;
		this.port = port;
		this.attr = attr;
	}
	
	public String Getattr(){         //  get attribute
		return this.attr;
	}
	
	public boolean equals(Object obj){
        if(this == obj){
        	return true;
        }
        if (!(obj instanceof MultiMsg)) {
            return false;
        }
        MultiMsg msg = (MultiMsg)obj;
        if (this.ip.equals(msg.ip) && this.port == msg.port){
                return true;
        }else{
                return false;
        }       
	}
	
    public int hashCode()
    {
        return this.ip.hashCode() * this.port;
    }
}
