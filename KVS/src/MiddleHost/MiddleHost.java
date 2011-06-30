package MiddleHost;

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
import java.util.List;
import java.util.Vector;
import java.util.concurrent.locks.ReentrantLock;

import Common.Configure;
import Common.Message;
import Common.PacketHandle;
import MiddleHost.NewNodeServer.AcceptThread;
import MiddleHost.NewNodeServer.CreateServerThread;
import Server.OperateMessage;

public class MiddleHost extends ServerSocket {
	AcceptThread acceptThread;
	//private MasterClient[][] toMasters;
	private ArrayList<ArrayList<MasterClient>> toMasters;
	//private static ServerSocket serverSocket;
	private Vector<OperateMessage> replyMsg;	
	private ReplyGuestThread replyGuest;	
	public MiddleHost(int port)throws IOException{
		super(port);
		replyMsg = new Vector<OperateMessage>();
		acceptThread = new AcceptThread();
		acceptThread.start();
		try {	
			int groupnum = Integer.valueOf(Configure.getInstance().getValue("ServerGroupNum"));
			toMasters = new ArrayList<ArrayList<MasterClient>>();
			for (int index = 0 ; index < groupnum ; index++){
				ArrayList<MasterClient> groupClients = new ArrayList<MasterClient>();
				toMasters.add(groupClients);
				//Integer.valueOf(Configure.getInstance().getValue("ServerPort"+index)
				int num = Integer.valueOf(
						Configure.getInstance().getValue("ServerGroup_" + index + "_Num"));
				//toMasters[index] = new MasterClient[num];
				for(int j = 0; j < num; j++){
					String ip = Configure.getInstance().getValue("ServerGroup_" + index 
							+ "_Host_" + j +  "_Addr").toString();
					int sPort = Integer.valueOf(Configure.getInstance().getValue("ServerGroup_" + index 
							+ "_Host_" + j +  "_Port"));
					MasterClient masterClient = new MasterClient(ip, sPort, index, j);
					toMasters.get(index).add(masterClient);
				}
			}	
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public void AddMasterClient(int group, int index, MasterClient master){
		
	}
	public void PutNewReply(OperateMessage msg){
		replyMsg.add(msg);
	}
	class AcceptThread extends Thread{
		public AcceptThread(){}
    	public void run(){
    		try{
    			try{
    	    		while(true){
    	    			Socket socket = accept();
    	    			CreateServerThread client = new CreateServerThread(socket);
    	    			//	servingThread.add(client);
    	    				client.start();
    	    		}
    	    	}catch(IOException e){}
    	    	finally{
    	    		close();
    	    	}
    		}catch(Exception e){}
    	}
	}
	//msg.getData().get("key").toString();
	
	 class CreateServerThread extends Thread{
	    	Socket client;
	    	InputStream is;
	    	ObjectInputStream dis;
	    	OutputStream outputStream;
			ObjectOutputStream dos;
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
	    				Message msg = (Message)dis.readObject(); // receive from client
	    				String key = (String)msg.getData().get("key");
	    				int hash = key.hashCode();
	    				int group = hash % toMasters.size();
	    				int member = hash % toMasters.get(group).size();
	    				msg.getData().put("socket", client);
	    				//
	    				OperateMessage opMsg = new OperateMessage(OperateMessage.TODO,msg, 0);
	    				//convert message to operateMessage
	    				toMasters.get(group).get(member).SendMsg(opMsg);
	    			}
	    		}catch(Exception e){}
	    	}
	 }
	
	class ReplyGuestThread extends Thread{
		public ReplyGuestThread(){}
		public void run(){
			while(replyMsg.size() > 0){
				OperateMessage opMsg = replyMsg.get(0);
				Message msg = opMsg.getMsg();
				Socket socket = (Socket)msg.getData().get("socket");
				OutputStream outputStream;
				ObjectOutputStream dos;			
				try {
					outputStream = socket.getOutputStream();
					dos = new ObjectOutputStream(outputStream);
					dos.writeObject(msg);
					dos.flush();
					dos.reset();
				} catch (Exception e) {
					System.out.println("send message failure");
					e.printStackTrace();
				}
				replyMsg.remove(0);
			}
		}
	}
}
