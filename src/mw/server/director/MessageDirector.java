package mw.server.director;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;

import mw.server.display.DisplayLED;
import mw.server.message.Message;

import mw.server.mediator.Mediator;

/**
 * @author thuss
 *
 * The MessageDirector is the hub of the server where all messages 
 * come in and go out of. A mediator (e.g. RawNMEAonCOM1) may register
 * with the director to receive messages. When the director gets an
 * appropriate message it will then send that message to the mediator.
 * The mediator may also send messages to the director so if a message
 * comes in on say Com1 the mediator will then forward that message to
 * the director.
 * 
 * A common interaction may look like this:
 * COM1 <--> RawNMEAonCOM1 <--> Director <--> RawNMEAonPort8080 <--> TCPIP
 */
public class MessageDirector implements Runnable {

	/**
	 * When this gets set to true the director thread terminates
	 */
	protected boolean terminate = false;
	
	protected Logger log = Logger.getLogger(MessageDirector.class);

	/**
	 * The director to communicate with
	 */
	protected static MessageDirector md = new MessageDirector();

	/**
	 * Passive mediators (such as a serversocket) do not deal with
	 * messages (e.g. NMEA), they simply monitor the connectionlayer
	 * to start real communication on (e.g. client socket) that do 
	 * deal with messages.
	 */
	protected List passivemediators = Collections.synchronizedList(new ArrayList());

	/**
	 * Listening mediators deal with messages and need to get 
	 * notified when an appropriate message arrives
	 */
	protected List listeningmediators = Collections.synchronizedList(new ArrayList());

	/** 
	 * The queue of messages waiting for the director to handle
	 */
	protected List messages = Collections.synchronizedList(new ArrayList());

	/**
	 * The current highest unique mediator id issued. This is used
	 * just like a sequence in a database to generate unique ID's for
	 * each mediator.
	 */
	protected int mediator_id = 0;

	/**
	 * This is a singleton so we protect the constructor from classes
	 * outside from instantiating.
	 */
	protected MessageDirector() {

	}

	/**
	 * Get the singleton instance of the Message director
	 */
	public static MessageDirector getInstance() {
		return md;
	}

	/**
	 * A mediator calls this method to register itself to
	 * receive messages.
	 * 
	 * @param med The mediator to register for receiving messages
	 */
	public void registerMediator(Mediator med) {
			if (med.isListener()) {
				listeningmediators.add(med);
			} else {
				passivemediators.add(med);
			}
	}

	/**
	 * A mediator calls this method to unregister itself so
	 * it will no longer receive messages.
	 * 
	 * @param med The mediator to unregister
	 */
	public void unregisterMediator(Mediator med) {
			if (med.isListener()) {
				listeningmediators.remove(med);
			} else {
				passivemediators.remove(med);
			}
	}

	/**
	 * Send a message to the director (this class). This
	 * method puts the message on a queue to be processed
	 * later in the directors thread.
	 * 
	 * @param msg The message to send to the director
	 */
	public void sendMessage(Message msg) {
		// If we already have the msg pending don't send a dup
		if (!messages.contains(msg)) {
			messages.add(msg);
		} 
	}

	/**
	 * Get messages that are pending handling by the director.
	 * This method avoids an extra synchronization call by
	 * pointing a new variable at the old arraylist and then
	 * creating a new arraylist (which is an atomic operation)
	 */
	protected List getPendingMessages() {
		List pendingmsgs = messages;
		messages = Collections.synchronizedList(new ArrayList());
		return pendingmsgs;
	}

	/** 
	 * Check whether we have messages pending. This method does
	 * not need to be synchronized because it simply reads the size.
	 * 
	 * @return boolean True if there are messages pending
	 */
	protected boolean pendingMessages() {
		if (messages.size() > 0) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Get a new and unique id for a mediator.
	 * This method needs to be synchronized because different
	 * mediator threads may be calling it to get new ids
	 * 
	 * @return int The new mediator id
	 */
	public synchronized int getNewMediatorId() {
		return mediator_id++;
	}

	/**
	 * Sets the terminate flag terminating the directors operation
	 * 
	 * @param terminate The terminate to set
	 */
	public void setTerminate(boolean terminate) {
		this.terminate = terminate;
	}

	/**
	 * This method distibutes the messages to all registered
	 * listening mediators.
	 * 
	 * @param msgs The messages to distribute
	 */
	protected void distributeMessages(List msgs) {
		// Loop over the messages
		Mediator med = null;
		Message msg = null;
		for (int i = 0; i < msgs.size(); i++) {
			msg = (Message) msgs.get(i);
			for (int j = 0; j < listeningmediators.size(); j++) {
				med = (Mediator) listeningmediators.get(j);
				// We skip the sending mediator
				if (msg.getSenderId() != med.getId()) {
					med.sendMessage(msg);
				}
			}
		}
	}

	/**
	 * Starts the message director running and handling messages
	 */
	public void run() {
		// Let everyone know we've started
		log.info("Director: started");
		log.info("Server started");
		// Flash the LED to signal that the server started
		DisplayLED led = DisplayLED.getInstance();
		new Thread(led).start();
		
		// Begin the main loop
		List pendingMsgs = null;
		while (!terminate) {
			// See if we have incoming messages	
			if (pendingMessages()) {
				pendingMsgs = getPendingMessages();
				distributeMessages(pendingMsgs);
				pendingMsgs = null;
				// Now sleep for a moment
				try {
					// System.gc();
					Thread.sleep(100);
				} catch (InterruptedException e) {
				}
			}
		}
		log.info("Director: stopped");

		// We've been told to terminate	so terminate mediators
		terminateMediators();
	}

	/**
	 * Terminates all mediators registered with the director
	 */
	protected void terminateMediators() {
		// Combine all mediators
		List mediators = new ArrayList();
		mediators.addAll(listeningmediators);
		mediators.addAll(passivemediators);
		for (int i = 0; i < mediators.size(); i++) {
			Mediator med = (Mediator) mediators.get(i);
			med.setTerminate(true);
		}

		// Sleep for a second
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
		}

		// Do it again due to multipoint children 
		for (int i = 0; i < mediators.size(); i++) {
			Mediator med = (Mediator) mediators.get(i);
			med.setTerminate(true);
		}

		// If we were to terminate and restart make sure
		// the lists are there to handle it.
		listeningmediators = new ArrayList();
		passivemediators = new ArrayList();
	}
}
