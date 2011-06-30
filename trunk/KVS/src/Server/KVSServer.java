package Server;


import java.io.File;
import java.io.IOException;

import Server.store.KVS;


public class KVSServer {
	public static KVS kvs;
	public static ServerHost msgServer;
	
	
	public static void main(String[] args){
		if (args.length != 4) {
            System.err.println
                ("Usage: java "  +
                 "-i <groupindex> -h <hostindex>");
            System.exit(2);
        }
		KVSServer server = new KVSServer(Integer.valueOf(args[1]),Integer.valueOf(args[3]));
	}
	
	public KVSServer(int groupindex , int hostindex){
		String file = "ServerGroup"+groupindex+"/"+"Host"+hostindex;
		kvs = new KVS(new File(file));
		try {
			msgServer = new ServerHost(groupindex,hostindex);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
