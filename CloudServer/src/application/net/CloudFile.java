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
	private FileType type;
	private CloudFile parent = null;
	
	public CloudFile(File f){
		System.out.println("Init CloudFile: " + f.getAbsolutePath());
		file = f;
		if(file.isDirectory()){
			type = FileType.DIRECTORY;
			File [] files = file.listFiles();
			if(files != null){
				dirContent = new ArrayList<CloudFile>();
				System.out.println(files.length);
				for(File fi : files){
					dirContent.add(new CloudFile(fi, this));
				}
			}
		}else{
			type = FileType.UNKNOWN;
		}
	}
	
	public CloudFile(File f, CloudFile parent){
		this.parent = parent;
		System.out.println("Init CloudFile: " + f.getAbsolutePath() + " parent: " + parent.get().getAbsolutePath());
		file = f;
		if(file.isDirectory()){
			type = FileType.DIRECTORY;
			File [] files = file.listFiles();
			if(files != null){
				dirContent = new ArrayList<CloudFile>();
				System.out.println(files.length);
				for(File fi : files){
					dirContent.add(new CloudFile(fi, this));
				}
			}
		}else{
			type = FileType.UNKNOWN;
		}
	}
	
	public CloudFile getParent(){
		return parent;
	}
	
	public FileType getType(){
		return type;
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
