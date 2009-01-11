package mw.server.connection;

import java.io.IOException;

/**
 * @author thuss
 *
 * This is the interface for common methods of connection layers.
 * ConnectionLayers can be serial ports, IR ports, Server sockets,
 * etc...
 */
public interface ConnectionLayer {

	/**
	 * Method openConnection opens the connection to the underlying
	 * layer such as a client socket, serial port, ir port, etc...
	 * @throws IOException IO error while opening the connection
	 */
	public void openConnection() throws IOException;

	/**
	 * Close the connection to the underlying layer. 
	 * E.g. serial port or server socket.
	 * 
	 * @throws IOException IOError while closing the layer
	 */
	public void closeConnection() throws IOException;

	/**
	 * The parameter string to pass the connection layer 
	 * implementation
	 */
	public void setParameters(String params);

}
