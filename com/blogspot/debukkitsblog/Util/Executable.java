package com.blogspot.debukkitsblog.Util;

import java.net.Socket;

public interface Executable {
	
	/**
	 * Put the lines to be executed inside the run.<br>
	 * This method is called by the Client or Server if the identifier<br>
	 * of an Datapackage fits to the identifier this Executable is registered with.
	 * @param msg the Datapackage to work with
	 * @param socket the Socket to work with
	 */
	public abstract void run(Datapackage msg, Socket socket);

}
