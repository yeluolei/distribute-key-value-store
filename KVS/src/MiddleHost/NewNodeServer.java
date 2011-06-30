package MiddleHost;


import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import Server.OperateMessage;

public class NewNodeServer extends ServerSocket {
	AcceptThread acceptThread;
	List<CreateServerThread> servingThread = new ArrayList<CreateServerThread>();
	
	
	public NewNodeServer(int port)throws IOException{
		super(port);
		acceptThread = new AcceptThread();
		acceptThread.start();
	}
	class AcceptThread extends Thread{
		public AcceptThread(){}
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
	
	 class CreateServerThread extends Thread{
	    	Socket client;
	    	InputStream is;
	    	ObjectInputStream dis;
	    	OutputStream outputStream;
			ObjectOutputStream dos;

	    	private final ReentrantLock lock = new ReentrantLock();
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
	    				if(msg.getType() == OperateMessage.NEW_SERVER_IN){
	    					//
	    					int group = (Integer) (msg.getMsg().getData().get("groupindex"));
	    					int port = (Integer) (msg.getMsg().getData().get("hostport"));
	    					String addr = (String) msg.getMsg().getData().get("hostaddr");
	    					int  hostindex = (Integer) msg.getMsg().getData().get("hostindex");
	    					MasterClient masterClient = new MasterClient(addr, port, group, hostindex);
	    				}
	    			}
	    		}catch(Exception e){}
	    	}
	    	/*public void SendMsg(Message msg){ //send to client
	    		try{
	    			dos.writeObject(msg);
					dos.flush();
					dos.reset();
	    		}catch(IOException ex){} 
	    	}*/
	 }
}
