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
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Handler;

import javax.xml.crypto.Data;

import Serveractivition.MultiMsg;

import Common.Configure;
import Common.Message;
import Common.PacketHandle;


public class MsgServer{
	 private MulticastSocket groupSocket = null;
	 private boolean tokenIn = false;
	 private int groupIndex;
	 private String hostname;               //
	 private Vector<MemberInfo> membertable;//
	 private boolean isMaster;
	 
	 public MsgServer(int groupindex ,int hostindex) throws IOException{
		 groupIndex = groupindex;
		 this.hostname = "ServerGroup_"+groupindex+"_Host_"+hostindex;
		 
		 // 加入group
		 groupSocket = new MulticastSocket(Integer.valueOf(Configure.getInstance().getValue("ServerPort"+groupindex)));
		 //groupSocket.setLoopbackMode(true);
		 InetAddress group = InetAddress.getByName(Configure.getInstance().getValue("ServerGroup"+groupindex));
		 groupSocket.joinGroup(group);
		 tokenIn = true;
		 
		 // 检测member
		 membertable = new Vector<MemberInfo>();//
		 new MainServer().start();
		 Timer timer1 = new Timer();
	     timer1.schedule(new Detector(), 0, 3000);
		 Timer timer2 = new Timer();
	     timer2.schedule(new SendHeartBeat(), 0, 1000);
	 }
	 
	 public void MulticastMsg(OperateMessage msg){
		 try {
				groupSocket.send(PacketHandle.getDatagram(msg,groupIndex));
			} catch (IOException e) {
				e.printStackTrace();
			}
	}
	 
	 class MainServer extends Thread{
		 public MainServer(){}
		 public void run(){
			 while (true){
				byte[] recvBuf = new byte[5000];
				DatagramPacket packet = new DatagramPacket(recvBuf, recvBuf.length);
				try{
					groupSocket.receive(packet);
					OperateMessage msg = PacketHandle.getMessage(packet, recvBuf);
					
					if (msg.getType() == OperateMessage.TODO){
						System.out.println(groupIndex + " Receive: "+msg.getMsg().getData().toString() + "\n");
						if (msg != null){
							RequestHandle requestHandle = new RequestHandle(msg);
							requestHandle.start();
						}
					}else if (msg.getType()== OperateMessage.HEART_BEAT){ //
						String name = (String) msg.getMsg().getData().get("name");  //
						boolean alreadyIsMember = false;
						for (int i = 0 ; i < membertable.size() ;i++){
							if (membertable.get(i).getName().equals(name)){
								alreadyIsMember = true;
								if (membertable.get(i).getStatus() == MemberInfo.RESTORING){
									break;
								}
								else if(membertable.get(i).getStatus() == MemberInfo.DYING) {
									membertable.get(i).setStatus(MemberInfo.ACTIVE);
									break;
								}else if (membertable.get(i).getStatus() == MemberInfo.DIED) {
									membertable.get(i).setStatus(MemberInfo.RESTORING);
									if (isMaster){
										 new Restore().start();
									}
								}
							}
						}
						
						if (!alreadyIsMember){
							MemberInfo memberInfo = new MemberInfo();
							memberInfo.setName(name);
							memberInfo.setStatus(MemberInfo.ACTIVE);
							membertable.add(memberInfo);
						}
					}else if (msg.getType() == OperateMessage.RESTORE_OK) {
						String name = (String) msg.getMsg().getData().get("name"); 
						for (int i = 0 ; i < membertable.size() ;i++){
							if (membertable.get(i).getName().equals(name)){
									membertable.get(i).setStatus(MemberInfo.ACTIVE);
									break;
								}
							}
					}else if (msg.getType() == OperateMessage.RESTORE_DATA) {
						ArrayList<Message> data = (ArrayList<Message>) msg.getMsg().getData().get("data");
						for (int i = 0; i < data.size(); i++) {
							KVSServer.kvs.Put((String)data.get(i).getData().get("key"), 
									(byte[])data.get(i).getData().get("value"));
						}
						System.out.println("\n*************\n Restore ok \n**************\n");
						Message message = new Message();
						message.setValue("name", hostname);
						groupSocket.send(PacketHandle.getDatagram(
								new OperateMessage(OperateMessage.RESTORE_OK,message , 0), groupIndex));
					}
				}catch (Exception e) {
					e.printStackTrace();
				}
			 }
		 }
	 }
	 
	 class Restore extends Thread{
		 public void run(){
			 Message message = new Message();
			 message.setValue("data", KVSServer.kvs.GetAll());
			 try {
				groupSocket.send(PacketHandle.getDatagram(
						 new OperateMessage(OperateMessage.RESTORE_DATA, message, 0),
						 groupIndex));
			} catch (IOException e) {
				e.printStackTrace();
			}
		 }
	 }
	 
	 class RequestHandle extends Thread{
		private OperateMessage msg;

		public RequestHandle(OperateMessage msg) {
			this.msg = msg;
		}

		@SuppressWarnings("unchecked")
		public void run() {
			if (msg.getMsg().getOperation() == Message.PUT) {
				@SuppressWarnings("rawtypes")
				HashMap data = msg.getMsg().getData();
				KVSServer.kvs.Put((String) data.get("key"),
						(byte[]) data.get("value"));
				msg.getMsg().setValue("status", "success");
				msg.setType(OperateMessage.REPLY);
				try {
					groupSocket.send(PacketHandle.getDatagram(msg,groupIndex));
				} catch (IOException e) {
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
						groupSocket.send(PacketHandle.getDatagram(msg,groupIndex));
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
	 
	 // 发送心跳检测信号
	 class SendHeartBeat extends TimerTask{
			@Override
			public void run() {
					Message message = new Message();
					message.setValue("name",hostname);
					try {
						groupSocket.send(PacketHandle.getDatagram(
								new OperateMessage(OperateMessage.HEART_BEAT,message,0),
								groupIndex));
					} catch (IOException e) {
						e.printStackTrace();
					}
			}
			
		}
	 
	 // 每3秒钟检查一下
	class Detector extends TimerTask{
		@Override
		public void run() {
			System.out.println("\n\n--------------------------------------");
			for (int i = 0 ; i < membertable.size(); i++){
				System.out.println(membertable.get(i).getName()+"\t"+membertable.get(i).getStatus());
				if (membertable.get(i).getStatus() == MemberInfo.DYING){
					membertable.get(i).setStatus(MemberInfo.DIED);
				}else if (membertable.get(i).getStatus() == MemberInfo.ACTIVE) {
					membertable.get(i).setStatus(MemberInfo.DYING);
				}
			}
		}
		
	} 	 
}

//public class MsgServer extends ServerSocket {
//	private int seq = 0;
//
//	//List<CreateServerThread> waitingThread = new ArrayList<CreateServerThread>();
//	List<CreateServerThread> servingThread = new ArrayList<CreateServerThread>();
//	NewClient new_client;
//	
//    public MsgServer(String addr,int port) throws IOException{
//    	super(port);
//    	new_client = new NewClient();
//    	new_client.start();
//    }
//    class NewClient extends Thread{
//    	public NewClient(){}
//    	public void run(){
//    		try{
//    			try{
//    	    		while(true){
//    	    			Socket socket = accept();
//    	    			CreateServerThread client = new CreateServerThread(socket);
//    	    			servingThread.add(client);
//    	    			client.start();
//    	    		}
//    	    	}catch(IOException e){}
//    	    	finally{
//    	    		close();
//    	    	}
//    		}catch(Exception e){}
//    	}
//    }
//    //each thread for each client
//    class CreateServerThread extends Thread{
//    	Socket client;
//    	InputStream is;
//    	ObjectInputStream dis;
//    	OutputStream outputStream;
//		ObjectOutputStream dos;
//
//    	private final ReentrantLock lock = new ReentrantLock();
//    	String name;
//    	
//    	public CreateServerThread(Socket s) throws IOException{
//    		client = s;
//    		try{
//    			is = s.getInputStream();
//    			dis = new ObjectInputStream(is);
//    			outputStream = s.getOutputStream();
//				dos = new ObjectOutputStream(outputStream);
//    		}catch(Exception e){e.printStackTrace();}
//    	} 	
//    	public void run(){
//    		try{
//    			while(true){
//    				Message msg = (Message)dis.readObject(); // receive from client
//    				if(msg.operation == Message.PUT){
//    					HashMap data = msg.getData();
//    					KVSServer.kvs.Put((String)data.get("key"), (byte[])data.get("value"));
//    				}
//    				if(msg.operation == Message.GET){
//    					HashMap data = msg.getData();
//    					byte[] value = KVSServer.kvs.Get((String)data.get("key"));
//    					// if success
//    					Message success = new Message();
//    					data.put("value", data);
//    					success.setData(data);
//    					SendMsg(success);
//    				}
//    			}
//    		}catch(Exception e){}
//    	}
//    	public void SendMsg(Message msg){ //send to client
//    		try{
//    			dos.writeObject(msg);
//				dos.flush();
//				dos.reset();
//    		}catch(IOException ex){} 
//    	}
//    }
//}
