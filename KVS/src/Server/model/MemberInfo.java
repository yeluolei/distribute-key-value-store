package Server.model;

public class MemberInfo {
	private String name;
	private int status;
	
	public static int ACTIVE = 1;
	public static int DIED = 2;
	public static int DYING = 3;
	public static int RESTORING = 4;
	
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
