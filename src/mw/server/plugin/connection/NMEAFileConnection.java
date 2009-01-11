package mw.server.plugin.connection;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;

import mw.server.connection.SingleConnectionLayer;

/**
 * @author thuss
 *
 * This is a ConnectionLayer implementation designed for reading
 * from a file. It is a single point to point
 * connection layer.
 */
public class NMEAFileConnection implements SingleConnectionLayer {

	/**
	 * Null output stream since we don't want to write to dummy file
	 */
	class NullOutput extends java.io.OutputStream {
		public void write(int b) throws java.io.IOException {
			// Don't do anything with this data
		}
	}

	/**
	 * Delayed input stream to approximate NMEA timing
	 */
	class DelayedInput extends InputStream {

		protected long tstamp = System.currentTimeMillis();

		protected long delay = 0;
		
		protected FileInputStream actualstream = null;
		
		protected File filename = null;

		public DelayedInput(File filename, int delay, boolean loop)
			throws FileNotFoundException {
			actualstream = new FileInputStream(filename);
			this.filename = filename;
			this.delay = delay;
		}

		public int available() throws IOException {
			long diff = System.currentTimeMillis() - tstamp;
			if (diff >= delay) {
				return 1;
			} else {
				return 0;
			}
		}

		public int read() throws IOException {
			int abyte = actualstream.read();
			if (abyte == '\n') {
				long diff = System.currentTimeMillis() - tstamp;
				if (diff >= delay) {
					tstamp = System.currentTimeMillis();
				}
			} else if (abyte == -1 && loop) {
				actualstream.close();
				actualstream = new FileInputStream(filename);
				abyte = '\n';
			}
			return abyte;
		}
		
		public void close() throws IOException {
			actualstream.close();
		}
	}
	
	protected Logger log = Logger.getLogger(NMEAFileConnection.class);

	/**
	 * The nmea file to read from 
	 */
	protected File nmeafile;

	/**
	 * The loop boolean
	 */
	protected boolean loop = false;

	/**
	 * The FileInputStream
	 */
	protected InputStream fis;

	/**
	 * The FileOutputStream
	 */
	protected OutputStream fos;

	/**
	 * The parameters for the connection
	 */
	protected String params;

	/**
	 * @see mw.server.connection.SingleConnectionLayer#openConnection()
	 */
	public void openConnection() throws IOException {
		// Set default file parameters
		String filename = "nmea.log";
		int delay = 150;

		// Get the parameters
		if (params != null) {
			StringTokenizer strtok = new StringTokenizer(params, ":");
			StringTokenizer strtok2 = null;
			String badparam = null;
			while (strtok.hasMoreTokens()) {
				String nexttok = strtok.nextToken();
				strtok2 = new StringTokenizer(nexttok, "=");
				if (strtok2.hasMoreTokens()) {
					String param = strtok2.nextToken();
					if (param.equalsIgnoreCase("file")
						&& strtok2.hasMoreTokens()) {
						filename = strtok2.nextToken();
					} else if (
						param.equalsIgnoreCase("loop")
							&& strtok2.hasMoreTokens()) {
						if (strtok2.nextToken().equalsIgnoreCase("true")) {
							loop = true;
						}
					} else if (
						param.equalsIgnoreCase("delay")
							&& strtok2.hasMoreTokens()) {
						try {
							delay = new Integer(strtok2.nextToken()).intValue();
						} catch (Exception e) {
							badparam = nexttok;
						}
					} else {
						badparam = nexttok;
					}
				} else {
					badparam = nexttok;
				}

				// Show warning message
				if (badparam != null) {
					String msg =
						"WARNING: "
							+ this.getClass().getName()
							+ " ignoring unrecognized parameter "
							+ badparam;
					log.warn(msg);
					badparam = null;
				}
			}
		}

		nmeafile = new File(filename);
		if (!nmeafile.exists()) {
			throw new IOException("File does not exist: " + filename);
		}

		// Create the input and output streams
		fis = new DelayedInput(nmeafile, delay, loop);
		fos = new NullOutput();
	}

	/**
	 * @see mw.server.connection.ConnectionLayer#setParameters(String)
	 * 
	 * Example serial port parameter string would be:
	 * comm=1:baudrate=4800:bitsperchar=8:parity=none:stopbits=1
	 */
	public void setParameters(String params) {
		this.params = params;
	}

	/**
	 * @see mw.server.connection.SingleConnectionLayer#getInputStream()
	 */
	public InputStream getInputStream() {
		return fis;
	}

	/**
	 * @see mw.server.connection.SingleConnectionLayer#getOutputStream()
	 */
	public OutputStream getOutputStream() {
		return fos;
	}

	/**
	 * @see mw.server.connection.ConnectionLayer#closeConnection()
	 */
	public void closeConnection() throws IOException {
		if (fis != null)
			fis.close();
		if (fos != null)
			fos.close();
	}
}
