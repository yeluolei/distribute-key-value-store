package Client;


import java.util.HashMap;


public class KvsLib {
	public int hasGet = 0;
	public HashMap rcvData;
	
	public void Put(String key, byte[]value){
		Message msg = new Message();
		msg.setOperation(Message.PUT);
		HashMap data = new HashMap();
		data.put("key", key);
		data.put("value", value);
		msg.setData(data);
		// send msg
		KvsClient.msgClient.SendMsg(msg);
	}
	public byte[] Get(String key){
		// construct
		Message msg = new Message();
		msg.setOperation(Message.GET);
		HashMap data = new HashMap();
		data.put("key", key);
		KvsClient.msgClient.SendMsg(msg);
		// send msg
		hasGet = 0;
		while(hasGet == 0){}
		return (byte[])rcvData.get("value");
	}
	public void ReceiveData(Message msg){
		rcvData = msg.getData();
		hasGet = 1;
	}
}
