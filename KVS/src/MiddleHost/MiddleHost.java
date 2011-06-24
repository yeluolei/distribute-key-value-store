package MiddleHost;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectInputStream.GetField;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Vector;

import javax.sound.sampled.Port;

import Common.Configure;
import Common.Message;
import Common.PacketHandle;

public class MiddleHost {
	private Vector<MulticastSocket>servers = null;
	private int servernum = 0;
	public static void main(String[] args)
	{
		MiddleHost middleHost = new MiddleHost();
	}
	
	public MiddleHost() {
		try {
			ServerSocket serverSocket = new ServerSocket(
					Integer.valueOf(Configure.getInstance().getValue("MiddlePort")));
			serverSocket.setReuseAddress(true);
			servernum = Integer.valueOf(Configure.getInstance().getValue("HostNum"));
			servers = new Vector<MulticastSocket>();
			for (int index = 1 ; index <= servernum ; index++)
			{
				MulticastSocket multicastSocket = new MulticastSocket(
						Integer.valueOf(Configure.getInstance().getValue("ServerPort"+index)));
				//multicastSocket.setLoopbackMode(true);
				InetAddress group = InetAddress.getByName(
						Configure.getInstance().getValue("ServerGroup"+index));
				
				multicastSocket.joinGroup(group);
				servers.add(multicastSocket);
			}
			
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
					// 接收请求
					Message msg = (Message) fromclient.readObject();
					System.out.println("Middle :" + msg.getOperation() +" , " + msg.getData());
					// 求出HASH值
					int serverIndex = msg.getData().get("key").toString()
							.hashCode()
							% servernum;
					DatagramPacket packet = PacketHandle.getDatagram(msg,serverIndex);
					if (packet != null) {
						// 发送到指定组
						servers.get(serverIndex).send(packet);
					}

					// 接收回应
					boolean replied = false;
					while (!replied) {
						byte[] recvBuf = new byte[5000];
						packet = new DatagramPacket(recvBuf, recvBuf.length);
						servers.get(serverIndex).receive(packet);
						Message recvmsg = PacketHandle.getMessage(packet,
								recvBuf);
						System.out.println("Middle :" + recvmsg.getOperation() +" , " + recvmsg.getData());
						// 判断是否已经
						// 写回结果
						toclient.writeObject(msg);
						toclient.flush();
						toclient.reset();
						replied = true;
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
	}
	
	
}
