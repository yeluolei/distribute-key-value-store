package Server;


import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;

@Entity
public class KVEntity {
	@PrimaryKey 
    String key;
	
	byte[] value;
	public KVEntity(String key, byte[] value){
		this.key = key;
		this.value = value;
	}
	public KVEntity() {}
	public void Print(){
		System.out.print("key: " + key + "    value: ");
		for(int i = 0; i < value.length; i++)
			System.out.print((char)value[i]);
		System.out.print("\n");
	}
	public String getKey() {
		return key;
	}
	public void setKey(String key) {
		this.key = key;
	}
	public byte[] getValue() {
		return value;
	}
	public void setValue(byte[] value) {
		this.value = value;
	}
	
}
