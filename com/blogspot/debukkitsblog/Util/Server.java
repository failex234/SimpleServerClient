package com.blogspot.debukkitsblog.Util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * A very simple Server class for Java network applications
 * @author Leonard Bienbeck
 * created on 09.03.2016 in Horstmar, NRW, Germany
 */
public abstract class Server {
	
	HashMap<String, Executable> idMethods = new HashMap<String, Executable>();
	
	ServerSocket server;
	int port;
	ArrayList<Socket> clients;
	
	Thread listeningThread;
	
	/**
	 * Executed the preStart()-Method,<br>
	 * creates a Server on <i>port</i><br>
	 * and starts the listening loop on its own thread.
	 * @param port the server shall work on
	 */
	public Server(int port){
		clients = new ArrayList<Socket>();
		this.port = port;
		
		registerMethod("LOGIN", new Executable() {
			@Override
			public void run(Datapackage msg, Socket socket) {
				registerClient(socket);
				onClientRegistered();
			}
		});
		
		preStart();
		
		start();
	}
	
	/**
	 * Executed while constructing the Server instance,<br>
	 * just before listening to data from the network starts.
	 */
	public abstract void preStart();
	
	/**
	 * Overwrite this method to react on a client registered (logged in)<br>
	 * to the server. That happens always, when a Datapackage<br>
	 * with identifier <i>LOGIN</i> is received from a client.
	 */
	public void onClientRegistered(){
	}
	
	private void startListening(){
		if(listeningThread == null && server != null){
			listeningThread = new Thread(new Runnable(){

				@Override
				public void run() {
					while(server != null){
						
						try {
							System.out.println("[Server] Waiting for connection...");
							final Socket tempSocket = server.accept();
							
							ObjectInputStream ois = new ObjectInputStream(tempSocket.getInputStream());
							Object raw = ois.readObject();
							
							if(raw instanceof Datapackage){
								final Datapackage msg = (Datapackage) raw;
								System.out.println("[Server] Message received: " + msg);
								
								for(final String current : idMethods.keySet()){
									if(msg.id().equalsIgnoreCase(current)){
										System.out.println("[Server] Executing method for identifier '" + msg.id() + "'");
										new Thread(new Runnable(){
											public void run(){
												idMethods.get(current).run(msg, tempSocket);
											}
										}).start();
										break;
									}
								}
																
							}
							
						} catch (IOException e) {
							e.printStackTrace();
						} catch (ClassNotFoundException e) {
							e.printStackTrace();
						}
						
					}
				}
			
			});
		
			listeningThread.start();
		}
	}
	
	/**
	 * Sends a Datapackage to a Socket
	 * @param message The Datapackage to be delivered
	 * @param socket The Socket the Datapackage shall be delivered to
	 */
	public void sendMessage(Datapackage message, Socket socket){		
		try {
			//Nachricht senden
			if(!socket.isConnected()){
				throw new Exception("Socket not connected.");
			}
			ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
			out.writeObject(message);
		} catch (Exception e){
			System.err.println("[SendMessage] Fehler: " + e.getMessage());
			//Bei Fehler: Socket aus Liste löschen
			if(toBeDeleted != null){
				toBeDeleted.add(socket);
			} else {
				clients.remove(socket);
			}
		}
	}
	
	private ArrayList<Socket> toBeDeleted;
	
	/**
	 * Broadcasts a Datapackage to every single logged-in socket,<br>
	 * one after another on the calling thread.<br>
	 * Every erroneous (unreachable etc.) socket is being removed in the end 
	 * @param message The Datapackage to be broadcasted
	 * @return The number of reachable the Datapackage has been delivered to 
	 */
	public int broadcastMessage(Datapackage message){
		toBeDeleted = new ArrayList<Socket>();
		
		//Nachricht an alle Sockets senden
		for(Socket current : clients){
			sendMessage(message, current);
		}
		
		//Alle Sockets, die fehlerhaft waren, im Anschluss löschen
		for(Socket current : toBeDeleted){
			clients.remove(current);
		}
		
		toBeDeleted = null;
		
		return clients.size();
	}
	
	/**
	 * Registers an Executable to be executed by the server<br>
	 * on an incoming Datapackage has <i>identifier</i> as its identifier.
	 * @param identifier The identifier the Executable is triggered by
	 * @param executable The Executable to be executed on arriving identifier
	 */
	public void registerMethod(String identifier, Executable executable){
		if(!identifier.equalsIgnoreCase("LOGIN")){
			idMethods.put(identifier, executable);
		} else {
			throw new IllegalArgumentException("Identifier may not be 'LOGIN'. "
					+ "Since v1.0.1 the server automatically registers new clients. "
					+ "To react on new client registed, use the onClientRegisters() Listener by overwriting it.");
		}
	}
	
	/**
	 * Registers a new client. From now on this Socket will receive broadcast messages.
	 * @param newClientSocket The Socket to be registerd
	 */
	public void registerClient(Socket newClientSocket){
		clients.add(newClientSocket);
	}
	
	private void start(){
		server = null;
		try {
			server = new ServerSocket(port);
		} catch (IOException e) {
			System.err.println("Error opening ServerSocket");
			e.printStackTrace();
		}
		startListening();
	}
	
	/**
	 * Interrupts the listening thread and closes the server
	 */
	public void stop(){
		if(listeningThread.isAlive()){
			listeningThread.interrupt();
		}
		
		if(server != null){
			try {
				server.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * @return The number of connected clients
	 */
	public int getClientCount(){
		return clients.size();
	}

}
