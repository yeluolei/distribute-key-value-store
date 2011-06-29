package Server;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import Common.Configure;
import Common.Message;
import Common.PacketHandle;


public class MasterMsgServer extends ServerSocket {
	private int seq = 0;

	List<CreateServerThread> servingThread = new ArrayList<CreateServerThread>();
	NewClient new_client;
	int memberNum;
	boolean tokenIn;
	HashMap putReply = new HashMap();
	HashMap putClient = new HashMap();
	
	
	
    public MasterMsgServer(String addr,int port) throws IOException{
    	super(port);
    	new_client = new NewClient();
    	new_client.start();
    }
    public void RcvPutReply(OperateMessage msg){
    	HashMap data = msg.getMsg().getData();
    	String key = (String)data.get("key");
    	putReply.put(key, (Integer)putReply.get("key") + 1);
    	if((Integer)putReply.get("key") == memberNum){
    		Socket client = (Socket)putClient.get("key");
    		OutputStream outputStream;
    		ObjectOutputStream dos;
    		try{
    			outputStream = client.getOutputStream();
				dos = new ObjectOutputStream(outputStream);
				dos.writeObject(msg);
				dos.flush();
				dos.reset();
			}catch(Exception e){
				System.out.println("send message failure");
				e.printStackTrace();
			}
    	}
    	
    }
    
    
    
    class NewClient extends Thread{
    	public NewClient(){}
    	public void run(){
    		try{
    			try{
    	    		while(true){
    	    			Socket socket = accept();
    	    			CreateServerThread client = new CreateServerThread(socket);
    	    			servingThread.add(client);
    	    			client.start();
    	    		}
    	    	}catch(IOException e){}
    	    	finally{
    	    		close();
    	    	}
    		}catch(Exception e){}
    	}
    }
    //each thread for each client
    class CreateServerThread extends Thread{
    	Socket client;
    	InputStream is;
    	ObjectInputStream dis;
    	OutputStream outputStream;
		ObjectOutputStream dos;
		
    //	private final ReentrantLock lock = new ReentrantLock();
    	String name;
    	
    	public CreateServerThread(Socket s) throws IOException{
    		client = s;
    		try{
    			is = s.getInputStream();
    			dis = new ObjectInputStream(is);
    			outputStream = s.getOutputStream();
				dos = new ObjectOutputStream(outputStream);
    		}catch(Exception e){e.printStackTrace();}
    	} 	
    	public void run(){
    		try{
    			while(true){
    				OperateMessage msg = (OperateMessage)dis.readObject(); // receive from client
    				if (msg.getMsg().getOperation() == Message.PUT) {
    					@SuppressWarnings("rawtypes")
    					HashMap data = msg.getMsg().getData();
    					putReply.put(data.get("key"), 0);
    					putClient.put(data.get("key"), client);
    					try {
    						KVSServer.msgServer.MulticastMsg(msg);
    					} catch (Exception e) {
    						e.printStackTrace();
    					}
    				}
    				if (msg.getMsg().getOperation() == Message.GET) {
    					if (tokenIn){
    						@SuppressWarnings("rawtypes")
    						HashMap data = msg.getMsg().getData();
    						@SuppressWarnings("unused")
    						byte[] value = KVSServer.kvs.Get((String) data.get("key"));
    						// if success
    						Message success = new Message();
    						data.put("value", value);
    						success.setData(data);
    						
    						msg.setType(OperateMessage.REPLY);
    						msg.setMsg(success);
    						try {
    							SendMsg(msg);
    						} catch (Exception e) {
    							e.printStackTrace();
    						}
    					}
    				}
    			}
    		}catch(Exception e){}
    	}
    	public void SendMsg(OperateMessage msg){ //send to client
    		try{
    			dos.writeObject(msg);
				dos.flush();
				dos.reset();
    		}catch(IOException ex){} 
    	}
    }
}
