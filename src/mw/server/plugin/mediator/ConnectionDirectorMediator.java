package mw.server.plugin.mediator;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;

import mw.server.connection.ConnectionLayer;
import mw.server.connection.MultiConnectionLayer;
import mw.server.connection.SingleConnectionLayer;
import mw.server.director.MessageDirector;
import mw.server.display.DisplayLED;
import mw.server.mediator.Mediator;
import mw.server.message.Message;
import mw.server.message.MessageHandler;
import mw.server.plugin.connection.SerialPortConnection;
import mw.server.plugin.message.NMEAMessageHandler;
import mw.server.plugin.protocol.RawNMEAProtocol;
import mw.server.protocol.ProtocolHandler;

/**
 * @author thuss
 *
 * The ConnectionDirectorMediator (great name huh) mediates all
 * data flow from the connection to the message director. It handles
 * capturing data from the connectionlayer, forwarding it to the
 * protocol handler, forwarding it to the messagehandler, and then
 * finally delivering it the message director.
 * 
 * Example:
 * Serial -> RawNMEA -> NMEAMessageHandler -> MessageDirector
 * 
 * It additionally has support for multipoint connection layers such
 * as a ServerSocket or single point to point connection layers such
 * as a serial port.
 */
public class ConnectionDirectorMediator implements Mediator {
	
	protected static Logger log = Logger.getLogger(ConnectionDirectorMediator.class);

	/**
	 * Flag to terminate the run method
	 */
	protected boolean terminate = false;
	
	/**
	 * Flag if mediator should auto restart on error
	 */
	protected boolean restart = false;
	
	/**
	 * Whether we turned on the error light
	 */
	protected boolean errorLight = false;
	
	/**
	 * The LED control class
	 */
	protected DisplayLED displayLED = DisplayLED.getInstance();

	/**
	 * An arbitrary name for this mediator. (e.g RawNMEASerial)
	 */
	protected String name;

	/**
	 * A unique id for each mediator. The director uses this to ensure 
	 * messages sent from this id are not sent back
	 */
	protected int id;

	/**
	 * Default inactivity timeout in milliseconds.
	 * Setting to 0 means there is no inactivity timeout
	 */
	protected long timeout = 0;

	/**
	 * Messages received from the director
	 */
	protected List directormsgs = new ArrayList();

	/**
	 * When set to true we will register with the director to receive
	 * messages.
	 */
	protected boolean listener = true;

	/**
	 * The messagehandler
	 */
	protected MessageHandler messagehandler;

	/**
	 * The connectionlayer
	 */
	protected ConnectionLayer connectionlayer;

	/**
	 * The protocolhandler
	 */
	protected ProtocolHandler protocolhandler;

	/**
	 * The messagedirector
	 */
	protected MessageDirector messagedirector;

	/** 
	 * The streamconnection
	 */
	protected Socket socket;

	/**
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		log.info("Mediator: " + name + " id=" + id + " started");
		// We use a do loop here since a restartable mediator may need to
		// be restarted if it has an error
		do {
			try {
				// Determine if it's a single or multipoint connection layer
				if (connectionlayer instanceof SingleConnectionLayer) {
					runSingleConnectionLayer();	
				} else if (connectionlayer instanceof MultiConnectionLayer) {
					runMultiConnectionLayer();
				}
			} catch (Exception e) {
				log.warn(e);
			}
			
			// Print restarting message
			if (this.isRestart()) {
				log.info("Mediator: " + name + " id=" + 
								   id + " restarting");
				try { Thread.sleep(500); } catch (Exception e) {}
				directormsgs.clear();
			}
		} while (this.isRestart());
		log.info("Mediator: " + name + " id=" + id + " stopped");
	}

	/**
	 * Method runMultiConnectionLayer handles the running of a multiconnection
	 * layer and determining when the it should switch over to handle an
	 * actual connection. It does this when the streamconnection is set.
	 * 
	 * @throws IOException Error reading/writing to the stream
	 */
	protected void runMultiConnectionLayer() throws IOException {
		// If null, this is the primary multipoint connection listener
		// such as a serversocket
		if (socket == null) {
			// We need to get the connection notifier and run new threads
			// for each new connection that is opened. Since we're a
			// multipoint parent we don't listen for messages.
			listener = false;
			messagedirector.registerMediator(this);
			MultiConnectionLayer mcl = (MultiConnectionLayer) connectionlayer;
			try {
				mcl.openConnection();
				ServerSocket ssock = mcl.getServerSocket();
				// When each connection arrives give it it's own thread
				while (!terminate) {
					Socket sock = ssock.accept();
					Mediator med = cloneForNewThread(sock);
					// Now start the thread
					if (med != null) {
						new Thread(med).start();
					}
					sock = null;
					med = null;
				}
			} finally {
				mcl.closeConnection();
				mcl = null;
			}
		} else {
			// We are now a streamconnection handling thread	
			// (e.g. socket spun of from a serversocket)
			try {
				// Cause inactive multipoint connections to timeout after
				// 30 seconds of no activity
				setTimeout(30000);
				runOnStreams(socket.getInputStream(), socket.getOutputStream());
			} finally {
				socket.close();
				socket = null;
			}
		}

	}

	/**
	 * Method cloneForNewThread is called when a multiconnectionlayer needs
	 * to clone itself to handle an actual connection stream. For example
	 * when a ServerSocket gets an incoming connection it switches over to h
	 * handle the actual connection on the Socket.
	 * 
	 * @param sc The stream connection to handle the connection
	 * @return Mediator The cloned mediator.
	 */
	protected Mediator cloneForNewThread(Socket sock) {
		ConnectionDirectorMediator mediator = null;
		try {
			// Create the new classes
			// Note connectionlayer and message director do not need cloning
			mediator =
				(ConnectionDirectorMediator) this.getClass().newInstance();
			ProtocolHandler ph =
				(ProtocolHandler) protocolhandler.getClass().newInstance();
			MessageHandler mh =
				(MessageHandler) messagehandler.getClass().newInstance();

			// Set the necessary values
			mediator.setSocket(sock);
			int newid = messagedirector.getNewMediatorId();
			mediator.setParameters(
				newid,
				name + "_" + newid,
				connectionlayer,
				ph,
				mh,
				messagedirector);
		} catch (Exception e) {
			log.warn(e);
		}
		return mediator;
	}

	/**
	 * Method runSingleConnectionLayer is called to handle a connection
	 * from a singleconnectionlayer such as a serial port.
	 * 
	 * @throws IOException Error reading/writing to the StreamConnection
	 */
	protected void runSingleConnectionLayer() throws IOException {
		SingleConnectionLayer scl = (SingleConnectionLayer) connectionlayer;
		try {
			scl.openConnection();
			runOnStreams(scl.getInputStream(), scl.getOutputStream());
		} finally {
			scl.closeConnection();
			scl = null;
		}
	}

	/**
	 * Method runOnStreamConnection will start the reading/writing
	 * process of handling data on a stream connection.
	 * 
	 * @throws IOException Error reading/writing to the StreamConnection
	 */
	protected void runOnStreams(InputStream in, OutputStream out)
		throws IOException {
		protocolhandler.startProtocol(in, out);
		try {
			// Register with the director now that we're ready to roll
			messagedirector.registerMediator(this);
			long lastactivity = System.currentTimeMillis();
			byte[] data = {};
			List msgs = null;
			int msgindex = -1;
			Message msg = null;
			
			// Uncomment to enable bps reporting
			long start = System.currentTimeMillis();
			long bytecount = 0;
			
			while (!terminate) {
				// Read 1 incoming message but don't block on it
				try {
					data = protocolhandler.readIncomingData(false);
				} catch (IOException e) {
					terminate = true;
					break;
				}
				if (data.length > 0) {
					msg = messagehandler.createMessage(id, name, data);
					// Send message to the director
					messagedirector.sendMessage(msg);
					
					// Uncomment to enable bps reporting
					float diff = (System.currentTimeMillis() - start) / 1000;
					bytecount += data.length + 2;
					if (diff >= 30) {
						float bps = (bytecount * 8) / diff;
						log.info(name + " averaged sending " + 
							bps + "bps");
						start = System.currentTimeMillis();
						bytecount = 0;	
					}
				}
				
				// Now deliver 1 message that is pending (if any)
				// First check if we need to retrieve a new batch of msgs
				if (msgindex == -1) {
					boolean pending = pendingMessages();
					if (pending) {
						msgs = getPendingMessages();
						msgindex = 0;
					}
				}
				
				// See if we have a message to deliver
				if (msgindex >= 0 && msgindex < msgs.size()) {
					msg = (Message) msgs.get(msgindex);
					msgindex++;
					// If we're about to send last message, reset to get more
					if (msgindex == msgs.size()) {
						msgindex = -1;
						msgs = null;
					}
					try {				
						protocolhandler.writeOutgoingData(msg.getMessage());
						// msg.setDelivered(true);
					} catch (IOException e) {
						if (name.startsWith("NMEAon")) {
							log.warn(e);
						}
						terminate = true;	
						break;
					}
				}

				// If we had nothing to do
				if (msg == null) {
					// If we have an inactivity timeout check it
					if (timeout > 0) {
						long inactivity =
							System.currentTimeMillis() - lastactivity;
						if (inactivity >= timeout)
							terminate = true;
					}

					// Sleep if we had nothing to do before trying again
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
					}
				} else {
					// Log our last activity
					lastactivity = System.currentTimeMillis();
					// Clear the message for starters
					msg = null;
				}
			}
		} finally {
			// If we turned errorlight on earlier, now turn it off
			if (errorLight) {
				displayLED.setErrorLight(name, false);
			}
			messagedirector.unregisterMediator(this);
			protocolhandler.stopProtocol();
			messagedirector = null;
			protocolhandler = null;
			connectionlayer = null;
		}
	}

	/**
	 * @see mw.server.mediator.Mediator#setParameters(int, String, ConnectionLayer, ProtocolHandler, MessageHandler, MessageDirector)
	 */
	public void setParameters(
		int id,
		String name,
		ConnectionLayer cl,
		ProtocolHandler ph,
		MessageHandler mh,
		MessageDirector md) {
		this.id = id;
		this.name = name;
		this.connectionlayer = cl;
		this.protocolhandler = ph;
		this.messagehandler = mh;
		this.messagedirector = md;

	}

	/** 
	 * This method gets called by the Director when a message
	 * needs to go out and gets put on our queue of outgoing 
	 * messages.
	 * 
	 * @param msg The message to be sent
	 */
	public void sendMessage(Message msg) {
		// If we already have the msg pending don't send a dup
		if (!directormsgs.contains(msg)) {
			directormsgs.add(msg);
		}
	}

	/**
	 * Get any pending messages. When this method is called the messages 
	 * returned are removed from the list of pending messages.
	 * 
	 * @return List Messages pending handling
	 */
	protected List getPendingMessages() {
		// Get the pending messages and allocate new buffer list
		List pendingmsgs = directormsgs;
		directormsgs = Collections.synchronizedList(new ArrayList());
		// Now check if our backlog is growing too large and drop old msgs
		int maxsize = 20;
		int psize = pendingmsgs.size();
		
		// Check if we have too many and need to drop some
		if (psize > maxsize) {
			// We have to drop messages so first thing is to turn
			// on the error light
			errorLight = true;
			displayLED.setErrorLight(name, true);
			
			// Now make sure we don't drop mandatory messages
			Message msg = null;
			List mustsend = new ArrayList();
			for (int i = 0; i < psize - maxsize; i++) {
				msg = (Message)pendingmsgs.get(i);
				if (msg.isMandatory()) {
					mustsend.add(msg);
				}
			}
			
			// To avoid infinite sending of mandatory messages
			// we must drop even mandatory messages if there are
			// too many
			if (mustsend.size() > maxsize) {
				// Truncate the mustsend list
				mustsend.subList(0, mustsend.size() - maxsize).clear();
			}
			
			// Now combine the remaining pendingmsgs and mustsend list
			pendingmsgs = pendingmsgs.subList(psize - maxsize, psize);
			pendingmsgs.addAll(0, mustsend);
			
			log.warn(name + " has too many ("+psize+") pending messages: sending " + 
					pendingmsgs.size());
		} else if (errorLight) {
			// We turned it on earlier and now we must turn it off
			displayLED.setErrorLight(name, false);
		}
		return pendingmsgs;
	}

	/** 
	 * Check whether we have messages pending.
	 * 
	 * @return boolean True if we have messages pending
	 */
	protected boolean pendingMessages() {
		if (directormsgs.size() > 0) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Returns the streamconnection.
	 * 
	 * @return StreamConnection
	 */
	public Socket getStreamConnection() {
		return socket;
	}

	/**
	 * Sets the streamconnection.
	 * 
	 * @param streamconnection The streamconnection to set
	 */
	public void setSocket(Socket sock) {
		this.socket = sock;
	}

	/**
	 * Returns the terminate flag
	 * 
	 * @return boolean
	 */
	public boolean getTerminate() {
		return terminate;
	}

	/**
	 * Sets the terminate.
	 * 
	 * @param terminate The terminate to set
	 */
	public void setTerminate(boolean terminate) {
		this.terminate = terminate;
	}

	/**
	 * Returns the unique name of the mediator
	 * 
	 * @return String
	 */
	public String getName() {
		return name;
	}

	/**
	 * Sets the name.
	 * 
	 * @param name The name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Returns the unique id of the mediator
	 * 
	 * @return int
	 */
	public int getId() {
		return id;
	}

	/**
	 * Sets the unique id of the mediator
	 * 
	 * @param id The id to set
	 */
	public void setId(int id) {
		this.id = id;
	}

	/**
	 * Returns the listener.
	 * @return boolean
	 */
	public boolean isListener() {
		return listener;
	}

	public static void main(String[] args) {
		try {
			log.info("ConnectionDirectorMediator Start");
			// As a sample we try to read raw NMEA protocol over COM1
			SingleConnectionLayer cl = new SerialPortConnection();
			cl.openConnection();
			ProtocolHandler ph = new RawNMEAProtocol();
			MessageHandler mh = new NMEAMessageHandler();
			ph.startProtocol(cl.getInputStream(), cl.getOutputStream());
			byte[] data;
			for (int i = 0; i < 20000; i++) {
				// Read incoming data but don't block on it
				data = ph.readIncomingData(false);
				if (data.length > 0) {
					Message msg = mh.createMessage(1, "TestMediator", data);
					// Now we would usually deliver the message to the 
					// director.. instead we just print it out
					String msgtext = new String(msg.getMessage());
					log.info("Received: " + msgtext);
				}
			}
			ph.stopProtocol();
			cl.closeConnection();
			log.info("ConnectionDirectorMediator Stop");
		} catch (Exception e) {
			log.warn(e);
		}
	}

	/**
	 * Returns the timeout.
	 * 
	 * @return long
	 */
	public long getTimeout() {
		return timeout;
	}

	/**
	 * Sets the timeout.
	 * 
	 * @param timeout The timeout to set
	 */
	public void setTimeout(long timeout) {
		this.timeout = timeout;
	}

	/**
	 * Returns the connectionlayer.
	 * 
	 * @return ConnectionLayer
	 */
	public ConnectionLayer getConnectionlayer() {
		return connectionlayer;
	}
	/**
	 * @return Returns the restart flag.
	 */
	public boolean isRestart() {
		return restart;
	}

	/**
	 * @param restart Set the restart flag
	 */
	public void setRestart(boolean restart) {
		this.restart = restart;
	}

}
