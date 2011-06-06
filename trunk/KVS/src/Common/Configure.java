package Common;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Configure {
	private static Configure configure;
	private Properties properties ;
	private InputStream in;
	private Configure()
	{
		properties = new Properties();
		try {
			in = new BufferedInputStream(new FileInputStream("property"));
			properties.load(in);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public static Configure getInstance(){
		if (configure == null)
		{
			configure = new Configure();
		}
		return configure;
	}
	
	public String getValue(String key)
	{
		return properties.getProperty(key);
	}
	
}
