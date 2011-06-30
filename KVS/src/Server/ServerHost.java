package Server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import Server.model.MemberInfo;

import Common.Configure;
import Common.Message;
import Common.PacketHandle;

public class ServerHost {
	private int groupIndex; // witch group this server is belong to
	private int hostIndex; // the index in the group
	private String hostname; // the name of this index

	private MulticastSocket groupSocket = null;
	private Socket taskListenSocket = null; // the socket to connect the middle
											// host
	private ObjectInputStream fromMiddle = null;
	private ObjectOutputStream toMiddle = null;

	private Vector<MemberInfo> membertable;// the member info of the group
	@SuppressWarnings("rawtypes")
	private HashMap putTasks;

	public ServerHost(int groupindex, int hostindex) throws IOException {
		groupIndex = groupindex;
		hostIndex = hostindex;
		this.hostname = "ServerGroup_" + groupindex + "_Host_" + hostindex;

		JoinGroup();
		new TaskListenConnect().start();
		ConnectMiddle();
		System.out.println(hostname + " started!\n");
	}

	// Connect Middle host
	public void ConnectMiddle() {
		InetAddress address;
		try {
			address = InetAddress.getByName(Configure.getInstance().getValue(
					"MiddleHost"));
			Socket socket = new Socket(address, Integer.valueOf(Configure
					.getInstance().getValue("MiddleServerPort")));

			ObjectOutputStream oos = new ObjectOutputStream(
					socket.getOutputStream());
			Message message = new Message();
			message.setValue("hostaddr",
					Configure.getInstance().getValue(hostname + "_Addr"));
			message.setValue("hostport",
					Configure.getInstance().getValue(hostname + "_Port"));
			message.setValue("groupindex", groupIndex);
			message.setValue("hostindex", hostIndex);
			OperateMessage operateMessage = new OperateMessage(
					OperateMessage.NEW_SERVER_IN, message, 0);
			oos.writeObject(operateMessage);
			oos.flush();
			oos.close();
			socket.close();
			System.out.println("Send info to middle host ok!\n");
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (NumberFormatException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("rawtypes")
	public void JoinGroup() {
		try {
			groupSocket = new MulticastSocket(Integer.valueOf(Configure
					.getInstance().getValue("ServerPort" + groupIndex)));
			InetAddress group = InetAddress.getByName(Configure.getInstance()
					.getValue("ServerGroup" + groupIndex));
			groupSocket.joinGroup(group);

			membertable = new Vector<MemberInfo>();//
			putTasks = new HashMap();
			Timer timer1 = new Timer();
			timer1.schedule(new Detector(), 3000);
			Timer timer2 = new Timer();
			timer2.schedule(new SendHeartBeat(), 1000);
			new GroupTaskListener().start();

			System.out.println(hostname + " joined group OK!\n");
		} catch (NumberFormatException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	class GroupTaskListener extends Thread {
		public GroupTaskListener() {
		}

		@SuppressWarnings("unchecked")
		public void run() {
			while (true) {
				byte[] recvBuf = new byte[5000];
				DatagramPacket packet = new DatagramPacket(recvBuf,
						recvBuf.length);
				try {
					groupSocket.receive(packet);
					OperateMessage msg = PacketHandle.getMessage(packet,
							recvBuf);

					if (msg.getType() == OperateMessage.TODO) {
						new PutHandle(msg).start();
					} else if (msg.getType() == OperateMessage.HEART_BEAT) {
						new HeartBeatHandle(msg).start();
					} else if (msg.getType() == OperateMessage.RESTORE_OK) {
						RestoreOK(msg);
					} else if (msg.getType() == OperateMessage.RESTORE_DATA) {
						if (msg.getMsg().getData().get("name").equals(hostname)) {
							RestoreData(msg);
						}
					} else if (msg.getType() == OperateMessage.PUT_OK) {
						ReceivePutOK(msg);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}

			}
		}

		public void RestoreOK(OperateMessage msg) {
			String name = (String) msg.getMsg().getData().get("name");
			synchronized (membertable) {
				for (int i = 0; i < membertable.size(); i++) {
					if (membertable.get(i).getName().equals(name)) {
						membertable.get(i).setStatus(MemberInfo.ACTIVE);
						break;
					}
				}
			}
		}
		
		public void RestoreData(OperateMessage msg) {
			ArrayList<Message> data = (ArrayList<Message>) msg.getMsg()
					.getData().get("data");
			for (int i = 0; i < data.size(); i++) {
				KVSServer.kvs.Put((String) data.get(i).getData().get("key"),
						(byte[]) data.get(i).getData().get("value"));
			}
			System.out
					.println("\n*************\n Restore ok \n**************\n");
			Message message = new Message();
			message.setValue("name", hostname);
			try {
				groupSocket.send(PacketHandle.getDatagram(new OperateMessage(
						OperateMessage.RESTORE_OK, message, 0), groupIndex));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		public void ReceivePutOK(OperateMessage msg){
			int time = 0;
			if (putTasks.get(msg.getSeq()) != null) {
				time = (Integer) putTasks.get(msg.getSeq());
				time += 1;

				if (time == getLiveMemberNum()) {
					msg.setType(OperateMessage.REPLY);
					msg.getMsg().setValue("status", "success");
					synchronized (toMiddle) {
						try {
							toMiddle.writeObject(msg);
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
					putTasks.remove(msg.getSeq());
				} else {
					putTasks.put(msg.getSeq(), time);
				}
			}
		}
	}

	/*
	 * wait for the middle host connect this host if connect success , this host
	 * send
	 */

	class TaskListenConnect extends Thread {
		public TaskListenConnect() {
			try {
				ServerSocket serverSocket = new ServerSocket(
						Integer.valueOf(Configure.getInstance().getValue(
								hostname + "_Port")));
				serverSocket.setReuseAddress(true);
				while (true) {
					taskListenSocket = serverSocket.accept();
					System.out.println(hostname
							+ " has connect to middle host\n");
					break;
				}
				fromMiddle = new ObjectInputStream(
						taskListenSocket.getInputStream());
				toMiddle = new ObjectOutputStream(taskListenSocket.getOutputStream());
				OperateMessage operateMessage;
				while (true) {
					operateMessage = (OperateMessage) fromMiddle.readObject();
					new RequestHandle(operateMessage).start();
				}
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	class HeartBeatHandle extends Thread {
		OperateMessage msg;

		public HeartBeatHandle(OperateMessage msg) {
			this.msg = msg;
		}

		public void run() {
			String name = (String) msg.getMsg().getData().get("name");
			boolean alreadyIsMember = false;
			synchronized (membertable) {
				for (int i = 0; i < membertable.size(); i++) {
					if (membertable.get(i).getName().equals(name)) {
						alreadyIsMember = true;
						if (membertable.get(i).getStatus() == MemberInfo.RESTORING) {
							break;
						} else if (membertable.get(i).getStatus() == MemberInfo.DYING) {
							membertable.get(i).setStatus(MemberInfo.ACTIVE);
							break;
						} else if (membertable.get(i).getStatus() == MemberInfo.DIED) {
							membertable.get(i).setStatus(MemberInfo.RESTORING);
							
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
		}
	}

	class PutHandle extends Thread {
		private OperateMessage msg;

		public PutHandle(OperateMessage msg) {
			this.msg = msg;
		}

		@SuppressWarnings("rawtypes")
		public void run() {
			HashMap data = msg.getMsg().getData();
			KVSServer.kvs.Put((String) data.get("key"),
					(byte[]) data.get("value"));
			msg.setType(OperateMessage.PUT_OK);
			try {
				groupSocket.send(PacketHandle.getDatagram(msg, groupIndex));
				System.out.println(hostname + " : handle Put OK!\n");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/*
	 * handle for one message
	 */
	class RequestHandle extends Thread {
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

				msg.setType(OperateMessage.TODO);
				putTasks.put(msg.getSeq(), 1);
				try {
					groupSocket.send(PacketHandle.getDatagram(msg, groupIndex));
				} catch (IOException e) {
					e.printStackTrace();
				}

				System.out.println(hostname + " : Master handle Put OK!\n");
			}
			if (msg.getMsg().getOperation() == Message.GET) {
				@SuppressWarnings("rawtypes")
				HashMap data = msg.getMsg().getData();
				byte[] value = KVSServer.kvs.Get((String) data.get("key"));
				msg.setType(OperateMessage.REPLY);
				msg.getMsg().setValue("value", value);
				msg.getMsg().setValue("status", "success");
				try {
					synchronized (toMiddle) {
						toMiddle.writeObject(msg);
						toMiddle.flush();
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
				System.out.println(hostname + " : handle get OK!\n");
			}
		}
	}

	/*
	 * send a heart beat every 1 second
	 */
	class SendHeartBeat extends TimerTask {
		@Override
		public void run() {
			Message message = new Message();
			message.setValue("name", hostname);
			try {
				groupSocket.send(PacketHandle.getDatagram(new OperateMessage(
						OperateMessage.HEART_BEAT, message, 0), groupIndex));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/*
	 * check the member info every 3 second
	 */
	class Detector extends TimerTask {
		@Override
		public void run() {
			// System.out.println("\n\n--------------------------------------");
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

	/*
	 * get the number of active member
	 */
	private int getLiveMemberNum() {
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
	
	 /*
	  *  a restore task , used only by the master
	  */
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
}
