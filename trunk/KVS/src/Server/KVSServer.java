package Server;


import java.io.File;
import java.io.IOException;

import Common.Configure;


public class KVSServer {
	public static KVS kvs;
	public static MsgServer msgServer;
	
	
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
		kvs = new KVS(new File("ServerGroup"+groupindex+"/"+"Host"+hostindex));
		try {
			msgServer = new MsgServer(groupindex,hostindex);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
