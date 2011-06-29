package Server;

import java.io.Serializable;

import Common.Message;

public class OperateMessage implements Serializable{
	public static int REPLY = 1;
	public static int TODO = 2;
	public static int BACKUP = 3;
	public static int HEART_BEAT = 4;
	public static int RESTORE_OK = 5;
	public static int RESTORE_DATA = 6;
	public static int currentseq = 0;
	private int type;
	private Message msg;
	private int seq;
	
	public OperateMessage(int type , Message msg ,int seq){
		this.type = type;
		this.msg = msg;
		this.seq = seq;
	}
	
	public static int getNextSeq(){
		currentseq = (currentseq + 1)%200;
		return currentseq;
	}
	
	public int getSeq() {
		return seq;
	}

	public void setSeq(int seq) {
		this.seq = seq;
	}

	public int getType() {
		return type;
	}
	public void setType(int type) {
		this.type = type;
	}
	public Message getMsg() {
		return msg;
	}
	public void setMsg(Message msg) {
		this.msg = msg;
	}
	
	
}
