package Server;


import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.PrimaryIndex;


public class KVEntityAccessor {
	PrimaryIndex<String ,KVEntity> kvEntityByKey;
	
	public KVEntityAccessor(EntityStore store){
		kvEntityByKey = store.getPrimaryIndex(String.class, KVEntity.class);
		
	}
}
