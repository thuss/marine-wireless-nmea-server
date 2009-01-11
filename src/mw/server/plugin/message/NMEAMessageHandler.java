package mw.server.plugin.message;

import mw.server.message.Message;
import mw.server.message.MessageHandler;

/**
 * @author thuss
 *
 * Message handler for creating NMEA Message
 */
public class NMEAMessageHandler implements MessageHandler {

	/**
	 * @see mw.server.message.MessageHandler#createMessage(int, String, byte[])
	 */
	public Message createMessage(int sender_id, String sender_name, byte[] content) {
		Message message = new NMEAMessage();
		message.setSenderId(sender_id);
		message.setSenderName(sender_name);
		message.setMessage(content);
		return message;
	}

}
