import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class ConfigReader {

	private String path;
	private File config;

	public ConfigReader(String path){
		this.path = path;
		config = new File(path);
	}

	public Properties getConfigProperties(){
		if(!config.exists())
			return null;
		try{
			Properties p = new Properties();
			BufferedInputStream is = new BufferedInputStream(new FileInputStream(config));
			p.load(is);
			is.close();
			return p;
		}catch(IOException e){
			e.printStackTrace();
		}
		return null;
	}

}
