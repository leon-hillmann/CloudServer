package application.net;

public enum FileType {
	UNKNOWN(-1), DIRECTORY(0);
	
	private int type;
	FileType(int t){
		this.type = t;
	}
	
	public FileType get(int s){
		for(FileType t : FileType.values()){
			if(t.type == type)
				return t;
		}
		return UNKNOWN;
	}
	
}
