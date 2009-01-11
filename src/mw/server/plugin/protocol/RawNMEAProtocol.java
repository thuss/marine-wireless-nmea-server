package mw.server.plugin.protocol;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import mw.server.protocol.ProtocolHandler;

/**
 * @author thuss
 *
 * Raw NMEA protocol handler that can either read or write raw
 * NMEA data to a StreamConnection.
 */
public class RawNMEAProtocol extends ProtocolHandler {

	/**
	 * Place we store a line of nmea data as we read it
	 */
	protected ByteArrayOutputStream nmealine = new ByteArrayOutputStream();
		
	/** 
	 * When set to true the first line ever received will be discarded.
	 * We do this when working with serial ports since the first line is
	 * almost always only partially complete. Not an issue with sockets.
	 */
	protected boolean linediscard = true;

	/** 
	 * Reads exactly one line and returns it, or returns an
	 * empty 0 length byte array if no messages were pending
	 * 
	 * @param blocking Whether the read should block until a line of data arrives
	 * @return byte[] A line of raw NMEA data
	 */
	public byte[] readIncomingData(boolean blocking) throws IOException {
		byte[] incoming = {};
		if (!blocking && in.available() == 0) return incoming;
		
		// Get as much data as we can
		boolean eol = false;
		int abyte = 0;
		do {
			abyte = in.read();
			if (abyte == '\n') {
				eol = true;
			} else if (abyte == '\r') {
				// Do nothing
			} else if (abyte == -1) {
				Thread.dumpStack();
			} else {
				nmealine.write(abyte);
			}
		} while ((in.available() > 0 || blocking) && !eol);
		
		// If we have a complete line
		if (eol) {
			// This code will only run the first time we get a line
			// and we do this to discard the first line since it
			// is almost always a partial and incomplete message when
			// used via serial port
			if (linediscard) {
				linediscard = false;
				nmealine.reset();
				return incoming;
			}
			
			incoming = nmealine.toByteArray();
			nmealine.reset();
		} else {
			
		}
		return incoming;
	}
	
	/**
	 * Write Raw NMEA data to the StreamConnection
	 * 
	 * @param outgoing The outgoing data to write
	 * @return IOException Error writing data to the stream connection
	 */
	public void writeOutgoingData(byte[] outgoing) throws IOException {
		out.write(outgoing);
		// Since it's NMEA we must include CR (ascii 13) and LF (ascii 10)
		out.write('\r');
		out.write('\n');
	}
	
	/**
	 * Returns the linediscard parameter. If true the first line of input
	 * will be discarded. This is to handle continuous connections such
	 * as a serial connection.
	 * 
	 * @return boolean If the first line of data should be discarded.
	 */
	public boolean getLinediscard() {
		return linediscard;
	}

	/**
	 * Set the linediscard parameter. If true the first line of input
	 * will be discarded. This is to handle continuous connections such
	 * as a serial connection where the first line of data will often 
	 * be incomplete.
	 * 
	 * @param linediscard Set to true to discard the first line of data
	 */
	public void setLinediscard(boolean linediscard) {
		this.linediscard = linediscard;
	}

}
