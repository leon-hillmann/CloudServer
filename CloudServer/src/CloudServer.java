import java.io.File;
import java.io.IOException;
import java.util.Properties;

import application.net.CloudFile;

public class CloudServer {

	public static boolean running = true;

	public static final String default_folder = "/media/cloud";
	public static String root_path;
	public static CloudFile rootFolder;
	public static TransmissionServer server;
	public static int port = 1337;
	public static final String config_path = "config.properties";
	
	public CloudServer(){
		rootFolder = new CloudFile(new File(root_path));
		System.out.println("====================Init Completed====================");
		
		ConfigReader reader = new ConfigReader(config_path);
		Properties ps;
		if((ps = reader.getConfigProperties()) != null){
			port = Integer.parseInt(ps.getProperty("port"));
			System.out.println("Loaded config file");
		}
		
		new CommandThread().start();
		try {
			server = new TransmissionServer(port);
			server.start();
		} catch (IOException e) {
			e.printStackTrace();
			running = false;
			System.exit(0);
		}
	}
	
	public static void main(String args[]){
		if(args.length > 0)
			root_path = args[0];
		else
			root_path = default_folder;

		new CloudServer();
	}

}