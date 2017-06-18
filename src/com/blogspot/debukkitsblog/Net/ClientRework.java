package com.blogspot.debukkitsblog.Net;

import javax.management.openmbean.KeyAlreadyExistsException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;

/**
 * A simple rework of DeBukkits Client-Class
 * This is essentially the same but also not.
 *
 * !! Not yet done !!
 *
 * @author Felix Naumann
 * @version 0.0.1
 */
public class ClientRework {

    private InetSocketAddress serveraddress;
    private int timeout;
    private String name;
    private Thread connectionListenerThread;

    private HashMap<String, Executable> methods = new HashMap<>();

    private Socket server;

    private boolean connected = false;
    private boolean registering = false;
    private boolean registered = false;

    /**
     * Autoconnect on object creation
     *
     * @param ip IP of Server
     * @param port Port of Server
     * @param timeout Timeout in milliseconds
     * @param nickname Desired client nickname
     */
    public ClientRework(String ip, int port, int timeout, String nickname) {
        try {
            //Just a simple check, to see if host is up
            InetAddress.getByName(ip);
        } catch (UnknownHostException e) {
            log("Host not found! No connection will be made!");
            e.printStackTrace();
            System.exit(1);
        }
        serveraddress = new InetSocketAddress(ip, port);
        this.timeout = timeout;
        this.name = nickname;

        if (nickname == null || nickname.isEmpty()) {
            log("No nickname given! No connection will be made!");
            System.exit(1);
        }

        try {
            server = new Socket();
            server.connect(serveraddress, this.timeout);
        } catch (IOException e) {
            log("Error creating socket! Program will exit now!");
            e.printStackTrace();
            System.exit(1);
        }
        log("Successfully connected to " + ip + ":" + port + "!");
        connected = true;

        registerStandardMethods();

        startListenerThread();
        log("Registering at server...");
        sendPackage(new Datapackage("_INTERNAL_LOGIN_", nickname));
        this.registering = true;
    }

    public void startListenerThread() {
        connectionListenerThread = new Thread(() -> {
            while (connected) {
                try {
                    ObjectInputStream input = new ObjectInputStream(server.getInputStream());
                    Object obj = input.readObject();

                    if (obj instanceof Datapackage) {
                        Datapackage pkg = (Datapackage) obj;
                        log("Received Datapackage with header \"" + pkg.get(0) + "\"");

                        for (String method : methods.keySet()) {
                            if (method.equals(pkg.get(0))) {
                                log("Executing method for " + method);
                                new Thread(() -> methods.get(method).run(pkg, server)).start();
                            }
                        }
                    }
                } catch (IOException e) {
                    log("Error while trying to create InputStream!");
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
                    log("An error occurred!");
                    e.printStackTrace();
                }
            }
        });
        connectionListenerThread.start();
    }

    /**
     * Replies by the server won't get handled by this method
     *
     * @param content
     */
    public void sendPackage(Datapackage content) {
        try {
            Socket temp = new Socket();
            temp.connect(serveraddress, timeout);

            ObjectOutputStream tempOOS = new ObjectOutputStream(temp.getOutputStream());
            tempOOS.writeObject(content);

            tempOOS.flush();
            tempOOS.close();
            temp.close();
        } catch (IOException e) {
            log("An error occurred!");
            e.printStackTrace();
        }
    }

    /**
     * Sends a full datapackages to all connected clients
     *
     * @param content The datapackage to send (without identifier)
     */
    public void broadcast(Datapackage content) {
        this.sendPackage(new Datapackage("_BROADCAST_", content));
    }

    private void registerStandardMethods() {
        methods.put("NAMEREQUEST", (msg, socket) -> {
            name = msg.get(1).toString();
            log("NAMEREQUEST: Client-Name set to " + name);
        });

        methods.put("STATUS", (msg, socket) -> log("STATUS: " + msg.get(1)));

        methods.put("STATUS_REGISTER", (pack, socket) -> {
            if (registering) {
                String status = (String) pack.get(1);
                log(status);
                if (!status.contains("failed")) {
                    registered = true;
                }
                registering = false;
            }
        });
    }

    public void registerMethod(String identifier, Executable e) {
        if (identifier.equals("NAMEREQUEST")) {
            throw new IllegalArgumentException("Identifier may not be 'NAMEREQUEST'. "
                    + "This method is already taken for nickname exchange "
                    + "between the server and the client");
        } else if (identifier.equals("STATUS")) {
            throw new IllegalArgumentException("Identifier may not be 'STATUS'. "
                    + "This method is already taken for command status exchange "
                    + "between the server and the client");
        } else if (methods.containsKey(identifier)) {
            throw new KeyAlreadyExistsException("Identifier may not be '" + identifier + "'."
                    + "This method is already registered. If you want to "
                    + "replace the method you have to use unregisterMethod() first!");
        } else if (identifier.equals("STATUS_REGISTER")) {
            throw new IllegalArgumentException("Identifier may not be 'STATUS_REGISTER'. "
                    + "This method is already taken for nickname exchange "
                    + "between the server and the client");
        } else {
            methods.put(identifier, e);
            log("Method \"" + identifier + "\" successfully registered!");
        }
    }

    public void unregisterMethod(String identifier) {
        if (identifier.equals("NAMEREQUEST")) {
            throw new IllegalArgumentException("Unable to unregister 'NAMEREQUEST'. "
                    + "This method cannot be unregistered!");
        } else if (identifier.equals("STATUS")) {
            throw new IllegalArgumentException("Unable to unregister 'STATUS'. "
                    + "This method cannot be unregistered!");
        } else if (!methods.containsKey(identifier)) {
            throw new IllegalArgumentException("Unable to unregister '" + identifier + "'. " +
                    "This method is not registered!");
        } else if (identifier.equals("STATUS_REGISTER")) {
            throw new IllegalArgumentException("Unable to unregister 'STATUS_REGISTER'. "
                    + "This method cannot be unregistered!");
        } else {
            methods.remove(identifier);
            log("Method \"" + identifier + "\" successfully unregistered!");
        }
    }


    public void log(String msg) {
        System.out.println("[CLIENT] " + msg);
    }
}
