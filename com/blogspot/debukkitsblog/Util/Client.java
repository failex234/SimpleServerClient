package com.blogspot.debukkitsblog.Util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.AlreadyConnectedException;
import java.util.HashMap;

/**
 * A very simple Client class for Java network applications
 * @author Leonard Bienbeck
 * @version 1.0.0
 * created on 09.03.2016 in Horstmar, NRW, Germany
 */
public class Client {

	private Socket loginSocket;
	private InetSocketAddress address;
	private int timeout;
	private Thread listeningThread;
	private HashMap<String, Executable> idMethods = new HashMap<String, Executable>();
	private int errorCount;
	private boolean autoKill = false;
	
	/**
	 * Builds a network client. To login and start listening call <b>start()</b><br>
	 * Register handlers for incoming packages using registerMethod(String, Executable) in your subclass-constructor<br>
	 * @param address The address to connect to, e. g. an IP or domainname
	 * @param port The port to connect to, e. g. 8112
	 * @param timeout The time after further tries to connect are aborted (in milliseconds)
	 * @param autoKill Wether the system should be shut down (exit) after 30 insuccessful tries of reconnecting
	 */
	public Client(String address, int port, int timeout, boolean autoKill){
		this.errorCount = 0;
		this.address = new InetSocketAddress(address, port);
		this.timeout = timeout;
		this.autoKill = autoKill;
	}
	
	/**
	 * Starts the client:<br>
	 * 1. Login to the server (on calling thread)<br>
	 * 2. Start listening to datapackages from the server; repair connection if necessary
	 */
	public void start(){
		login();
		startListening();
	}
	
	private void repairConnection(){
		System.out.println("[Client-Connection-Repair] Repairing connection...");
		if(loginSocket != null){
			try { loginSocket.close(); } catch (IOException e) { }
			loginSocket = null;
		}
		
		login();
		startListening();
	}
	
	private void login(){
		//Verbindung herstellen
		try{
			System.out.println("[Client] Connecting...");
			if(loginSocket != null && loginSocket.isConnected()){
				throw new AlreadyConnectedException();
			}
			loginSocket = new Socket();
			loginSocket.connect(this.address, this.timeout);
			System.out.println("[Client] Connected to " + loginSocket.getRemoteSocketAddress());
		} catch(IOException ex){
			System.err.println("[Client] Connection failed: " + ex.getMessage());
			onConnectionProblem();
		}
		
		//Einloggen
		try{
			System.out.println("[Client] Logging in...");
			ObjectOutputStream out = new ObjectOutputStream(loginSocket.getOutputStream());
			out.writeObject(new Datapackage("LOGIN", "HELO"));
			System.out.println("[Client] Logged in.");
		} catch(IOException ex){
			System.err.println("[Client] Login failed.");
		}		
	}
	
	private void startListening(){
		
		//Wenn der ListeningThread lebt, nicht neu starten!
		if(listeningThread != null && listeningThread.isAlive()){
			return;
		}
		
		listeningThread = new Thread(new Runnable(){
			@Override
			public void run() {
				
				//Wiederhole ständig die Prozedur:
				while(true){
					try{
						//Bei fehlerhafter Verbindung, diese reparieren
						if(loginSocket != null && !loginSocket.isConnected()){
							while(!loginSocket.isConnected()){
								repairConnection();
								if(loginSocket.isConnected()){
									break; //diese, kleinere, innere while-Schleife! -- nicht while(true)
								}
								
								Thread.sleep(5000);
								repairConnection();
							}
						}
						
						onConnectionGood();
						
						//Auf eingehende Nachricht warten und diese bei Eintreffen lesen
						ObjectInputStream ois = new ObjectInputStream(loginSocket.getInputStream());
						Object raw = ois.readObject();
						
						//Nachricht auswerten
						if(raw instanceof Datapackage){
							final Datapackage msg = (Datapackage) raw;
							
							for(final String current : idMethods.keySet()){
								if(msg.id().equalsIgnoreCase(current)){
									System.out.println("[Client] Message received. Executing method for '" + msg.id() + "'...");
									new Thread(new Runnable(){
										public void run(){
											idMethods.get(current).run(msg, loginSocket);
										}
									}).start();
									break;
								}
							}
															
						}
						
					} catch(Exception ex){					
						if(ex.getMessage().equals("Connection reset")){
							onConnectionProblem();
							System.err.println("[WARNING] Server offline.");
							if((++errorCount > 30) && autoKill){
								System.err.println("[ERROR] Server dauerhaft nicht erreichbar, beende.");
								System.exit(0);
							} else {
								repairConnection();
							}							
						} else {
							ex.printStackTrace();
						}
					}
					
					//Bis hieher fehlerfrei? Dann errorCount auf Null setzen:
					errorCount = 0;
					
				} //while true
				
			}//run			
		});
		
		//Thread starten
		listeningThread.start();
			
	}
	
	/**
	 * Sends a datapackage to the server, aborting on timeout<br>
	 * @param message The Datapackage you want to send to the server
	 * @param timeout The time in milliseconds the try shall be aborted after
	 * @return a Datapackage, the reply from the server
	 */
	public Datapackage sendMessage(Datapackage message, int timeout){
		try{
			Socket tempSocket = new Socket();
			tempSocket.connect(address, timeout);
			
			ObjectOutputStream tempOOS = new ObjectOutputStream(tempSocket.getOutputStream());
			tempOOS.writeObject(message);
			
			ObjectInputStream tempOIS = new ObjectInputStream(tempSocket.getInputStream());
			Object raw = tempOIS.readObject();
			
			tempOOS.close();
			tempOIS.close();
			tempSocket.close();
			
			if(raw instanceof Datapackage){
				return (Datapackage) raw;
			}
		} catch(Exception ex){
			System.err.println("[Client] Error while sending message:");
			ex.printStackTrace();
		}
		
		return null;
	}
	
	/**
	 * Sends a message to the server consisting of an identifier-String (header)<br>
	 * for the server to separate all the messages and as many strings as you want<br>
	 * for data (body).
	 * @param ID Identifier (header) for separating and identification
	 * @param content Content of your message (lots of Strings)
	 * @return a Datapackage, the reply from the server
	 */
	public Datapackage sendMessage(String ID, String... content){
		return sendMessage(new Datapackage(ID, (Object[]) content));
	}
	
	/**
	 * Sends a datapackage to the server, aborting on timeout<br>
	 * @param message The Datapackage you want to send to the server
	 * @return a Datapackage, the reply from the server
	 */
	public Datapackage sendMessage(Datapackage message){
		return sendMessage(message, this.timeout);
	}
	
	/**
	 * Registers an Executable to be run on Datapackge with <i>identifier</i> incoming 
	 * @param identifier The identifier to be reacted on
	 * @param executable The Executable ro be run
	 */
	public void registerMethod(String identifier, Executable executable){
		idMethods.put(identifier, executable);
	}
	
	/**
	 * Called when the server is disconnected and repairing of the connection (reconnect) starts
	 */
	public void onConnectionProblem(){ }
	
	/**
	 * Called when (re)connection to the server and waiting for an incoming message starts
	 */
	public void onConnectionGood(){ }
	
}
