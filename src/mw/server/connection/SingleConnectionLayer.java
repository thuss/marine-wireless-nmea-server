package mw.server.connection;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author thuss
 *
 * Connection Layer for a point to point single connection such as a
 * serial port or client socket.
 */
public interface SingleConnectionLayer extends ConnectionLayer {

	public InputStream getInputStream();
	
	public OutputStream getOutputStream();
}
