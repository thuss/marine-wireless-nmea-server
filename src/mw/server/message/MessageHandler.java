package mw.server.message;

/**
 * @author thuss
 *
 * Methods all message handlers must implement. Currently the message handler is only 
 * responsible for creating a message.
 */
public interface MessageHandler {


	/**
	 * Method createMessage takes the sender_id, sender_name, and message content
	 * and builds a message object.
	 * 
	 * @param sender_id The unique id of the mediator
	 * @param sender_name The unique name of the mediator
	 * @param content The messag content
	 * @return Message
	 */
	public Message createMessage(int sender_id, String sender_name, byte[] content);

}
