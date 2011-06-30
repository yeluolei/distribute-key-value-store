package Server;



import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import Common.Configure;
import Common.Message;
import Common.PacketHandle;


public class MsgServer{
	 private MulticastSocket groupSocket = null;
	 private boolean tokenIn = false;
	 private int groupIndex;                // witch group this server is belong to 
	 @SuppressWarnings("unused")
	 private int hostIndex;                  // the index in the group
	 private String hostname;               // the name of this index
	 private Vector<MemberInfo> membertable;// the member info of the group
	 private boolean isMaster;              // if this server is master
	 @SuppressWarnings("rawtypes")
	 private HashMap puttaskinfo;           // to recode that all the "put" task has been done
	 private int seq;
	 
	 @SuppressWarnings("rawtypes")
	public MsgServer(int groupindex ,int hostindex) throws IOException{
		 groupIndex = groupindex;
		 hostIndex = hostindex;
		 seq = 0;
		 this.hostname = "ServerGroup_"+groupindex+"_Host_"+hostindex;
		 
		 // 加入group
		 groupSocket = new MulticastSocket(Integer.valueOf(Configure.getInstance().getValue("ServerPort"+groupindex)));
		 //groupSocket.setLoopbackMode(true);
		 InetAddress group = InetAddress.getByName(Configure.getInstance().getValue("ServerGroup"+groupindex));
		 groupSocket.joinGroup(group);
		 
		 
		 // check if it's a master
		 if (Configure.getInstance().getValue("ServerGroup_"+groupindex+"_DefaultMaster").equals(hostname)){
			 //new MasterMsgServer(Integer.valueOf(
			 //		 Configure.getInstance().getValue("MasterPort"+hostindex)));
			 System.out.println(hostname + " is master!\n\n");
			 tokenIn = true;
			 puttaskinfo = new HashMap();
			 isMaster = true;
		 }

		 
		 // 检测member
		 membertable = new Vector<MemberInfo>();//
		 new MainServer().start();
		 Timer timer1 = new Timer();
	     timer1.schedule(new Detector(), 0, 3000);
		 Timer timer2 = new Timer();
	     timer2.schedule(new SendHeartBeat(), 0, 1000);
	     
	     System.out.println(hostname + " started OK!\n");
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
		 @SuppressWarnings("unchecked")
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
						synchronized (membertable) {
							for (int i = 0; i < membertable.size(); i++) {
								if (membertable.get(i).getName().equals(name)) {
									alreadyIsMember = true;
									if (membertable.get(i).getStatus() == MemberInfo.RESTORING) {
										break;
									} else if (membertable.get(i).getStatus() == MemberInfo.DYING) {
										membertable.get(i).setStatus(
												MemberInfo.ACTIVE);
										break;
									} else if (membertable.get(i).getStatus() == MemberInfo.DIED) {
										if (isMaster) {
											new Restore(membertable.get(i)
													.getName()).start();
										}
										membertable.get(i).setStatus(
												MemberInfo.RESTORING);
									}
								}
							}

							if (!alreadyIsMember) {
								MemberInfo memberInfo = new MemberInfo();
								memberInfo.setName(name);
								memberInfo.setStatus(MemberInfo.ACTIVE);
								membertable.add(memberInfo);
							}
						}
					}else if (msg.getType() == OperateMessage.RESTORE_OK) {
						String name = (String) msg.getMsg().getData().get("name"); 
						synchronized (membertable) {
							for (int i = 0; i < membertable.size(); i++) {
								if (membertable.get(i).getName().equals(name)) {
									membertable.get(i).setStatus(
											MemberInfo.ACTIVE);
									break;
								}
							}
						}
					}else if (msg.getType() == OperateMessage.RESTORE_DATA) {
						if (msg.getMsg().getData().get("name").equals(hostname)){
							ArrayList<Message> data = (ArrayList<Message>) msg
									.getMsg().getData().get("data");
							for (int i = 0; i < data.size(); i++) {
								KVSServer.kvs.Put((String) data.get(i)
										.getData().get("key"), (byte[]) data
										.get(i).getData().get("value"));
							}
							System.out
									.println("\n*************\n Restore ok \n**************\n");
							Message message = new Message();
							message.setValue("name", hostname);
							groupSocket.send(PacketHandle.getDatagram(
									new OperateMessage(
											OperateMessage.RESTORE_OK, message,
											0), groupIndex));
						}
					}else if (msg.getType() == OperateMessage.TOKEN_PASS) {
						if (msg.getMsg().getData().get("next").equals(hostname)){
							tokenIn = true;
							System.out.println(hostname + " get the token!\n");
						}
					}else if (msg.getType() == OperateMessage.PUT_OK) {
						if (isMaster){
							int time = 0;
							if (puttaskinfo.get(msg.getSeq())!= null){
								time = (Integer)puttaskinfo.get(msg.getSeq());
							}
							time += 1;
							if (time == getLiveMemberNum()){
								msg.setType(OperateMessage.REPLY);
								groupSocket.send(PacketHandle.getDatagram(msg,groupIndex));
								puttaskinfo.remove(msg.getSeq());
							}else {
								puttaskinfo.put(msg.getSeq(), time);
							}
						}
					}
				}catch (Exception e) {
					e.printStackTrace();
				}
			 }
		 }
	 }
	 
	 // get the number of active member
	 private int getLiveMemberNum(){
		 int num = 0;
		 synchronized (membertable) {
			for (int i = 0; i < membertable.size(); i++) {
				if (membertable.get(i).getStatus() == MemberInfo.ACTIVE) {
					num++;
				}
			}
		 }
		 return num;
	 }
	 
	 
	 // a restore task , used only by the master
	 class Restore extends Thread{
		 private String name;
		 public Restore(String name){
			 this.name = name;
		 }

		 public void run(){
			 Message message = new Message();
			 message.setValue("data", KVSServer.kvs.GetAll());
			 message.setValue("name", name);
			 try {
				groupSocket.send(PacketHandle.getDatagram(
						 new OperateMessage(OperateMessage.RESTORE_DATA, message, 0),
						 groupIndex));
			} catch (IOException e) {
				e.printStackTrace();
			}
		 }
	 }
	 
	 
	 //  处理一个请求
	 class RequestHandle extends Thread{
		private OperateMessage msg;

		public RequestHandle(OperateMessage msg) {
			this.msg = msg;
		}

		@SuppressWarnings("unchecked")
		public void run() {
			while (seq != msg.getSeq()) {
				try {
					sleep(100);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			if (msg.getMsg().getOperation() == Message.PUT) {
				@SuppressWarnings("rawtypes")
				HashMap data = msg.getMsg().getData();
				KVSServer.kvs.Put((String) data.get("key"),
						(byte[]) data.get("value"));
				msg.getMsg().setValue("status", "success");
				msg.setType(OperateMessage.PUT_OK);
				try {
					groupSocket.send(PacketHandle.getDatagram(msg,groupIndex));
					Addseq();
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				System.out.println(hostname+" : handle Put OK!\n");
			}
			if (msg.getMsg().getOperation() == Message.GET) {
				if (tokenIn){
					@SuppressWarnings("rawtypes")
					HashMap data = msg.getMsg().getData();
					byte[] value = KVSServer.kvs.Get((String) data.get("key"));
					// if success
					Message success = new Message();
					data.put("value", value);
					success.setData(data);
					msg.setType(OperateMessage.REPLY);
					
					msg.setMsg(success);
					try {
						groupSocket.send(PacketHandle.getDatagram(msg,groupIndex));
						tokenIn = false;
						Message tokenPassMessage = new Message();
						// Here need to get the next member
						tokenPassMessage.setValue("next", getNextAviable());
						groupSocket.send(PacketHandle.getDatagram(new OperateMessage(OperateMessage.TOKEN_PASS,
								tokenPassMessage, 0),groupIndex));
					} catch (IOException e) {
						e.printStackTrace();
					}
					Addseq();
					System.out.println(hostname+" : handle get OK!\n");
				}
			}
		}
	}
	 
	 
	 // get the next aviable member to pass the token 
	 public String getNextAviable() {
		 // TODO complete this function 
		 boolean ok = false;
		 int i = 0;
		 synchronized (membertable) {
			while (!ok) {
				if (membertable.get(i).getStatus() == MemberInfo.ACTIVE) {
					if (!membertable.get(i).getName().equals(hostname)) {
						ok = true;
						return membertable.get(i).getName();
					}
				}
				i++;
			}
			return membertable.get(0).getName();
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
			//System.out.println("\n\n--------------------------------------");
			synchronized (membertable) {
				for (int i = 0; i < membertable.size(); i++) {
					// System.out.println(membertable.get(i).getName()+"\t"+membertable.get(i).getStatus());
					if (membertable.get(i).getStatus() == MemberInfo.DYING) {
						membertable.get(i).setStatus(MemberInfo.DIED);
					} else if (membertable.get(i).getStatus() == MemberInfo.ACTIVE) {
						membertable.get(i).setStatus(MemberInfo.DYING);
					}
				}
			}
		}
	}
	
	private void Addseq(){
		synchronized (this) {
			seq = (seq+1)%200;	
		}
	}
}
