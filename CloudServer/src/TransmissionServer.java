import java.io.IOException;
import java.net.ServerSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

public class TransmissionServer extends Thread{
	
	ServerSocket server;
	public static List<ClientThread> connectedClients;
	
	public TransmissionServer(int port) throws IOException{
		server = new ServerSocket(port);
		connectedClients = new ArrayList<ClientThread>();
	}
	
	@Override
	public void run(){
		try {
			while(CloudServer.running){
				ClientThread newClient = new ClientThread(server.accept());
				connectedClients.add(newClient);
				System.out.println("added client");
				newClient.start();
			}
			server.close();
		} catch (IOException e) {
			if(e instanceof SocketException){
				System.out.println(e.getMessage());
			}else
				e.printStackTrace();
		}
	}
	
	@Override
	public void interrupt(){
		try {
			server.close();
		} catch (IOException e) {
		}
		System.out.println("Server shutdown");
		super.interrupt();
	}
	
	public static void removeClient(ClientThread c){
		System.out.println("try removing");
		if(connectedClients == null || c == null)
			return;
		connectedClients.remove(c);
		System.out.println("Client disconnected");
	}	
}
