package Common;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;

import Server.OperateMessage;


public class PacketHandle {
	public static DatagramPacket getDatagram(OperateMessage msg,int index){
		ByteArrayOutputStream byteStream = new ByteArrayOutputStream(5000);
		try {
			ObjectOutputStream os = new ObjectOutputStream(
					new BufferedOutputStream(byteStream));
			os.flush();
			os.writeObject(msg);
			os.flush();
			byte[] sendbuff = byteStream.toByteArray();
			DatagramPacket packet = new DatagramPacket(sendbuff,
					sendbuff.length,
					InetAddress.getByName(Configure.getInstance().getValue("ServerGroup"+index)),
					Integer.valueOf(Configure.getInstance().getValue("ServerPort"+index)));
			return packet;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static OperateMessage getMessage(DatagramPacket packet , byte[] recvBuf){
		int byteCount = packet.getLength();
		ByteArrayInputStream byteStream = new ByteArrayInputStream(recvBuf);
		try {
			ObjectInputStream is = new ObjectInputStream(
					new BufferedInputStream(byteStream));
			OperateMessage msg = (OperateMessage) is.readObject();
			is.close();
			return msg;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}
