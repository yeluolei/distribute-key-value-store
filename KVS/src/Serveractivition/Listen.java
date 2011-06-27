package Serveractivition;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Timer;
import java.util.Vector;


public class Listen extends Thread{
	
	String ip;
	int port;
	static Map<MultiMsg, Boolean> member ;
	
	public Listen(String ip, int port){
		this.ip = ip;
		this.port = port;
		Listen.member = new HashMap<MultiMsg, Boolean>();
	}
	
	public Vector<MultiMsg> GetMsg(){                // get all of the meassage in the table
		Vector<MultiMsg> result = new Vector<MultiMsg>();
    	Iterator iter = member.entrySet().iterator();
    	while (iter.hasNext()) {
    	    Map.Entry entry = (Map.Entry) iter.next();
    	    MultiMsg key = (MultiMsg)entry.getKey();
    	    result.add(key);
    	}
		return result;
	}
	
	public void run(){   
		System.out.println("server start.......");   
	    MulticastSocket multicastSocket = null;
		try {
			multicastSocket = new MulticastSocket(port);
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
		try {
			multicastSocket.joinGroup(group);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}   
	    byte[] data = new byte[500];    
	    DatagramPacket packet = new DatagramPacket(data, data.length);   
        Timer timer = new Timer();
        timer.schedule(new Detector(), 0, 3000);
	    while (true) {   
	        try {   
	            multicastSocket.receive(packet);  
	            ByteArrayInputStream byteStream = new ByteArrayInputStream(data);
	            ObjectInputStream os = new ObjectInputStream(byteStream);
	            MultiMsg msg = null;
				try {
					msg = (MultiMsg)os.readObject();
				} catch (ClassNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	            member.put(msg,true); 
	        } catch (IOException ex) {     
	            System.exit(1);   
	        }   
	    }   
	}
	
	private static class Detector extends java.util.TimerTask{
		
        @Override
        public void run() {
            // TODO Auto-generated method stub
        	Iterator iter = member.entrySet().iterator();
        	while (iter.hasNext()) {
        	    Map.Entry entry = (Map.Entry) iter.next();
        	    MultiMsg key = (MultiMsg)entry.getKey();
        	    Boolean val = (Boolean)entry.getValue();
        	    if(val == false){
        	    	iter.remove();
        	    	continue;
        	    }
        	    member.put(key, false);
        	}
        }
	}
}
