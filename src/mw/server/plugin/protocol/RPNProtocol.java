package mw.server.plugin.protocol;

import java.io.IOException;

/**
 * @author thuss
 *
 * This class implements the Rose Point Navigation protocol handle
 * both SUBSCRIBE and NOTIFY requests
 */
public class RPNProtocol extends RawNMEAProtocol {

	protected boolean subscribed = false;

	/**
	 * Constructor.
	 */
	public RPNProtocol() {
		setLinediscard(false);
	}

	/**
	 * This method currently doesn't return anything, however, it must
	 * be called because it handles the RPN Subscription request.
	 * 
	 * @see mw.server.protocol.ProtocolHandler#readIncomingData(boolean)
	 */
	public byte[] readIncomingData(boolean blocking) throws IOException {
		byte[] incoming = new byte[0];
		byte[] inc = super.readIncomingData(blocking);
		// If we have a successful subscription don't waste time parsing anymore
		if (inc.length > 0 && !subscribed) {
			String line = new String(inc);
			if (line.startsWith("SUBSCRIBE")) {
				String aline = null;
				// Now that we have the subscribe loop until we
				// get an empty line (just CRLF);
				do {
					aline = new String(super.readIncomingData(true));
					if (aline.length() == 0)
						break;
				} while (aline.length() > 0);
				String rpnmessage = "HTTP/1.1 200 OK\r\n\r\n";
				out.write(rpnmessage.getBytes());
				subscribed = true;
			}
		}
		return incoming;
	}

	/**
	 * Outgoing data is always surrounded by http rpn NOTIFY message
	 * 
	 * @see mw.server.protocol.ProtocolHandler#writeOutgoingData(byte[])
	 */
	public void writeOutgoingData(byte[] outgoing) throws IOException {
		// We won't start sending data until they have subscribed
		if (!subscribed)
			return;
		String rpnmessage =
			"NOTIFY the-source HTTP/1.1\r\n"
				+ "Content-Type: text/nmea0183\r\n"
				+ "Content-Length: "
				+ outgoing.length
				+ "\r\n\r\n";
		out.write(rpnmessage.getBytes());
		out.write(outgoing);
		out.write("\r\n".getBytes());
	}
}
