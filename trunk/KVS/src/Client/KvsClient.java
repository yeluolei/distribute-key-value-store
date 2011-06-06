package Client;


public class KvsClient {
	public static MsgClient msgClient;
	public static KvsLib kvsLib;
	
	public KvsClient(){
		msgClient = new MsgClient("localhost", 10086);
		kvsLib = new KvsLib();	
	}
	public static void main(){
		KvsClient client = new KvsClient();
		client.kvsLib.Put("john", "grammy".getBytes());
		byte[] value = client.kvsLib.Get("john");
		System.out.print("key: john   value: ");
		for(int i = 0; i < value.length; i++)
			System.out.print((char)value[i]);
		System.out.print("\n");
	}
}
