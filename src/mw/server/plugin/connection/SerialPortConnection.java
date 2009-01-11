package mw.server.plugin.connection;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
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
 * and writing to a serial port. It is a single point to point
 * connection layer.
 */
public class SerialPortConnection implements SingleConnectionLayer {
	
	protected static Logger log = Logger.getLogger(SerialPortConnection.class);

	/**
	 * The serial port to read and write data 
	 */
	protected File serialport;

	/**
	 * The FileInputStream
	 */
	protected InputStream fis;

	/*
	 * The FileOutputStream
	 */
	protected OutputStream fos;

	protected Object jcp = null;

	/**
	 * The parameters for the connection
	 */
	protected String params;

	/**
	 * @see mw.server.connection.SingleConnectionLayer#openConnection()
	 */
	public void openConnection() throws IOException {
		// Set default port parameters
		String comm = "COM1";
		String baudrate = "4800";
		String bitsperchar = "8";
		String parity = "N";
		String stopbits = "1";

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
					if (param.equalsIgnoreCase("comm")
						&& strtok2.hasMoreTokens()) {
						comm = strtok2.nextToken();
					} else if (
						param.equalsIgnoreCase("baudrate")
							&& strtok2.hasMoreTokens()) {
						baudrate = strtok2.nextToken();
					} else if (
						param.equalsIgnoreCase("bitsperchar")
							&& strtok2.hasMoreTokens()) {
						bitsperchar = strtok2.nextToken();
					} else if (
						param.equalsIgnoreCase("parity")
							&& strtok2.hasMoreTokens()) {
						parity = strtok2.nextToken();
					} else if (
						param.equalsIgnoreCase("stopbits")
							&& strtok2.hasMoreTokens()) {
						stopbits = strtok2.nextToken();
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

		try {
			jcp = new JavaCommProxy(comm, baudrate);
			fis = ((JavaCommProxy)jcp).getInputStream();
			fos = ((JavaCommProxy)jcp).getOutputStream();
		} catch (NoClassDefFoundError e) {
			// Javacomm is not installed
		}

		// Otherwise just use the regular file api
		if (jcp == null) {
			serialport = new File(comm);
			if (!serialport.exists()) {
				throw new IOException("Serial port does not exist: " + comm);
			}

			// Create the input and output streams
			fis = new FileInputStream(serialport);
			fos = new FileOutputStream(serialport);
		}
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
		if (jcp != null) {
			((JavaCommProxy)jcp).close();
		} else {
			if (fis != null)
				fis.close();
			if (fos != null)
				fos.close();
		}
	}

	/**
	 * Test method which reads from the serial port on Com1 and prints
	 * output for a few seconds before terminating
	 * @param args Command line arguments are ignored
	 */
	public static void main(String[] args) {
		SerialPortConnection spc = null;
		try {
			log.info("Starting Serial Port Connection");
			spc = new SerialPortConnection();
			spc.setParameters("comm=COM1:baudrate=4800:bitsperchar=8:parity=N:stopbits=1");
			spc.openConnection();
			InputStream in = spc.getInputStream();

			ByteArrayOutputStream baos = new ByteArrayOutputStream(32);
			byte[] buffer = new byte[32];
			int nread = 0;

			int i = 0;
			while ((nread = in.read(buffer)) > -1 && i < 10000) {
				baos.write(buffer, 0, nread);
				if (nread > 0) {
					System.out.print(baos.toString());
					baos.reset();
				}
				i++;
			}
			in.close();
			log.info("");
			log.info("Done");
		} catch (Exception e) {
			log.warn(e);
		} finally {
			try {
				if (spc != null)
					spc.closeConnection();
			} catch (Exception e) {
			}
		}
	}
}
