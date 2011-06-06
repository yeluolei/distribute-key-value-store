package Server;


import java.io.Serializable;
import java.util.HashMap;


public class Message implements Serializable{
	public static int GET = 1;
	public static int PUT = 2;
	int operation;
	HashMap data;
	public Message(){
		operation = 0;
		data = new HashMap();
	}
	public int getOperation() {
		return operation;
	}
	public void setOperation(int operation) {
		this.operation = operation;
	}
	public HashMap getData() {
		return data;
	}
	public void setData(HashMap data) {
		this.data = data;
	}
	
}
