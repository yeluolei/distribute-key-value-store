package Client;


import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;

import Common.Message;

public class MsgClient {
		Socket socket;
		Listen listen;
		boolean is_msg = false;
    	InputStream is;
    	ObjectInputStream dis;
		OutputStream outputStream;
		ObjectOutputStream dos;
		
		public MsgClient(String serverName, int port){			       
	    	try{
	    		InetAddress address = InetAddress.getByName(serverName);
	    	    socket = new Socket(address, port);	    		
	    		outputStream = socket.getOutputStream();
				dos = new ObjectOutputStream(outputStream);
				is = socket.getInputStream();
    			dis = new ObjectInputStream(is);
	    	}
	    	catch(IOException e){e.printStackTrace();}
	    	listen = new Listen();	
	    	 listen.start();
		}
		
		public void SendMsg(Message msg){
			try{
				dos.writeObject(msg);
				dos.flush();
				dos.reset();
			}catch(Exception e){
				System.out.println("send message failure");
				e.printStackTrace();
				}
		}
		class Listen extends Thread{
			boolean knowSeq = false;
			public void Listen(){
				this.start();
			}
			public void run(){
	    		try{  			
	    			while(true){
	    				Message msg = (Message)dis.readObject();
	    				if(msg.getOperation() == Message.GET){
	    					KvsClient.kvsLib.ReceiveData(msg);
	    				}
	    			}
	    		}catch(Exception e){
	    			System.out.println("uh-oh, server happens to break down\n quit");	    			
	    			System.exit(-1);
	    		//	e.printStackTrace();
	    			}
	    	}
		}
}

