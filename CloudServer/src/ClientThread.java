import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

public class ClientThread extends Thread{
	
	Socket socket;
	private DataInputStream dis = null;
	private DataOutputStream dos = null;
	private ObjectOutputStream oos = null;
	private ObjectInputStream ois = null;
	private InputStream is;
	private OutputStream os;
	private PrivateKey priv_key;
	private PublicKey pub_key;
	private SecretKey aes_key;
	
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
			dis = new DataInputStream(is = socket.getInputStream());
			dos = new DataOutputStream(os = socket.getOutputStream());
			oos = new ObjectOutputStream(os);
			ois = new ObjectInputStream(is);
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
			System.out.println("Applying key to connection...");
			try {
				Cipher c = Cipher.getInstance("RSA");
				c.init(Cipher.DECRYPT_MODE, priv_key);
				setInputCipher(c);
			} catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			sendCommand(Command.OK);
			break;
			
		case AES_TRANSMISSION:
			if(Command.get(dis.readInt()) != Command.OBJECT_TRANSMISSION){
				System.out.println("Client does not transmit the key");
				break;
			}
			try {
				System.out.println("Waiting for AESKey");
				ois = new ObjectInputStream(dis);
				aes_key = (SecretKey) ois.readObject();
				sendCommand(Command.OK);
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				System.out.println("Error while key transmission");
				break;
			}
			try {
				Cipher cin = Cipher.getInstance("AES/CTR/NoPadding");
				Cipher cout = Cipher.getInstance("AES/CTR/NoPadding");
				cin.init(Cipher.DECRYPT_MODE, aes_key);
				cout.init(Cipher.ENCRYPT_MODE, aes_key);
				setInputCipher(cin);
				setOutputCipher(cout);
			} catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			break;
			
		default:
			sendCommand(Command.UNKNOWN);
			break;
		}
	}
	public void setOutputCipher(Cipher c){
		try {
			dos = null;
			oos = null;
			System.out.println("Create CipherOutputStream");
			CipherOutputStream cos = new CipherOutputStream(os = socket.getOutputStream(), c);
			System.out.println("Create DataOutputStream");
			dos = new DataOutputStream(cos);
			System.out.println("Create ObjectOutputStream");
			oos = new ObjectOutputStream(dos);
			System.out.println("----------Done!");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void setInputCipher(Cipher c){
		try {
			dis = null;
			ois = null;
			System.out.println("Create CipherInputStream");
			CipherInputStream cis = new CipherInputStream(is = socket.getInputStream(), c);
			System.out.println("Create DataInputStream");
			dis = new DataInputStream(cis);
			System.out.println("----------Done!");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void close() throws IOException{
		dis.close();
		dos.close();
		oos.close();
		ois.close();
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
