package MiddleHost;

import java.io.IOException;

import Common.Configure;

public class Frontend {
	public static MiddleHost middleHost;
	public static NewNodeServer newNodeServer;
	
	
	public static void main(String[] args)
	{
		Frontend frontend = new Frontend();
	}
	public Frontend(){
		int newNodePort = Integer.valueOf(Configure.getInstance().getValue("MiddleServerPort"));;
		int middlePort = Integer.valueOf(Configure.getInstance().getValue("MiddlePort"));
		try {
			middleHost = new MiddleHost(middlePort);
			newNodeServer = new NewNodeServer(newNodePort);
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
}
