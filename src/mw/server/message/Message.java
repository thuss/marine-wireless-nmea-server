package mw.server.message;

/**
 * @author thuss
 *
 * The Message abstract base class includes methods any message
 * implementation should include. Specific message implementations
 * may require additional methods. For example NMEA may require
 * a getMessageType method.
 */
public abstract class Message {
	
//	protected boolean delivered = false;

	/**
	 * The message
	 */
	protected byte[] message;

	/**
	 * The unique id of the mediator sending the message
	 */
	protected int senderId;

	/**
	 * The unique name of the mediator sending the message
	 */
	protected String senderName;
	
	/**
	 * Method to determine if this message can be skipped 
	 * when we need to drop excess messages or if it's
	 * mandatory
	 * @return If this message MUST be delivered
	 */
	public boolean isMandatory() {
		if (message.length > 7 &&
			message[3] == 'A' && message[4] == 'P' && message[5] == 'B') {
				return true;
		}
		return false;
	}
	
	/**
	 * Returns the message.
	 * @return byte[]
	 */
	public byte[] getMessage() {
		return message;
	}

	/**
	 * Sets the message.
	 * @param message The message to set
	 */
	public void setMessage(byte[] message) {
		this.message = message;
	}

	/**
	 * Returns the senderId.
	 * @return int
	 */
	public int getSenderId() {
		return senderId;
	}

	/**
	 * Returns the senderName.
	 * @return int
	 */
	public String getSenderName() {
		return senderName;
	}

	/**
	 * Sets the senderId.
	 * @param senderId The senderId to set
	 */
	public void setSenderId(int senderId) {
		this.senderId = senderId;
	}

	/**
	 * Sets the senderName.
	 * @param senderName The senderName to set
	 */
	public void setSenderName(String senderName) {
		this.senderName = senderName;
	}
	
//	public void setDelivered(boolean del) {
//		if (delivered) {
//			System.out.println("Already delivered: " + new String(message));
//		}
//		delivered = true;
//	}
//	
//	protected void finalize() {
//		if (!delivered) {
//			System.out.println("Undelivered: " + new String(message));
//		}
//	}
	
	/**
	 * We override this method so that you can do List.contains
	 * to see if the same message is already in a list
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		// Check if it's the same memory location
		if (obj == this) {
			return true;
		}
		
		// If it an instance of the same object
		if (obj instanceof Message) {
			byte[] message2 = ((Message)obj).getMessage();
			// If both byte arrays are the same length
			if (message.length == message2.length) {
				// Test each byte in the array for equality
				for (int i = message.length - 1; i > -1; i--) {
					if (message[i] != message2[i]) {
						return false;
					}
				}
			} else {
				return false;
			}
		} else {
			return false;
		}
		return true;
	}
}
