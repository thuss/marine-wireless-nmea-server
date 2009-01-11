package mw.server.mediator;

import mw.server.connection.ConnectionLayer;
import mw.server.director.MessageDirector;
import mw.server.message.Message;
import mw.server.message.MessageHandler;
import mw.server.protocol.ProtocolHandler;

/**
 * @author thuss
 *
 * The mediator interface defines the methods all mediator
 * implementations must implement. A mediator handles the
 * flow of data between the Connection Layer (e.g. socket),
 * ProtocolHandler (e.g. rawnmea), and the MessageHandler
 * (e.g. NMEAMessageHandler). Any implemented mediator should
 * be thread safe to run in its own Thread.
 */
public interface Mediator extends Runnable {

	/**
	 * Method setParameters must be called before the mediators run method
	 * is called to prepare it for running.
	 * 
	 * @param id JVM Unique id to assign this mediator
	 * @param name Unique name to assign this mediator (e.g. RawNMEAonCOM1)
	 * @param cl ConnectionLayer (e.g. ServerSocketConnection)
	 * @param ph ProtocolHandler (e.g. RawNMEAProtocol)
	 * @param mh MessageHandler (e.g. NMEAMessageHandler)
	 * @param md MessageDirector(e.g. MessageDirector.getInstance())
	 */
	public void setParameters(
		int id,
		String name,
		ConnectionLayer cl,
		ProtocolHandler ph,
		MessageHandler mh,
		MessageDirector md);

	public void sendMessage(Message msg);

	/** 
	 * This method is invoked to start the mediator running and
	 * managing the flow of data.
	 */
	public void run();

	/**
	 * Returns true if this mediator is a message listener (if
	 * the mediator wants to receive messages from the director).
	 * 
	 * @return boolean True if it should receive message from director
	 */
	public boolean isListener();

	/**
	 * Returns the name.
	 * @return String
	 */
	public String getName();

	/**
	 * Sets the name.
	 * @param name The name to set
	 */
	public void setName(String name);
	
	/**
	 * Get whether this mediator should auto restart
	 * @return true if this mediator should auto restart
	 */
	public boolean isRestart();
	
	/**
	 * Set whether this mediator should auto restart
	 * @param restart Set to true if mediator should auto restart
	 */
	public void setRestart(boolean restart);

	/**
	 * Returns the id.
	 * @return int
	 */
	public int getId();

	/**
	 * Sets the id.
	 * @param id The id to set
	 */
	public void setId(int id);

	/**
	 * Returns the terminate flag
	 * @return boolean
	 */
	public boolean getTerminate();

	/**
	 * Sets the terminate flag
	 * @param terminate The terminate to set
	 */
	public void setTerminate(boolean terminate);
	
	/**
	 * Returns the connectionlayer.
	 * 
	 * @return ConnectionLayer
	 */
	public ConnectionLayer getConnectionlayer();
}
