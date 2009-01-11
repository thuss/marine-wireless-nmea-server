package mw.server.configuration;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mw.server.connection.ConnectionLayer;
import mw.server.director.MessageDirector;
import mw.server.mediator.Mediator;
import mw.server.message.MessageHandler;
import mw.server.protocol.ProtocolHandler;

import org.kxml.kdom.Document;
import org.kxml.kdom.Element;
import org.kxml.parser.XmlParser;

/**
 * @author thuss
 *
 * Singleton class to get the system configuration and instantiate mediators
 * based on that configuration. The configuration file is specified using XML
 */
public class Configuration {

	/**
	 * This is where we store the singleton reference
	 */
	protected static Configuration configsingleton = new Configuration();

	/**
	 * The reference to the configuration file
	 */
	protected File configfile = null;

	/**
	 * List of mediators built from the configuration file
	 */
	protected List mediators = null;

	/**
	 * HashMap of available mediators keyed by name
	 */
	protected HashMap availablemediators;

	/**
	 * HashMap of available connections keyed by name
	 */
	protected HashMap connectionlayers;

	/**
	 * HashMap of available messagehandlers keyed by name
	 */
	protected HashMap messagehandlers;
	
	/**
	 * HashMap of available features keyed by feature name
	 */
	protected HashMap features;

	/**
	 * HashMap of available protocolhandlers keyed by name
	 */
	protected HashMap protocolhandlers;

	/**
	 * Protected constructor since only the configuration class
	 * can instantiate itself. This is because it's a singleton.
	 */
	protected Configuration() {
	}

	/**
	 * Get an instance of the Configuration class
	 */
	public static Configuration getInstance() {
		return configsingleton;
	}

	/** 
	 * Parse the configuration file and construct the necessary
	 * internal datastructures.
	 * 
	 * @param filename The filename of the configuration file
	 * @throws FileNotFoundException Could not find configuration file
	 * @throws IOException IO Error while reading from configuration file
	 * @throws ClassNotFoundException Class specified in configuration file not found
	 * @throws IllegalAccessException Not allowed to access the specified class
	 * @throws InstantiationException Error instantiating the specified class
	 */
	public void parseConfiguration(String filename)
		throws
			FileNotFoundException,
			IOException,
			ClassNotFoundException,
			IllegalAccessException,
			InstantiationException {
		configfile = null;
		mediators = new ArrayList();

		// Try to open the configuration file
		configfile = new File(filename);
		if (!configfile.exists()) {
			throw new FileNotFoundException(
				"Configuration file not found: " + configfile);
		}

		// Try to read it
		FileReader reader = null;
		try {
			reader = new FileReader(configfile);
			// Now do the real work 
			parseXML(reader);
		} finally {
			if (reader != null)
				reader.close();
		}
	}

	/**
	 * Method parseXML takes a Reader to get XML configuration data from
	 * and then builds internal data structures based on the configuration
	 * 
	 * @param reader The Reader to get the XML configuration data from
	 * @throws IOException IO Eror while reading from the configuration file
	 * @throws ClassNotFoundException Class specified in configuration file not found
	 * @throws IllegalAccessException Not allowed to access the specified class
	 * @throws InstantiationException Error instantiating the specified class
	 */
	protected void parseXML(Reader reader)
		throws
			IOException,
			ClassNotFoundException,
			IllegalAccessException,
			InstantiationException {
		XmlParser parser = new XmlParser(reader);
		Document doc = new Document();
		doc.parse(parser);
		Element root = doc.getRootElement();
		for (int i = 0; i < root.getChildCount(); i++) {
			Element elem = root.getElement(i);
			if (elem == null)
				continue;
			String name = elem.getName();
			if (name.equals("connectionlayers")) {
				connectionlayers = buildImplMap(elem);
			} else if (name.equals("messagehandlers")) {
				messagehandlers = buildImplMap(elem);
			} else if (name.equals("protocolhandlers")) {
				protocolhandlers = buildImplMap(elem);
			} else if (name.equals("mediator")) {
				mediators.add(buildMediator(elem));
			} else if (name.equals("mediators")) {
				availablemediators = buildImplMap(elem);
			} else if (name.equals("features")) {
				features = buildFeatureMap(elem);
			}
		}
	}

	/**
	 * Method buildMediator instantiates a mediator based on
	 * an XML specification and returns that mediator.
	 * 
	 * @param elem The mediator element
	 * @return Mediator The instantiated mediator based on the element
	 * @throws IllegalAccessException Not allowed to access the specified class
	 * @throws InstantiationException Error instantiating the specified class
	 */
	protected Mediator buildMediator(Element elem)
		throws IllegalAccessException, InstantiationException {
		Mediator med = null;
		ConnectionLayer cl = null;
		ProtocolHandler ph = null;
		MessageHandler mh = null;
		
		// Get the director since mediators need to contact the director
		MessageDirector director = MessageDirector.getInstance();
		
		// Make sure we have the necessary attributes
		String name = elem.getValue("name");
		String type = elem.getValue("type");
		String restart = elem.getValue("restart");
		if (name != null && type != null) {
			// Make sure the xml attributes are correct
			Class medclass = (Class) availablemediators.get(type);
			if (medclass == null) {
				throw new RuntimeException(
					"No mediator of type "
						+ type
						+ " found in configuration file "
						+ configfile);
			}

			// Get mediator child elements and instantiate handlers
			Element child = null;
			for (int i = 0; i < elem.getChildCount(); i++) {
				child = elem.getElement(i);
				if (child == null)
					continue;
				if (child.getName().equals("connectionlayer")) {
					cl = (ConnectionLayer) instantiateHandler(child);
				} else if (child.getName().equals("messagehandler")) {
					mh = (MessageHandler) instantiateHandler(child);
				} else if (child.getName().equals("protocolhandler")) {
					ph = (ProtocolHandler) instantiateHandler(child);
				}
			}

			if (cl == null || ph == null || mh == null) {
				throw new RuntimeException(
					"Mediators require a connectionlayer, "
						+ "protocolhandler, and messagehandler but not all are present in "
						+ elem);
			}

			// Instantiate and return the mediator
			med = (Mediator) medclass.newInstance();
			med.setParameters(
				director.getNewMediatorId(),
				name,
				cl,
				ph,
				mh,
				director);
			if (restart != null && restart.equalsIgnoreCase("true")) {
				med.setRestart(true);
			}
		} else {
			throw new RuntimeException(
				"Attributes name and type are "
					+ "requiered for: "
					+ elem.toString());
		}
		return med;
	}

	/**
	 * Method instantiateHandler will instantiat a connectionlayer,
	 * protocolhandler, or messagehandler based on an XML specification.
	 * 
	 * @param elem The handler element
	 * @return Object Either a connectionlayer, protocolhandler, or messagehandler
	 * @throws IllegalAccessException Not allowed to access the specified class
	 * @throws InstantiationException Unable to instantiate a new instance
	 */
	protected Object instantiateHandler(Element elem)
		throws IllegalAccessException, InstantiationException {
		Object handler = null;
		String type = elem.getValue("type");
		Class handlerclass = null;

		// Make sure they specific a type
		if (type == null) {
			throw new RuntimeException(
				"Attribute type is required for: "
					+ elem
					+ " in configuration file "
					+ configfile);
		}

		// Now handle the element
		if (elem.getName().equals("connectionlayer")) {
			handlerclass = (Class) connectionlayers.get(type);
			if (handlerclass == null) {
				throw new RuntimeException(
					"No connectionlayer found of type "
						+ type
						+ " in configuration file "
						+ configfile);
			}
			ConnectionLayer cl = (ConnectionLayer) handlerclass.newInstance();
			// Since connectionlayers can have parameters we need to check
			// for those as well. If no params are present it will use the
			// default parameter settings of the connectionlayer.
			for (int i = 0; i < elem.getChildCount(); i++) {
				Element child = elem.getElement(i);
				if (child == null)
					continue;
				if (child.getName().equals("parameters")
					&& child.getChildCount() == 1) {
					cl.setParameters(child.getChild(0).toString());
				} else {
					throw new RuntimeException(
						"Unexpected or invalid element "
							+ child
							+ " in configuration file "
							+ configfile);
				}
			}
			handler = cl;
		} else if (elem.getName().equals("messagehandler")) {
			handlerclass = (Class) messagehandlers.get(type);
			if (handlerclass == null) {
				throw new RuntimeException(
					"No messagehandler found of type "
						+ type
						+ " in configuration file "
						+ configfile);
			}
			handler = handlerclass.newInstance();
		} else if (elem.getName().equals("protocolhandler")) {
			handlerclass = (Class) protocolhandlers.get(type);
			if (handlerclass == null) {
				throw new RuntimeException(
					"No protocolhandler found of type "
						+ type
						+ " in configuration file "
						+ configfile);
			}
			handler = handlerclass.newInstance();
		} else {
			throw new RuntimeException(
				"Unknown handler "
					+ elem
					+ " in configuration file "
					+ configfile);
		}

		return handler;
	}
	
	protected HashMap buildFeatureMap(Element elem) {
		HashMap map = new HashMap();
		Element child = null;
		String name = null;
		String options = null;
		String enabled = null;
		for (int i = 0; i < elem.getChildCount(); i++) {
			child = (Element) elem.getElement(i);
			if (child == null)
				continue;
			if (child.getName().equals("feature")) {
				name = child.getValue("name");
				options = child.getValue("options");
				enabled = child.getValue("enabled");
				if (name != null && enabled != null) {
					if (enabled.equalsIgnoreCase("true")) {
						map.put(name, options);
					}
				} else {
					throw new RuntimeException(
							"Attributes name and enabled are required "
							+ "for: "
							+ child.toString());
				}
			}
		}
		return map;
	}

	/**
	 * Method buildImplMap takes a group of impl elements from
	 * the configuration file (say connectionlayers) and builds
	 * a hashmap of name -> class mappings.
	 * 
	 * @param elem The parent of the impl elemnts
	 * @return HashMap A hashmap of name -> class mappings
	 * @throws ClassNotFoundException Could not find the specific class
	 */
	protected HashMap buildImplMap(Element elem)
		throws ClassNotFoundException {
		HashMap map = new HashMap();
		Element child = null;
		String name = null;
		String classname = null;
		for (int i = 0; i < elem.getChildCount(); i++) {
			child = (Element) elem.getElement(i);
			if (child == null)
				continue;
			if (child.getName().equals("impl")) {
				name = child.getValue("name");
				classname = child.getValue("class");
				if (name != null && classname != null) {
					map.put(name, Class.forName(classname));
				} else {
					throw new RuntimeException(
						"Attributes name and class are "
							+ "requiered for: "
							+ child.toString());
				}
			}
		}
		return map;
	}

	/**
	 * Returns the List of Mediator objects based on the
	 * configuration file.
	 * 
	 * @return List of Mediator objects
	 */
	public List getMediators() {
		return mediators;
	}
	
	/**
	 * Returns the Map of features that are enabled. The
	 * key of the map is the feature name and the value
	 * is any options.
	 * 
	 * @return Map of enabled features 
	 */
	public Map getFeatures() {
		return features;
	}
	
	/**
	 * Returns the connnectionlayers.
	 * @return Class[]
	 */
	public HashMap getConnectionlayers() {
		return connectionlayers;
	}

	/**
	 * Returns the messagehandlers.
	 * @return Class[]
	 */
	public HashMap getMessagehandlers() {
		return messagehandlers;
	}

	/**
	 * Returns the protocolhandlers.
	 * @return Class[]
	 */
	public HashMap getProtocolhandlers() {
		return protocolhandlers;
	}

}
