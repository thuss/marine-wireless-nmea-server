package mw.server.plugin.connection;

import mw.server.connection.MultiConnectionLayer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;

/**
 * @author thuss
 *
 * Server Socket Connection Layer which listents on the port specified in 
 * the parameters method.
 */
public class ServerSocketConnection implements MultiConnectionLayer {
	
	protected static Logger log = Logger.getLogger(ServerSocketConnection.class);

	/**
	 * The ServerSocket
	 */
	protected ServerSocket serversocket;

	/**
	 * Connection parameters
	 */
	protected String params;

	/**
	 * @see mw.server.connection.MultiConnectionLayer#openConnection()
	 */
	public void openConnection() throws IOException {
		
		// Set the default values
		int port = 8080;

		// Get the parameters
		if (params != null) {
			StringTokenizer strtok = new StringTokenizer(params, ":");
			StringTokenizer strtok2 = null;
			String badparam = null;
			while (strtok.hasMoreTokens()) {
				String nexttok = strtok.nextToken();
				strtok2 = new StringTokenizer(nexttok, "=");
				if (strtok2.hasMoreTokens()) {
					String param = strtok2.nextToken();
					if (param.equalsIgnoreCase("port")
						&& strtok2.hasMoreTokens()) {
						try {
							port = new Integer(strtok2.nextToken()).intValue();
						} catch (NumberFormatException e) {
							badparam = nexttok;
						}
					} else {
						badparam = nexttok;
					}
				} else {
					badparam = nexttok;
				}

				// Show warning message
				if (badparam != null) {
					String msg =
						"WARNING: "
							+ this.getClass().getName()
							+ " ignoring unrecognized parameter "
							+ badparam;
					log.warn(msg);
					badparam = null;
				}
			}
		}

		serversocket = new ServerSocket(port);
	}

	/**
	 * @see mw.server.connection.MultiConnectionLayer#getServerSocket()
	 */
	public ServerSocket getServerSocket() {
		return serversocket;
	}

	/**
	 * @see mw.server.connection.ConnectionLayer#closeConnection()
	 */
	public void closeConnection() throws IOException {
		if (serversocket != null)
			serversocket.close();
	}

	/**
	 * @see mw.server.connection.ConnectionLayer#setParameters(String)
	 */
	public void setParameters(String params) {
		this.params = params;
	}

	/**
	 * Test method which reads from the socket connection on port 8080 
	 * and prints output
	 * @param args Command line arguments are ignored
	 */
	public static void main(String[] args) {
		ServerSocketConnection ssc = null;
		ServerSocket ss;
		try {
			log.info("Starting Server Socket Connection");
			ssc = new ServerSocketConnection();
			ssc.openConnection();
			ss = ssc.getServerSocket();
			Socket sock = ss.accept();
			InputStream in = sock.getInputStream();

			ByteArrayOutputStream baos = new ByteArrayOutputStream(32);
			byte[] buffer = new byte[32];
			int nread = 0;
			while ((nread = in.read(buffer)) > -1) {
				baos.write(buffer, 0, nread);
				if (nread > 0) {
					System.out.print(baos.toString());
					baos.reset();
				}
			}
			in.close();
			sock.close();
			log.info("");
			log.info("Done");
		} catch (Exception e) {
			log.warn(e);
		} finally {
			try {
				if (ssc != null)
					ssc.closeConnection();
			} catch (Exception e) {
			}
		}

	}
}