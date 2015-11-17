import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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
	private InputStream is;
	private OutputStream os;

	private Cipher cin = null, cout = null;

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
			sendObject(CloudServer.rootFolder);
			break;

		case PUBLIC_KEY_REQUEST:
			System.out.println("Applying key to connection...");
			try {
				Cipher c = Cipher.getInstance("RSA");
				c.init(Cipher.DECRYPT_MODE, priv_key);
				setInputCipher(c);
			} catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.out.println(ipString() + "> Transmitting key...");
			sendObject(pub_key);
			System.out.println(ipString() + "> Key transmitted");
			break;

		case AES_TRANSMISSION:
			System.out.println("Waiting for AESKey");
			aes_key = (SecretKey) getClientObject();
			try {
				Cipher cin = Cipher.getInstance("AES"); //Hier ist auch AES wichtig!
				Cipher cout = Cipher.getInstance("AES");
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
	
	/**
	 * liest ein Object vom Client!
	 * @return das Object (oder null wenn fehler)
	 * @throws IOException wenn was nicht klappt.
	 */
	
	private Object getClientObject() throws IOException{
		Command server_cmd = Command.get(dis.readInt());
		if(server_cmd == Command.OBJECT_TRANSMISSION){
			int length = dis.readInt();
			byte [] objectBytes = new byte[length];
			dis.read(objectBytes);
			return getBytesObject(objectBytes);
		}else if(server_cmd == Command.UNKNOWN){ //wenn das nicht Objecttransmission ist dann klappt das nicht...
			System.out.println("Command sent to server was invalid");
		}else{
			System.out.println("Unknown Command (no Object_transmission!)");
		}
		System.out.println("Returning null...");
		return null;
	}
	
	public void sendObject(Object o){
		try{
			byte [] objectBytes = getObjectBytes(o);
			sendCommand(Command.OBJECT_TRANSMISSION, new int[] {objectBytes.length});
			System.out.println("Sending " + objectBytes.length + " bytes long Object");
			dos.write(objectBytes);
			dos.flush();
		}catch(IOException e){
			e.printStackTrace();
		}
	}
	
	public void setOutputCipher(Cipher c){
		try {
			System.out.println("Create CipherOutputStream");
			CipherOutputStream cos = new CipherOutputStream(os = socket.getOutputStream(), c);
			System.out.println("Create DataOutputStream");
			dos = new DataOutputStream(cos);
			System.out.println("----------Done!");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void setInputCipher(Cipher c){
		try {
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
	
	/**
	 * gibt die bytes eines Objects zurück..
	 * @param o das object
	 * @return Object-Bytes
	 */
	private byte[] getObjectBytes(Object o){
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutputStream oos = null;
		try {
			oos = new ObjectOutputStream(bos);
			oos.writeObject(o);
			oos.flush();
			byte [] result = bos.toByteArray();
			oos.close();
			return result;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally{
			if(oos != null)
				try {
					oos.close();
				} catch (IOException e) {}
		}
		return null;
		
	}


	/**
	 * gibt ein Object aus einem byte[] zurück und entschlüsselt diese mit dem Cipher c.
	 * @param bytes die bytes
	 * @param c der cipher
	 * @return das Object
	 */
	
	private Object getBytesObject(byte[] bytes){
		try {
			ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
			ObjectInputStream ois = null;
			ois = new ObjectInputStream(bis);
			Object o = ois.readObject();
			ois.close();
			return o;
		} catch (IOException | ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	public void close() throws IOException{
		dis.close();
		dos.close();
		socket.close();
		TransmissionServer.removeClient(this);
	}

	public void sendCommand(Command cmd) throws IOException{
		sendCommand(cmd, null);
	}

	public void sendCommand(Command cmd, int [] args) throws IOException{
		dos.writeInt(cmd.getCode());
		if(args != null){
			for(int a : args){
				dos.writeInt(a);
			}
		}
		dos.writeInt(Integer.MIN_VALUE);
		dos.flush();
	}
}
