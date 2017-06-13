package com.blogspot.debukkitsblog.Net;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.AlreadyConnectedException;
import java.util.HashMap;

import javax.management.openmbean.KeyAlreadyExistsException;
import javax.net.ssl.SSLSocketFactory;

/**
 * A very simple Client class for Java network applications<br>
 * created on 09.03.2016 in Horstmar, NRW, Germany
 *
 * @author Leonard Bienbeck
 * @author Felix Naumann
 * @version 2.1.0
 */
public class Client {

    private Socket loginSocket;
    private InetSocketAddress address;
    private int timeout;

    private Thread listeningThread;
    private HashMap<String, Executable> idMethods = new HashMap<String, Executable>();

    private int errorCount;

    private boolean autoKill = false;
    private boolean secureMode;

    private String clientName = "";

    /**
     * Builds a network client. To login and start listening call <b>start()</b>.
     * <br>
     * Register handlers for incoming packages using<br>
     * registerMethod(String, Executable) in your subclass-constructor<br>
     * <br>
     * The connection timeout is 10 seconds,<br>
     * the client will not kill itself after 30 non-successful tries of<br>
     * (re)connecting and SSL is used to encrypt communication (beta stage!).<br>
     * <br>
     *
     * @param address The address to connect to, e. g. an IP or domainname
     * @param port    The port to connect to, e. g. 8112
     */
    public Client(String address, int port) {
        this(address, port, 10000, false, false);
    }

    /**
     * Builds a network client. To login and start listening call <b>start()</b>
     * <br>
     * Register handlers for incoming packages using registerMethod(String,
     * Executable) in your subclass-constructor<br>
     *
     * @param address  The address to connect to, e. g. an IP or domainname
     * @param port     The port to connect to, e. g. 8112
     * @param timeout  The time after further tries to connect are aborted (in
     *                 milliseconds)
     * @param autoKill whether the system should be shut down (exit) after 30
     *                 non-successful tries of (re)connecting
     * @param useSSL   whether SSL should be used to encrypt communication
     */
    public Client(String address, int port, int timeout, boolean autoKill, boolean useSSL) {
        this.errorCount = 0;
        this.address = new InetSocketAddress(address, port);
        this.timeout = timeout;
        this.autoKill = autoKill;

        registerStandardMethods();

        if (secureMode = useSSL) {
            System.setProperty("javax.net.ssl.trustStore", "ssc.store");
            System.setProperty("javax.net.ssl.keyStorePassword", "SimpleServerClient");
        }
    }

    /**
     * Starts the client:<br>
     * 1. Login to the server (on calling thread)<br>
     * 2. Start listening to datapackages from the server; repair connection if
     * necessary
     */
    public void start() {
        login();
        startListening();
    }

    /**
     * Registering the standard methods however
     * you SHOULD register _MSG_ and _BROADCAST_ for
     * yourself, to make everything you want with it.
     * First element(1) of Datapackage is always the message.
     * Second element(2) of Datapackage is the senders name.
     */
    public void registerStandardMethods() {
        idMethods.put("NAMEREQUEST", new Executable() {
            @Override
            public void run(Datapackage msg, Socket socket) {
                clientName = msg.get(1).toString();
                log("NAMEREQUEST: Client-Name set to " + clientName);
            }
        });

        idMethods.put("STATUS", new Executable() {
            @Override
            public void run(Datapackage msg, Socket socket) {
                log("STATUS: " + msg.get(1));
            }
        });

    }

    private void repairConnection() {
        System.out.println("[Client-Connection-Repair] Repairing connection...");
        if (loginSocket != null) {
            try {
                loginSocket.close();
            } catch (IOException e) {
            }
            loginSocket = null;
        }

        login();
        startListening();
    }

    private void login() {
        // Verbindung herstellen
        try {
            System.out.println("[Client] Connecting" + (secureMode ? " using SSL..." : "..."));
            if (loginSocket != null && loginSocket.isConnected()) {
                throw new AlreadyConnectedException();
            }

            if (secureMode) {
                loginSocket = ((SSLSocketFactory) SSLSocketFactory.getDefault()).createSocket(address.getAddress(),
                        address.getPort());
            } else {
                loginSocket = new Socket();
                loginSocket.connect(this.address, this.timeout);
            }

            System.out.println("[Client] Connected to " + loginSocket.getRemoteSocketAddress());
        } catch (IOException ex) {
            ex.printStackTrace();
            // System.err.println("[Client] Connection failed: " +
            // ex.getMessage());
            onConnectionProblem();
        }

        // Einloggen
        try {
            System.out.println("[Client] Logging in...");
            ObjectOutputStream out = new ObjectOutputStream(loginSocket.getOutputStream());
            out.writeObject(new Datapackage("_INTERNAL_LOGIN_", "HELO"));
            System.out.println("[Client] Logged in.");
            onReconnect();
        } catch (IOException ex) {
            System.err.println("[Client] Login failed.");
        }
    }

    private void startListening() {

        // Don't restart if ListeningThread is still alive!
        if (listeningThread != null && listeningThread.isAlive()) {
            return;
        }

        listeningThread = new Thread(new Runnable() {
            @Override
            public void run() {

                // Repeat everything
                while (true) {
                    try {
                        // Repair failed connection
                        if (loginSocket != null && !loginSocket.isConnected()) {
                            while (!loginSocket.isConnected()) {
                                repairConnection();
                                if (loginSocket.isConnected()) {
                                    break;
                                }

                                Thread.sleep(5000);
                                repairConnection();
                            }
                        }

                        onConnectionGood();

                        // Wait for incoming messages and read them
                        // on receive
                        ObjectInputStream ois = new ObjectInputStream(loginSocket.getInputStream());
                        Object raw = ois.readObject();

                        // Evaluating the Message
                        if (raw instanceof Datapackage) {
                            final Datapackage msg = (Datapackage) raw;

                            for (final String current : idMethods.keySet()) {
                                if (msg.id().equalsIgnoreCase(current)) {
                                    System.out.println(
                                            "[Client] Message received. Executing method for '" + msg.id() + "'...");
                                    new Thread(new Runnable() {
                                        public void run() {
                                            idMethods.get(current).run(msg, loginSocket);
                                        }
                                    }).start();
                                    break;
                                }
                            }

                        }

                    } catch (Exception ex) {
                        ex.printStackTrace();
                        onConnectionProblem();
                        System.err.println("Server offline?");
                        if ((++errorCount > 30) && autoKill) {
                            System.err.println("Cannot reach server. Exiting...");
                            System.exit(0);
                        } else {
                            repairConnection();
                        }
                    }

                    // If no errors occured set errorCount to 0
                    errorCount = 0;

                } // while true

            }// run
        });

        // start Thread
        listeningThread.start();

    }

    /**
     * Sends a datapackage to the server, aborting on timeout<br>
     *
     * @param message The Datapackage you want to send to the server
     * @param timeout The time in milliseconds the try shall be aborted after
     * @return a Datapackage, the reply from the server
     */
    public Datapackage sendMessage(Datapackage message, int timeout) {
        try {
            Socket tempSocket;
            if (secureMode) {
                tempSocket = ((SSLSocketFactory) SSLSocketFactory.getDefault()).createSocket(address.getAddress(),
                        address.getPort());
            } else {
                tempSocket = new Socket();
                tempSocket.connect(address, timeout);
            }

            ObjectOutputStream tempOOS = new ObjectOutputStream(tempSocket.getOutputStream());
            tempOOS.writeObject(message);

            ObjectInputStream tempOIS = new ObjectInputStream(tempSocket.getInputStream());
            Object raw = tempOIS.readObject();

            tempOOS.close();
            tempOIS.close();
            tempSocket.close();

            if (raw instanceof Datapackage) {
                return (Datapackage) raw;
            }
        } catch (Exception ex) {
            System.err.println("[Client] Error while sending message:");
            ex.printStackTrace();
        }

        return null;
    }

    /**
     * Sends a message to the server consisting of an identifier-String (header)
     * <br>
     * for the server to separate all the messages and as many strings as you
     * want<br>
     * for data (body).
     *
     * @param ID      Identifier (header) for separating and identification
     * @param content Content of your message (lots of Strings)
     * @return a Datapackage, the reply from the server
     */
    public Datapackage sendMessage(String ID, String... content) {
        return sendMessage(new Datapackage(ID, (Object[]) content));
    }

    /**
     * Sends a datapackage to the server, aborting on timeout<br>
     *
     * @param message The Datapackage you want to send to the server
     * @return a Datapackage, the reply from the server
     */
    public Datapackage sendMessage(Datapackage message) {
        return sendMessage(message, this.timeout);
    }

    /**
     * Registers an Executable to be run on Datapackge with <i>identifier</i>
     * incoming
     *
     * @param identifier The identifier to be reacted on
     * @param executable The Executable ro be run
     */
    public void registerMethod(String identifier, Executable executable) {
        if (identifier.equalsIgnoreCase("NAMEREQUEST")) {
            throw new IllegalArgumentException("Identifier may not be 'NAMEREQUEST'. "
                    + "This method is already taken for nickname exchange "
                    + "between the server and the client");
        } else if (identifier.equalsIgnoreCase("STATUS")) {
            throw new IllegalArgumentException("Identifier may not be 'STATUS'. "
                    + "This method is already taken for command status exchange "
                    + "between the server and the client");
        } else if (idMethods.containsKey(identifier)) {
            throw new KeyAlreadyExistsException("Identifier may not be '" + identifier + "'."
                    + "This method is already registered. If you want to "
                    + "replace the method you have to use unregisterMethod() first!");
        } else {
            idMethods.put(identifier, executable);
        }
    }
    /**
     * Unregisters a Method that is registered on the client
     *
     * @param identifier The method to unregister
     */
    public void unregisterMethod(String identifier) {
        if (identifier.equalsIgnoreCase("NAMEREQUEST")) {
            throw new IllegalArgumentException("Unable to unregister 'NAMEREQUEST'. "
                    + "This method cannot be unregistered!");
        } else if (identifier.equalsIgnoreCase("STATUS")) {
            throw new IllegalArgumentException("Unable to unregister 'STATUS'. "
                    + "This method cannot be unregistered!");
        } else if (!idMethods.containsKey(identifier)) {
            throw new IllegalArgumentException("Unable to unregister '" + identifier + "'. " +
                    "This method is not registered!");
        } else {
            idMethods.remove(identifier);
        }
    }

    /**
     * Called when the server is disconnected and repairing of the connection
     * (reconnect) starts.<br>
     * Warning: This method is executed on the main networking thread!
     */
    public void onConnectionProblem() {
    }

    /**
     * Called when (re)connection to the server and waiting for an incoming
     * message starts.<br>
     * Warning: This method is executed on the main networking thread!
     */
    public void onConnectionGood() {
    }

    /**
     * Called when the client logges in to the server for the first time.<br>
     * Warning: This method is executed on the main networking thread!
     */
    public void onReconnect() {
    }

    /**
     * Logs a message to the console. May be appropiate for later use
     * @param msg The message to log
     */
    public void log(String msg) {
        System.out.println(msg);
    }

}
