package com.blogspot.debukkitsblog.Net;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by felix on 18.06.17.
 */
public abstract class ServerRework {

    private int port;
    private int timeout;

    private ArrayList<Socket> clients = new ArrayList<>();
    private HashMap<Socket, String> clientnames = new HashMap<>(); //Register on connect / demand nickname on connect
    private HashMap<String, Executable> methods = new HashMap<>();

    private ServerSocket server;

    private Thread connectionListenerThread;

    public ServerRework(int port, int timeout) {
        this.port = port;
        this.timeout = timeout;

        preStart();

        startServer();
    }

    public abstract void preStart();

    public abstract void onClientConnected();

    public abstract void onClientRegistered(Socket client, String name);

    public abstract void onClientUnregistered();

    public abstract void onClientDisconnected();


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
                        if (isRegistered(incomingSocket)) {
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

    private void registerClient(Socket client, String name) {
        if (!isRegistered(client)) {
            clients.add(client);
            clientnames.put(client, name);
        }
    }

    private void setName(Socket client, String name) {
        if (isRegistered(client)) {
            clientnames.replace(client, name);
        }
    }

    private String getName(Socket client) {
        if (isRegistered(client)) {
            return clientnames.get(client);
        }
        return null;
    }

    private boolean isRegistered(Socket client) {
        if (clients.contains(client)) return true;
        return false;
    }

    private boolean isRegistered(String client) {
        if (clientnames.containsValue(client)) return true;
        return false;
    }

    private void registerStandardMethods() {
        /**
         * Only register client when nickname is given.
         */
        methods.put("_INTERNAL_LOGIN_", (msg, socket) -> {
            try {
                String name = (String) msg.get(1);
                if (name == null) throw new NullPointerException("Nickname required!");
                registerClient(socket, name);
                onClientRegistered(socket, name);
                log("Client \"" + socket.getInetAddress() + "\" registered with nickname \"" + name + "\"");
            } catch (Exception e) {
                log("Client " + socket.getInetAddress().toString() + " tried to register but forgot a nickname.");
                log("Sending status...");
                sendPackage(new Datapackage("STATUS", "Registration failed! Nickname missing!"), socket);
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

    }

    public void sendPackage(Datapackage content, Socket receiver) {

    }

    public void broadcast(Datapackage content) {
        log("Broadcasting to all clients...");
        for (int i = 0; i < clients.size(); i++) {
            sendPackage(content, clients.get(i));
        }
    }

    public void log(String msg) {
        System.out.println("[SERVER] " + msg);
    }
}
