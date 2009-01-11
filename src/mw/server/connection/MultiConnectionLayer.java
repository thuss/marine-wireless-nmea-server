package mw.server.connection;

import java.net.ServerSocket;

/**
 * @author thuss
 *
 * Connection Layer for a multipoint connection such as a server socket.
 */
public interface MultiConnectionLayer extends ConnectionLayer {

	/**
	 * Returns the Server Socket for this multi connection layer
	 * @return ServerSocket to accept incoming connections on
	 * @throws IOException IOError opening the layer.
	 */
	public ServerSocket getServerSocket();

}
