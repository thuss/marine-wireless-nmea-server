package mw.server;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

import mw.server.configuration.Configuration;
import mw.server.connection.ConnectionLayer;
import mw.server.connection.MultiConnectionLayer;
import mw.server.connection.SingleConnectionLayer;
import mw.server.director.MessageDirector;
import mw.server.mediator.Mediator;

/**
 * @author thuss
 *
 * This is the Marine Wireless Server main class used to start the server.
 */
public class Server {
	
	protected static Logger log = Logger.getLogger(Server.class);

	/**
	 * Name of the Server
	 */
	protected static final String product = "Marine Wireless Navigation Server";

	
	/**
	 * Method parse to parse the command line arguments into a HashMap
	 * and removes the - before the key. For example -f filename would
	 * become f -> filename in the HashMap.
	 * @param args The aray of arguments
	 * @return HashMap 
	 */
	protected static HashMap parse(String[] args) {
		HashMap argMap = new HashMap();

		if ((args == null) || (args.length == 0)) {
			return argMap;
		}

		for (int i = 0; i < args.length; i++) {
			String arg = args[i];

			if (arg.startsWith("-")) {
				if (i == (args.length - 1)) {
					argMap.put(arg.substring(1, arg.length()), null);
					continue;
				}

				String value = args[i + 1];

				if (value.startsWith("-")) {
					argMap.put(arg.substring(1, arg.length()), null);
				} else {
					argMap.put(arg.substring(1, arg.length()), value);
				}
			}
		}

		return argMap;
	}

	/**
	 * Print the help out to standard output
	 */
	protected static void showHelp() {
		log.info("Usage: java mw.server.Server -f configurationfile");
	}
	
	protected static void testConnectionLayer(ConnectionLayer cl) throws IOException {
		if (cl instanceof SingleConnectionLayer) {
			((SingleConnectionLayer)cl).openConnection();			
			cl.closeConnection();
		} else if (cl instanceof MultiConnectionLayer) {
			((MultiConnectionLayer)cl).openConnection();			
			cl.closeConnection();
		} else {
			throw new RuntimeException(cl.getClass().getName() +
				" is not a known connectionlayer type");
		}		
	}

	/**
	 * Run the server
	 */
	public static void main(String[] args) {
		try {
			// Initialize Log4j
			BasicConfigurator.configure();
			
			// Intro message
			log.info(product);
			log.info("Server initializing");

			// Parse command line arguments
			HashMap argmap = parse(args);
			String configfile = null;
			if ((configfile = (String) argmap.get("f")) == null) {
				showHelp();
				return;
			}

			// Read the configuration file to get the mediators
			Configuration config = Configuration.getInstance();
			config.parseConfiguration(configfile);
			List mediators = config.getMediators();
			config = null;
			configfile = null;

			// Get the director
			MessageDirector director = MessageDirector.getInstance();

			// Start each of the mediators
			Mediator med = null;
			for (int i = 0; i < mediators.size(); i++) {
				med = (Mediator) mediators.get(i);
				testConnectionLayer(med.getConnectionlayer());
				new Thread(med).start();
			}

			// Sleep to allow mediators to start
			Thread.sleep(300);
			director.run();
		} catch (Exception e) {
			log.warn(e.getMessage());
		}
	}
}
