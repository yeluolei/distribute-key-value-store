package Server;


import java.io.File;
import java.io.IOException;

import Common.Configure;


public class KVSServer {
	public static KVS kvs;
	public static MsgServer msgServer;
	
	
	public static void main(String[] args){
		if (args.length != 4 || !"-h".equals(args[0])) {
            System.err.println
                ("Usage: java "  +
                 " -h <envHome> -i <index>");
            System.exit(2);
        }
		KVSServer server = new KVSServer(new File(args[1]),Integer.valueOf(args[3]));
	}
	
	public KVSServer(File home, int index){
		
		kvs = new KVS(home);
		try {
			msgServer = new MsgServer(index);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
