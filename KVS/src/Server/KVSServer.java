package Server;


import java.io.File;
import java.io.IOException;


public class KVSServer {
	public static KVS kvs;
	public static MsgServer msgServer;
	
	
	public static void main(String[] args){
		if (args.length != 2 || !"-h".equals(args[0])) {
            System.err.println
                ("Usage: java "  +
                 " -h <envHome>");
            System.exit(2);
        }
		KVSServer server = new KVSServer(new File(args[1]));
	}
	
	public KVSServer(File home){
		
		kvs = new KVS(home);
		try {
			msgServer = new MsgServer(10086);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
