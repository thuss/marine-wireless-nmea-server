/*
 * Created on Oct 6, 2003
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package mw.server.plugin.connection;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;

import javax.comm.CommPortIdentifier;
import javax.comm.SerialPort;

/**
 * @author thuss
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class JavaCommProxy {
	
	protected SerialPort serialPort = null;

	protected InputStream is = null;

	protected OutputStream os = null;

	public JavaCommProxy(String comm, String baudrate) throws IOException {
		boolean haveport = false;
		try {
			CommPortIdentifier portId;
			Enumeration portList = CommPortIdentifier.getPortIdentifiers();
			while (portList.hasMoreElements() && !haveport) {
				portId = (CommPortIdentifier) portList.nextElement();
				if (portId.getPortType() == CommPortIdentifier.PORT_SERIAL) {
					if (portId.getName().equals(comm)) {
						serialPort = (SerialPort) portId.open(comm, 2000);
						is = serialPort.getInputStream();
						os = serialPort.getOutputStream();
						serialPort.setSerialPortParams(new Integer(baudrate).intValue(), 
								SerialPort.DATABITS_8, 
								SerialPort.STOPBITS_1, 
								SerialPort.PARITY_NONE);
						serialPort.setFlowControlMode(SerialPort.FLOWCONTROL_NONE);
						haveport = true;
					}
				}
			}
		} catch (Exception e) {
			throw new IOException(e.getMessage());
		}
		
		if (!haveport) {
			throw new IOException("JavaComm: Could not find serial port: "+comm);
		}
	}

	public InputStream getInputStream() {
		return is;
	}

	public OutputStream getOutputStream() {
		return os;
	}

	public void close() {
		try {
			if (is != null)
				is.close();
			if (os != null)
				os.close();
			if (serialPort != null)
				serialPort.close();
		} catch (IOException e) {
		}
	}
}
