
public enum Command {
	UNKNOWN(-1), CLOSE(0), GET_ROOT_FILE(1), OBJECT_TRANSMISSION(2), PUBLIC_KEY_REQUEST(3);
	
	private int code;
	Command(int cmd){
		this.code = cmd;
	}
	
	public int getCode(){
		return code;
	}
	
	public static Command get(int cmd){
		for(Command command : Command.values()){
			if(command.getCode() == cmd)
				return command;
		}
		return null;
	}
}
