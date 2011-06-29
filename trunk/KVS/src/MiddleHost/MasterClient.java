package MiddleHost;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;

import Server.OperateMessage;

import Common.Message;

public class MasterClient { // middlehost use it to send request to master
	Socket socket; // to master
	Listen listen;
	boolean is_msg = false;
	InputStream is;
	ObjectInputStream dis;
	OutputStream outputStream;
	ObjectOutputStream dos;

	Socket hostClient;

	public MasterClient(String serverName, int port) {
		try {
			InetAddress address = InetAddress.getByName(serverName);
			socket = new Socket(address, port);
			outputStream = socket.getOutputStream();
			dos = new ObjectOutputStream(outputStream);
			is = socket.getInputStream();
			dis = new ObjectInputStream(is);
		} catch (IOException e) {
			e.printStackTrace();
		}
		listen = new Listen();
		listen.start();
	}

	void SetHostClient(Socket client) {
		this.hostClient = client;
	}

	public void SendMsg(OperateMessage msg) {
		try {
			dos.writeObject(msg);
			dos.flush();
			dos.reset();
		} catch (Exception e) {
			System.out.println("send message failure");
			e.printStackTrace();
		}
	}

	public void SendToClient(OperateMessage msg) { // send to hostclient of this
													// request
		OutputStream outputStream;
		ObjectOutputStream dos;
		try {
			outputStream = hostClient.getOutputStream();
			dos = new ObjectOutputStream(outputStream);
			dos.writeObject(msg);
			dos.flush();
			dos.reset();
		} catch (IOException ex) {
		}
	}

	class Listen extends Thread {
		boolean knowSeq = false;

		public void Listen() {
			this.start();
		}

		public void run() {
			try {
				while (true) {
					// from master
					OperateMessage msg = (OperateMessage) dis.readObject();
					// if msg tells the failure of the master, do something

					// if msg is the reply of put/get, send it to guest-client
					SendToClient(msg);

				}
			} catch (Exception e) {
				System.out
						.println("uh-oh, server happens to break down\n quit");
				System.exit(-1);
				// e.printStackTrace();
			}
		}
	}
}
