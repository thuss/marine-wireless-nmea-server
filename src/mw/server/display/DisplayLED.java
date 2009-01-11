/*
 * Created on Apr 4, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package mw.server.display;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Map;

import org.apache.log4j.Logger;

import mw.server.configuration.Configuration;

/**
 * @author thuss
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class DisplayLED implements Runnable {
	/**
	 * Logging class
	 */
	protected static Logger log = Logger.getLogger(DisplayLED.class);
	
	/**
	 * Proc file to control the LED light
	 */
	protected String filename = "/proc/errorlight";
	
	/**
	 * The file to write to to turn the led on or off
	 */
	protected File writeFile = null;
	
	/**
	 * The display LED singleton
	 */
	protected static DisplayLED singleton = new DisplayLED();
	
	/**
	 * The unique identifier of the last sender
	 */
	protected String lastSender = new String();
	
	/**
	 * The current state of the led. True is on.
	 */
	protected boolean curState = false;
	
	/** 
	 * Whether we have an LED to write to
	 */
	protected boolean haveLED = false;
	
	/**
	 * Protected constructor because it's a singleton
	 */
	protected DisplayLED() {
		// Get the configuration to see if this is enabled
		Configuration config = Configuration.getInstance();
		Map features = config.getFeatures();
		if (features.containsKey("errorled")) {
			String options = (String)features.get("errorled");
			if (options != null) {
				filename = options;
			}
			writeFile = new File(filename);
			if (writeFile.exists()) {
				haveLED = true;
			} else {
				log.warn("LED "+filename+" does not exist");
			}
		}
	}
	
	/**
	 * Get an instance of the DisplayLED class
	 * @return The display LED singleton
	 */
	public static DisplayLED getInstance() {
		return singleton;
	}
	
	/**
	 * Set the error light on or off
	 * @param sender Unique identifier such as mediator name
	 * @param state True is on and false is off
	 */
	public synchronized void setErrorLight(String sender, boolean state) {
		if (haveLED) {
			if (state == curState) {
				// If sender is setting the state the same simply record 
				// who the last sender was
				lastSender = sender;
			} else if ((!state && lastSender.equals(sender)) || state) {
				// If we're turning it off && sender is the one who turned it on
				// || if we're turning it on
				setLED(state);
				lastSender = sender;
				curState = state;
			}
		}
	}
	
	
	
	/** 
	 * Turn the LED on or off. This method should only be called by 
	 * the setErrorLight method, hence, it's protected.
	 * @param state True turns the light on, false off.
	 */
	protected void setLED(boolean state) {
		// Initialize the value to write
		byte writeIt = '0';
		if (state) writeIt = '1';
		try {
			// Open the file for writing and write the state
			FileOutputStream fos = new FileOutputStream(writeFile);
			fos.write(writeIt);
			fos.close();
		} catch (Exception e) {
			log.warn("Could not update LED state by writing to "+filename);
		}
	}
	
	/**
	 * Blink the led 3 times
	 */
	public void run() {
		int sleeptime = 300;
		int count = 3;
		try {
			for (int i = 0; i < count; i++) {
				if (i != 0) {
					Thread.sleep(sleeptime);
				}
				setErrorLight("ME", true);
				Thread.sleep(sleeptime);
				setErrorLight("ME", false);
			}
		} catch (Exception e) {
			
		}
	}
}
