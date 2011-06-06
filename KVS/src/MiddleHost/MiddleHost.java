package MiddleHost;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import javax.sound.sampled.Port;

import Common.Configure;

public class MiddleHost {
	public static void main(String[] args)
	{
		MiddleHost middleHost = new MiddleHost();
	}
	
	public MiddleHost() {
		try {
			ServerSocket serverSocket = new ServerSocket(
					Integer.valueOf(Configure.getInstance().getValue("MiddlePort")));
			while (true) {
				Socket clientSocket = serverSocket.accept();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
