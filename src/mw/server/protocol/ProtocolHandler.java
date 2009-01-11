package mw.server.protocol;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author thuss
 *
 * Protocol handler interface that designates handling a specific protocol
 * through a StreamConnection.
 */
public abstract class ProtocolHandler {
    
    /**
     * The InputStream
     */
	protected InputStream in;
	
	/**
	 * The OutputStream
	 */
	protected OutputStream out;

	/**
	 * Sets the Streams
	 * @throws IOException Error opening the StreamConnection
	 */
	public void startProtocol(InputStream in, OutputStream out) {
		this.in = in;
		this.out = out;
	}
	
	/**
	 * Cleanup method when protocol work is complete
	 */
	public void stopProtocol() {
	}
	
	/** 
	 * Reads exactly one NMEA message and returns it, or returns an
	 * empty byte array if no messages were pending
	 * 
	 * @param blocking Whether this read should block until it gets data
	 */
	abstract public byte[] readIncomingData(boolean blocking) throws IOException;
	
	/**
	 * Write outgoing data to the OutputStream
	 * @param outgoing The outgoing data to write to the OutputStream
	 * @throws IOException Error writing to the StreamConnection
	 */
	abstract public void writeOutgoingData(byte[] outgoing) throws IOException;
}
