package MiddleHost;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;

import Common.Configure;
import Common.Message;
import Common.PacketHandle;
import Server.OperateMessage;

public class MiddleHost {
	private Vector<MulticastSocket>groups = null;
//	private Vector<MasterClient> toMasters = null;
	private int groupnum = 0;
	private static ServerSocket serverSocket;
	public static void main(String[] args)
	{
		MiddleHost middleHost = new MiddleHost();
	}
	
	public MiddleHost() {
		try {
			serverSocket = new ServerSocket(
					Integer.valueOf(Configure.getInstance().getValue("MiddlePort")));
			serverSocket.setReuseAddress(true);
			groupnum = Integer.valueOf(Configure.getInstance().getValue("ServerGroupNum"));
			groups = new Vector<MulticastSocket>();
//			toMasters = new Vector<MasterClient>();
			for (int index = 0 ; index < groupnum ; index++)
			{
/*				MasterClient master = new MasterClient(
						Configure.getInstance().getValue("Master"+index),
						Integer.valueOf(Configure.getInstance().getValue("MasterPort"+index)));
				toMasters.add(master);*/
				 MulticastSocket multicastSocket = new MulticastSocket(
                          Integer.valueOf(Configure.getInstance().getValue("ServerPort"+index)));
				 InetAddress group = InetAddress.getByName(
                          Configure.getInstance().getValue("ServerGroup"+index));
          
				 multicastSocket.joinGroup(group);
				 groups.add(multicastSocket);
			}
			
			System.out.println("Middle Host Started OK!\n");
			
			while (true) {
				Socket clientSocket = serverSocket.accept();
				HostConnect hostConnect = new HostConnect(clientSocket);
				hostConnect.start();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	class HostConnect extends Thread{
		private Socket socket;
		private ObjectInputStream fromclient;
		private ObjectOutputStream toclient; 
		public HostConnect(Socket socket)
		{
			this.socket = socket;
			try {
				fromclient = new ObjectInputStream(socket.getInputStream());
				toclient = new ObjectOutputStream(socket.getOutputStream());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		@Override
		public void run() {
			try {
				while (true) {
					// ��������
					Message msg = (Message) fromclient.readObject();
					System.out.println("Middle :" + msg.getOperation() +" , " + msg.getData());
					// ���HASHֵ
					int groupIndex = msg.getData().get("key").toString()
							.hashCode()
							% groupnum;
					int seq = OperateMessage.getNextSeq();
					OperateMessage message = new OperateMessage(OperateMessage.TODO,
							msg,seq);
					DatagramPacket packet = PacketHandle.getDatagram(message,groupIndex);
					if (packet != null) {
						// ���͵�ָ����-master
/*						toMasters.get(serverIndex).SetHostClient(socket);
						toMasters.get(serverIndex).SendMsg(message);*/
						
						groups.get(groupIndex).send(packet);
					}

					// ���ջ�Ӧ
					boolean replied = false;
					while (!replied) {
						byte[] recvBuf = new byte[5000];
						packet = new DatagramPacket(recvBuf, recvBuf.length);
						groups.get(groupIndex).receive(packet);
						OperateMessage recvmsg = PacketHandle.getMessage(packet,
								recvBuf);
						// �ж��Ƿ��Ѿ�
						// д�ؽ��
						if (recvmsg.getType() == OperateMessage.REPLY &&
								recvmsg.getSeq() == seq){
							toclient.writeObject(recvmsg.getMsg());
							toclient.flush();
							toclient.reset();
							replied = true;
						}
					}
				}
			} catch (IOException e) {
				System.out.println("connecting closed !\n");
				this.stop();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
				this.stop();
			}
		}
	}
	
	
}
