import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ClientThread extends Thread{
	
	Socket socket;
	private DataInputStream dis = null;
	private DataOutputStream dos = null;
	
	public ClientThread(Socket socket){
		this.socket = new Socket();
		try {
			dis = new DataInputStream(socket.getInputStream());
			dos = new DataOutputStream(socket.getOutputStream());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void run(){
		try{
		while(CloudServer.running){
			if(dis == null || dos == null){
				TransmissionServer.connectedClients.remove(this);
				break;
			}
			if(dis.available() > 0){
				int cmd = dis.readInt();
				System.out.println("Client " + Command.get(cmd));
				if(cmd  == Command.CLOSE.getCode()){
					close();
					break;
				}
				processCommand(cmd);
			}
		}
		TransmissionServer.connectedClients.remove(this);
		socket.close();
		}catch(IOException e){
			TransmissionServer.connectedClients.remove(this);
			e.printStackTrace();
		}
	}
	
	private void processCommand(int cmd) throws IOException{
		switch(Command.get(cmd)){
		case CLOSE:
			close();
			break;
			
		case GET_ROOT_FILE:
			sendCommand(Command.OBJECT_TRANSMISSION);
			ObjectOutputStream oos = new ObjectOutputStream(dos);
			oos.writeObject(CloudServer.rootFolder);
			oos.flush();
			break;
			
		default:
			sendCommand(Command.UNKNOWN);
			break;
		}
	}
	
	public void close() throws IOException{
		dis.close();
		dos.close();
		socket.close();
		TransmissionServer.removeClient(this);
	}
	
	public void sendCommand(Command cmd) throws IOException{
		dos.writeInt(cmd.getCode());
		dos.flush();
	}
	
	public void sendCommand(Command cmd, int [] args) throws IOException{
		dos.writeInt(cmd.getCode());
		for(int a : args){
			dos.writeInt(a);
		}
		dos.flush();
	}
}
