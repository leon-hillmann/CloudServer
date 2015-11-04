import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;

import javax.crypto.spec.SecretKeySpec;

public class ClientThread extends Thread{
	
	Socket socket;
	private DataInputStream dis = null;
	private DataOutputStream dos = null;
	private ObjectOutputStream oos = null;
	private PrivateKey priv_key;
	private PublicKey pub_key;
	
	public ClientThread(Socket socket){
		this.socket = socket;
		System.out.println("Client " + socket.getInetAddress().getHostName() + " attemps to connect");
		try {
			System.out.println("Generating client-specific keypair");
			KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
			generator.initialize(2048);
			KeyPair pair = generator.generateKeyPair();
			priv_key = pair.getPrivate();
			pub_key = pair.getPublic();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			priv_key = null;
			pub_key = null;
			e.printStackTrace();
		}
		
		try {
			dis = new DataInputStream(socket.getInputStream());
			dos = new DataOutputStream(socket.getOutputStream());
			oos = new ObjectOutputStream(dos);
		} catch (IOException e) {
			e.printStackTrace();
			oos = null;
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
				System.out.println(ipString() + "> " + Command.get(cmd));
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
	
	private String ipString(){
		try{
		return socket.getRemoteSocketAddress().toString().split("/")[1];
		}catch (Exception e){
			return "Client ";
		}
	}
	
	private void processCommand(int cmd) throws IOException{
		switch(Command.get(cmd)){
		case CLOSE:
			close();
			break;
			
		case GET_ROOT_FILE:
			sendCommand(Command.OBJECT_TRANSMISSION);
			oos.writeObject(CloudServer.rootFolder);
			oos.flush();
			break;
			
		case PUBLIC_KEY_REQUEST:
			System.out.println(ipString() + "> Transmitting key...");
			sendCommand(Command.OBJECT_TRANSMISSION);
			oos.writeObject(pub_key);
			oos.flush();
			System.out.println(ipString() + "> Key transmitted");
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
