import java.util.Scanner;

public class CommandThread extends Thread{

	Scanner scan;
	public CommandThread(){
		scan = new Scanner(System.in);
	}

	@Override
	public void run(){
		while(CloudServer.running){
			String cmd = scan.next();
			processCommand(cmd);
		}
	}

	private void processCommand(String command){
		switch(command.toLowerCase()){

		case "stop":
		case "shutdown":
			CloudServer.running = false;
			CloudServer.server.interrupt();
			break;

		default:
			System.out.println("Unknown Command");
			break;
		}
	}

}
