package Serveractivition;

import java.io.IOException;
import java.net.UnknownHostException;

public class Member {

	   
		/**
		 * @param args
		 * @throws IOException 
		 * @throws UnknownHostException 
		 * @throws NumberFormatException 
		 * @throws InterruptedException 
		 */
		public static void main(String[] args) throws NumberFormatException, UnknownHostException, IOException, InterruptedException {
			// TODO Auto-generated method stub
			Listen listen = new Listen(args[0],Integer.parseInt(args[1]));
			Send send = new Send(args[0],Integer.parseInt(args[1]),args[2]);
			listen.start();
			send.start();
			while(true){
				Thread.sleep(1000);
				System.out.println(listen.GetMsg().get(0).Getattr());
			}
		}
	   
}
