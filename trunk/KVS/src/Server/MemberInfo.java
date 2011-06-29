package Server;

import java.beans.IntrospectionException;

public class MemberInfo {
	private String name;
	private int status;
	
	static int ACTIVE = 1;
	static int DIED = 2;
	static int DYING = 3;
	static int RESTORING = 4;
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public int getStatus() {
		return status;
	}
	public void setStatus(int status) {
		this.status = status;
	}
	
}
