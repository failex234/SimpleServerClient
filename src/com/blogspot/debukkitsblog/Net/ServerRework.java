package com.blogspot.debukkitsblog.Net;

import javax.management.openmbean.KeyAlreadyExistsException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * A simple rework of the original SimpleServerClient
 * project by Leonard Bienbeck (DeBukkit).
 * This code is essentially very similar to the original one
 * but still a bit different
 *
 * !! Not yet done !!
 *
 * @author Felix Naumann
 * @version 0.0.1
 */
public abstract class ServerRework {

    private int port;
    private int timeout;

    private ArrayList<Socket> clients = new ArrayList<>();
    private HashMap<Socket, String> clientnames = new HashMap<>(); //Register on connect / demand nickname on connect
    private HashMap<String, Executable> methods = new HashMap<>();

    private ServerSocket server;

    private Thread connectionListenerThread;

    /**
     * Constructor for the server. The timeout parameter currently does
     * nothing.
     *
     * @param port    The port the server should listen on
     * @param timeout The time in seconds when a non-responding client should be unregistered-
     */
    public ServerRework(int port, int timeout) {
        this.port = port;
        this.timeout = timeout;

        preStart();

        startServer();
    }

    public abstract void preStart();

    public void onClientConnected(Socket client) {
    }

    public void onClientRegistered(Socket client, String name) {
    }

    public void onClientUnregistered(Socket client, String name) {
    }

    public void onClientDisconnected(Socket client) {
    }


    /**
     * Starts the main Listener Thread
     */
    public void startServer() {
        try {
            server = new ServerSocket(this.port);
        } catch (IOException e) {
            log("Error creating Socket!");
            e.printStackTrace();
            System.exit(1);
        }

        registerStandardMethods();

        log("Listening for connections...");
        connectionListenerThread = new Thread(() -> {
            while (true) {
                try {
                    Socket incomingSocket = server.accept();
                    ObjectInputStream stream = new ObjectInputStream(incomingSocket.getInputStream());

                    Object incomingObject = stream.readObject();

                    //Only process object if its an actual datapackage
                    if (incomingObject instanceof Datapackage) {
                        Datapackage pkg = (Datapackage) incomingObject;

                        String cmd = (String) pkg.get(0);

                        log("Received package with header \"" + cmd + "\"");
                        if (isRegistered(incomingSocket) || cmd.equals("_INTERNAL_LOGIN_")) {
                            for (String methodname : methods.keySet()) {
                                if (methodname.equals(cmd)) {
                                    new Thread(() -> methods.get(cmd).run(pkg, incomingSocket)).start();
                                    log("Executing method " + cmd + " for \"" + incomingSocket.getInetAddress().toString() + "\"(" + getName(incomingSocket) + ")");
                                }
                            }
                        } else {
                            log("Client " + incomingSocket.getInetAddress().toString() + " not registered thus executing not method");
                            log("Sending status");
                            sendPackage(new Datapackage("STATUS", "ERROR: not registered!"), incomingSocket);
                        }

                    }
                } catch (IOException e) {

                } catch (ClassNotFoundException cne) {

                }

            }
        });

        connectionListenerThread.start();
    }

    /**
     * Registers a client
     *
     * @param client The Client to register
     * @param name   The nickname of the client for better identification
     */
    private void registerClient(Socket client, String name) {
        if (!isRegistered(client)) {
            clients.add(client);
            clientnames.put(client, name);
        }
    }

    /**
     * Unregister a client
     *
     * @param client The client to unregister
     */
    private void unregisterClient(Socket client) {
        if (isRegistered(client)) {
            clients.remove(client);
            clientnames.remove(client);
        }
    }

    /**
     * Change name of a client. If the client wants to change his nickname
     *
     * @param client The client to change the nickname of
     * @param name   New nickname for the client
     */
    private void setName(Socket client, String name) {
        if (isRegistered(client)) {
            clientnames.replace(client, name);
        }
    }

    /**
     * Get name of a client
     *
     * @param client The client to get the name from
     * @return The name of the given client, null if client is not registered
     */
    private String getName(Socket client) {
        if (isRegistered(client)) {
            return clientnames.get(client);
        }
        return null;
    }

    /**
     * Check if a client is registered / connected
     *
     * @param client The client to check
     * @return true if client is connected / registered, otherwise false
     */
    private boolean isRegistered(Socket client) {
        if (clients.contains(client)) return true;
        return false;
    }

    private boolean isRegistered(String client) {
        if (clientnames.containsValue(client)) return true;
        return false;
    }

    /**
     * Registration of some standard methods that should exist from
     * the beginning to react to and with some basic i/o to and from
     * clients.
     */
    private void registerStandardMethods() {
        /**
         * Only register client when nickname is given.
         */
        methods.put("_INTERNAL_LOGIN_", (msg, socket) -> {
            try {
                String name = (String) msg.get(1);
                onClientConnected(socket);
                if (name == null) throw new NullPointerException("Nickname required!");
                registerClient(socket, name);
                onClientRegistered(socket, name);
                log("Client \"" + socket.getInetAddress() + "\" registered with nickname \"" + name + "\"");
                log("Sending status...");
                sendPackage(new Datapackage("STATUS_REGISTER", "Successfully registered with nickname " + name + "!"), socket);
            } catch (Exception e) {
                log("Client " + socket.getInetAddress().toString() + " tried to register but forgot a nickname.");
                log("Sending status...");
                sendPackage(new Datapackage("STATUS_REGISTER", "Registration failed! Nickname missing!"), socket);
            }
        });

        methods.put("_SET_NAME_", (msg, socket) -> {
            setName(socket, msg.get(1).toString());
            sendPackage(new Datapackage("STATUS", "OK"), socket);
            log("Client \"" + socket.getInetAddress().toString() + "\" (" + getName(socket) + ") set name to " + msg.get(1));
        });

        methods.put("_GET_NAME_", (msg, socket) -> {
            sendPackage(new Datapackage("NAMEREQUEST", getName(socket)), socket);
            log("Client \"" + socket.getInetAddress().toString() + "\" (" + getName(socket) + ") requested name");
        });

        methods.put("_MSG_", (msg, socket) -> {
            log("There are currently " + clients.size() + " Clients connected!");
            if (!isRegistered(socket)) {
                sendPackage(new Datapackage("STATUS", "Error: Please set Nickname!"), socket);
                log("Nickname not set! Sending status...");
            } else {
                String message = "";
                String receiver = "";

                /*
                Two Exception Catchers to exactly tell if the message or the receiver was forgotten.
                 */
                try {
                    message = msg.get(1).toString();

                } catch (IndexOutOfBoundsException e) {
                    sendPackage(new Datapackage("STATUS", "Error: No Message sent!"), socket);
                    return;
                }

                try {
                    receiver = msg.get(2).toString();
                } catch (IndexOutOfBoundsException e) {
                    sendPackage(new Datapackage("STATUS", "Error: No Receiver sent!"), socket);
                    return;
                }
                Socket receiver_sock = null;
                for (Socket key : clientnames.keySet()) {
                    if (clientnames.get(key).equals(receiver)) {
                        receiver_sock = key;
                    }
                }

                if (isRegistered(receiver)) {
                    sendPackage(new Datapackage("_MSG_", message), receiver_sock);
                    sendPackage(new Datapackage("STATUS", "OK"), socket);
                } else {
                    sendPackage(new Datapackage("STATUS", "Error: Client not found!"), socket);
                }
            }
        });

        methods.put("_BROADCAST_", (msg, socket) -> {
            if (!isRegistered(socket)) {
                sendPackage(new Datapackage("STATUS", "Error: Please set Nickname!"), socket);
            } else {
                String message = "";
                try {
                    message = msg.get(1).toString();
                } catch (IndexOutOfBoundsException e) {
                    sendPackage(new Datapackage("STATUS", "Error: No Message sent!"), socket);
                    return;
                }
                String sender = getName(socket);
                sendPackage(new Datapackage("STATUS", "OK"), socket);
                broadcast(new Datapackage("_BROADCAST_", sender, message));
            }
        });

        methods.put("_INTERNAL_LOGOUT_", (pack, socket) -> {
            onClientUnregistered(socket, getName(socket));
            unregisterClient(socket);
            log("Client \"" + socket.getInetAddress() + "\"(" + getName(socket) + ") just unregistered");
            log("Sending status...");
            sendPackage(new Datapackage("STATUS", "OK"), socket);
            onClientDisconnected(socket);
        });

    }

    /**
     * Sends a Datapackage to a client
     *
     * @param content  The Datapackage to sent (First element should always be the identifier!)
     * @param receiver The client to send the Datapackage to
     */
    public void sendPackage(Datapackage content, Socket receiver) {
        if (receiver.isConnected()) {
            try {
                ObjectOutputStream out = new ObjectOutputStream(receiver.getOutputStream());
                out.writeObject(content);
            } catch (IOException e) {
                log("An Exception occurred whilst trying to send a package to \"" + receiver.getInetAddress().toString() + "\"(" + getName(receiver) + ")");
                e.printStackTrace();
            }
        } else {
            log("Socket not connected!");
        }
    }

    /**
     * Broadcasts a Datapackage to all connected clients
     *
     * @param content The Datapackage to broadcast
     */
    public void broadcast(Datapackage content) {
        log("Broadcasting to all clients...");
        for (int i = 0; i < clients.size(); i++) {
            sendPackage(content, clients.get(i));
        }
    }

    /**
     * Register any method except for the already standard ones
     *
     * @param identifier The identifier of the method
     * @param e          The Executable (The code that gets executed)
     */
    public void registerMethod(String identifier, Executable e) {
        if (identifier.equals("_INTERNAL_LOGIN_")) {
            throw new IllegalArgumentException("Identifier may not be '_INTERNAL_LOGIN_'. "
                    + "This method is already taken for communication between "
                    + "the server and the respective clients.");
        } else if (identifier.equals("_MSG_")) {
            throw new IllegalArgumentException("Identifier may not be '_MSG_'. "
                    + "This method is already taken for communication between "
                    + "the server and the respective clients.");
        } else if (identifier.equals("_BROADCAST_")) {
            throw new IllegalArgumentException("Identifier may not be '_BROADCAST_'. "
                    + "This method is already taken for communication between "
                    + "the server and the respective clients.");
        } else if (identifier.equals("_GET_NAME_")) {
            throw new IllegalArgumentException("Identifier may not be '_GET_NAME_'. "
                    + "This method is already taken for nickname exchange "
                    + "between the server and the respective clients");
        } else if (identifier.equals("_SET_NAME_")) {
            throw new IllegalArgumentException("Identifier may not be '_SET_NAME_'. "
                    + "This method is already taken for nickname exchange "
                    + "between the server and the respective clients");
        } else if (methods.containsKey(identifier)) {
            throw new KeyAlreadyExistsException("Identifier may not be '" + identifier + "'."
                    + "This method is already registered. If you want to "
                    + "replace the method you have to use unregisterMethod() first!");
        } else {
            methods.put(identifier, e);
        }
    }

    /**
     * Unregisters a method
     *
     * @param identifier The method to unregister
     */
    public void unregisterMethod(String identifier) {
        if (identifier.equals("_INTERNAL_LOGIN_")) {
            throw new IllegalArgumentException("Unable to unregister '_INTERNAL_LOGIN_'. " +
                    "This method cannot be unregistered!");
        } else if (identifier.equals("_MSG_")) {
            throw new IllegalArgumentException("Unable to unregister '_MSG_'. " +
                    "This method cannot be unregistered!");
        } else if (identifier.equals("_BROADCAST_")) {
            throw new IllegalArgumentException("Unable to unregister '_BROADCAST_'. " +
                    "This method cannot be unregistered!");
        } else if (identifier.equals("_GET_NAME_")) {
            throw new IllegalArgumentException("Unable to unregister '_GET_NAME_'. " +
                    "This method cannot be unregistered!");
        } else if (identifier.equals("_SET_NAME_")) {
            throw new IllegalArgumentException("Unable to unregister '_SET_NAME_'. " +
                    "This method cannot be unregistered!");
        } else if (!methods.containsKey(identifier)) {
            throw new IllegalArgumentException("Unable to unregister '" + identifier + "'. " +
                    "This method is not registered!");
        } else {
            methods.remove(identifier);
        }
    }

    /**
     * Returns a count of all connected client
     *
     * @return The count of all connected / registered clients
     */
    public int getClientCount() {
        return this.clients.size();
    }

    /**
     * Kill the server
     */
    public void kill() {
        try {
            connectionListenerThread.interrupt();
            server.close();
        } catch (Exception e) {
            log("An error occurred whilst trying to kill the server...");
            e.printStackTrace();
        }
    }

    /**
     * Log to console
     *
     * @param msg The message to log
     */
    private void log(String msg) {
        System.out.println("[SERVER] " + msg);
    }
}
