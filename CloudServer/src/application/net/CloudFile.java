package application.net;
import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CloudFile implements Serializable{
	private static final long serialVersionUID = 2117757004337517974L;
	private File file;
	List<CloudFile> dirContent = null;

	public CloudFile(File f){
		System.out.println("Init CloudFile: " + f.getAbsolutePath());
		file = f;
		if(file.isDirectory()){
			File [] files = file.listFiles();
			if(files != null){
				dirContent = new ArrayList<CloudFile>();
				System.out.println(files.length);
				for(File fi : files){
					dirContent.add(new CloudFile(fi));
				}
			}
		}
	}

	public List<CloudFile> getDirectoryContent(){
		return dirContent;
	}

	public File get(){
		return file;
	}
	
	public void printAllSubFiles(){
		if(dirContent == null)
			return;
		
		for(CloudFile f : dirContent){
			System.out.println(f.get().getAbsolutePath());
			f.printAllSubFiles();
		}
	}
}
