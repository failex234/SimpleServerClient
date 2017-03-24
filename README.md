### SimpleServerClient ###
Offers very simple and easy-to-use Java classes for Client-Server-Client or just Server-Client applications doing all the work for connection setup, reconnection, timeout, keep-alive, etc. in the background.

# How to use THE SERVER
```java
import java.net.Socket;

import com.blogspot.debukkitsblog.Util.Datapackage;
import com.blogspot.debukkitsblog.Util.Executable;
import com.blogspot.debukkitsblog.Util.Server;

public class MyServer extends Server {

	public MyServer(int port) {
		super(port, true, true);
	}

	@Override
	public void preStart() {
		registerMethod("Ping", new Executable() {
			@Override
			public void run(Datapackage msg, Socket socket) {
				sendMessage(new Datapackage("REPLY", "Pong"), socket);
			}
		});
	}

}
```



Just make your own class, e. g. MyServer extending Server, simply use the original constructor and implement
the preStart method. In the preStart method just add
```java
  registerMethod("IAMANIDENTIFIER", new Executable() {
			@Override
			public void run(Datapackage msg, Socket socket) {
				  doSomethingWith(msg, socket);
			}
	});
```
for every identifier of an Datapackge the server received, you want to react to.

EXAMPLE: So if you register "Ping" and an Executable repsonding "Pong" to the client, just register
```java
  registerMethod("Ping", new Executable() {
			@Override
			public void run(Datapackage msg, Socket socket) {
				  sendMessage(new Datapackage("IdentifierForTheReplyPackage", "Pong"), socket);
			}
	});
```
and that's it.

For more identifiers to react on, just put those lines multiple times into your preStart(). Do not forget to send
a reply to the clients you got the Datapackge from, because it will wait until world ends for a reply.

EXAMPLE for a server broadcasting a chat-message to all connected clients:
```java
  registerMethod("Message", new Executable() {			
			@Override
			public void run(Datapackage msg, Socket socket) {
			  	System.out.println("[Message] New chat message arrived, delivering to all the clients...");
			  	broadcastMessage(msg); //The broadcast to all the receivers
			  	sendMessage(new Datapackage("REPLY", String.valueOf(reveicerCount)), socket); //The reply (NECESSARY! unless you want the client to block while waiting for this package)
			  	close(socket); //Close the connection to the socket you got the Datapackage from
			}
	});
```

	
# How to use THE CLIENT
```java
import java.net.Socket;

import com.blogspot.debukkitsblog.Util.Client;
import com.blogspot.debukkitsblog.Util.Datapackage;
import com.blogspot.debukkitsblog.Util.Executable;

public class MyClient extends Client {

	public MyClient(String address, int port) {
		super(address, port, 10000, false);

		registerMethod("Message", new Executable() {
			@Override
			public void run(Datapackage msg, Socket socket) {
				System.out.println("Look! I got a new message from the server: " + msg.get(1));
			}
		});

		start();
	}

}
```



Just make your own class, e. g. MyClient extending Client, simply use the original constructor.
Whenever you are ready for the client to login, call start(). The client will connect to the server
depending on the constructor-parameters and register itself on the server. From now on it can
receive messages from the server and stay connected (and reconnects if necessary) until you call stop().


To react on an incoming message, just add
```java
		registerMethod("IAMANIDENTIFIER", new Executable() {
			@Override
			public void run(Datapackage msg, Socket socket) {
				doSomethingWith(msg, socket);		
			}
		});
```
somewhere, I suggest the constructor itself.


EXMAPLE for an incoming chat message from the server:
```java
		registerMethod("Message", new Executable() {
			@Override
			public void run(Datapackage msg, Socket socket) {
				System.out.println("Look! I got a new message from the server: " + msg.get(1));				
			}
		});
```

Different from the client, the server will not expect a reply by default. So dont always send him an Reply-Package, because he
needs an extra-identifier-method registered for that.


# Useful methods
AS SERVER:

  Broadcast messages using: broadcastMessage(Datapackage)
  
  Send messages to a specified client using: sendMessage(Datapackage, Socket)
  
  Receive messages from the client using registerMethod-Executables
  
AS CLIENT:

  Send messages to the server using: sendMessage(different parameters)
  
  Receive replys to this message using its return value (that will be reply Datapackage)
  
  Receive messages from the server using registerMethod-Executables


# Event handlers
As the client: To react on a (re)connect or disconnect from/to the server,
just override the onConnectionProblem() or onConnectionGood() methods and fill them with your code.
onConnectionGood() is called on every (re)connect AND successful registration to the server,
onConnectionProblem() is called on every exception thrown inside the listener thread and every disconnect (responding very fast!)
