package Serveractivition;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;


public class Send extends Thread{
	String ip;
	int port;
	String attr;
	
	public Send(String ip, int port, String attr){
		this.ip = ip;
		this.port = port;
		this.attr = attr;
	}
	

	public void run(){   
	    System.out.println("client start.......");   
	    MulticastSocket multicastSocket = null;
		try {
			multicastSocket = new MulticastSocket();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    InetAddress group = null;
		try {
			group = InetAddress.getByName(ip);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}   
		InetAddress inet = null;
		try {
			inet = InetAddress.getLocalHost();
		} catch (UnknownHostException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	    MultiMsg msg = new MultiMsg(inet.getHostAddress(),port,attr);
	    ByteArrayOutputStream byteStream = new ByteArrayOutputStream(500);
	    ObjectOutputStream os;
	    byte[] data = null;
		try {
			os = new ObjectOutputStream(new BufferedOutputStream(byteStream));
			os.writeObject(msg);
			os.flush();
			data = byteStream.toByteArray();
			os.close();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	    DatagramPacket packet = new DatagramPacket(data, data.length, group, port);   
	    while (true) {   
	        try {   
	            multicastSocket.send(packet);   
	            Thread.sleep(1000);   
	        } catch (IOException ex) {   
	        	ex.printStackTrace();
	            System.exit(1);   
	        } catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}   
	    }   
	}   
}
