import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.*;
import java.util.Scanner;

public class ClientRunnable implements Runnable {
	private Socket c;
	private String serverName;
	private String clientName;
	private String clientIP;
	private SocketAddress sa;
	private InetSocketAddress ca;
	private InputStream c_in;
	private OutputStream c_out;
	private Scanner in;
	private PrintWriter out;
	
	public ClientRunnable(Socket c, String serverName) {
		this.serverName = serverName;
		this.c = c;
		this.sa = c.getRemoteSocketAddress();
		this.ca = (InetSocketAddress) this.sa;
	}
	
	public void run()  {
		try {
			clientIP = ca.getAddress().toString();
			System.out.println(this.clientIP + "[" + ca.getPort() + "] has joined.");
			
			c_in = c.getInputStream();
			c_out = c.getOutputStream();
			in = new Scanner(c_in);
			out = new PrintWriter(c_out);
			sendResponse("You have joined the " + serverName + "\r\n");

			while (in.hasNextLine()) {
				String line = in.nextLine();
				System.out.println(clientName + " said: " + line);
				String response = "";
				
				if (line.startsWith("bye")) {
					this.disconnect();
				} else {
					response = this.messageEvaluate(line);
					this.sendResponse(response);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void disconnect() throws IOException {
		this.sendResponse("bye");
		c.close();
		Server.removeUser(this);
		System.out.println(this.clientIP + "[" + ca.getPort() + "] has left.\r\n");
	}
	
	public boolean sendResponse(String message) {
		out.println(message);
		return out.checkError();
	}
	
	private String registerUser(String line) {
		String desiredUsername = line.split(" ")[1];
		if(Server.addUser(this, desiredUsername)) {
			this.clientName = desiredUsername;
			return "200 ok " + this.clientName + " successfuly registered!\r\n";
		}
		else {
			return "100 err " + desiredUsername + " already taken!\r\n";
		}
	}
	
	private String sendTo(String line) {
		if(!Server.usernameExists(this.clientName)) {
			return "100 err you're not registered!\r\n";
		}
		
		String[] splittedLine = line.split(" ");
		String receiverName = splittedLine[1];
		String message = "";
		for(int i=2; i<splittedLine.length; i++) {
			message += splittedLine[i] + " ";
		}
		message += "\r\n";
		Server.sendMessage(clientName, message, clientName);
		if(!Server.usernameExists(receiverName)) {
			return "100 err " + receiverName + " does not exists!\r\n";
		}
		else {
			if(Server.sendMessage(receiverName, message, clientName)) {
				Server.sendMessage(receiverName, "300 msg_from " + clientName + " " + message + "\r\n", Server.getServerName());
				return "200 ok message to " + receiverName + " sent successfully\r\n";
			}
			else {
				return "100 err server error\r\n";
			}
		}
	}
	
	private String sendAll(String line) {
		if(!Server.usernameExists(this.clientName)) {
			return "100 err you're not registered!\r\n";
		}
		
		String[] splittedLine = line.split(" ");
		String message = "";
		for(int i=1; i<splittedLine.length; i++) {
			message += splittedLine[i] + " ";
		}
		if(Server.sendMessageToAll(message, clientName)) { 
			return "200 ok message sent successfully\r\n";
		}
		else {
			return "100 err server error!\r\n";
		}
	}
	
	private String getUserList() {
		if(!Server.usernameExists(this.clientName)) {
			return "100 err you're not registered!\r\n";
		}
		return Server.clientsList();
	}
	
	private String messageEvaluate(String line) {
		if(line.startsWith("user")) {
			return this.registerUser(line);
		}
		else if(line.startsWith("send_to")) {
			return this.sendTo(line);
		}
		else if(line.startsWith("send_all")) {
			return this.sendAll(line);
		}
		else if(line.startsWith("list")) {
			return this.getUserList();
		}
		return null;
	}
	

}
