package Server;

import java.io.File;


import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.StoreConfig;


public class KVS {	
	
	private Environment env;
    private EntityStore store;
    private KVEntityAccessor dao;
    
    
    public static void main(String[] args){
    	if (args.length != 2 || !"-h".equals(args[0])) {
            System.err.println
                ("Usage: java "  +
                 " -h <envHome>");
            System.exit(2);
        }
        KVS example = new KVS(new File(args[1]));
        example.run();
        example.close();
    }
    
    
    public KVS(File envPath)throws DatabaseException{
    	EnvironmentConfig envConfig = new EnvironmentConfig();
        envConfig.setAllowCreate(true);
        envConfig.setTransactional(true);
        env = new Environment(envPath, envConfig);

        /* Open a transactional entity store. */
        StoreConfig storeConfig = new StoreConfig();
        storeConfig.setAllowCreate(true);
        storeConfig.setTransactional(true);
        store = new EntityStore(env, "KVStore", storeConfig);
        
        dao = new KVEntityAccessor(store);
    }
    public void run(){
    	String key = "key";
    	byte[] value = "value".getBytes();
    	dao.kvEntityByKey.put(new KVEntity(key, value));
    	KVEntity entity1 = dao.kvEntityByKey.get(key);
    	entity1.Print();
    }
    public void Put(String key, byte[] value){
    	dao.kvEntityByKey.put(new KVEntity(key, value));
    }
    public byte[] Get(String key){
    	KVEntity entity = dao.kvEntityByKey.get(key);
    	//entity.Print();
    	return entity.getValue();
    }
    public void close()throws DatabaseException {
    	store.close();
        env.close();
    }
}
