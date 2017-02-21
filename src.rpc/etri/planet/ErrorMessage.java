package etri.planet;

import java.io.IOException;



/**
 * 
 * @author Kang-Woo Lee
 */
public class ErrorMessage extends ReturnMessage {
	private Throwable m_cause;
	
	public ErrorMessage(int reqId, Throwable e) {
		super(new PlanetHeader(MSG_ERROR, reqId));
		
		m_cause = e;
	}
	
	public ErrorMessage(PlanetHeader header) {
		super(header);
	}
	
	public Throwable getCause() {
		return m_cause;
	}

	public void readPayload(PlanetReader reader) throws IOException {
		m_cause = reader.readException();
	}

	public void writePayload(PlanetWriter writer) throws IOException {
		writer.writeException(m_cause);
	}
	
	public String toString() {
		if ( getReadError() == null ) {
			return "Error[cause=" + m_cause + "]";
		}
		else {
			return "Error[corrupted:cause=" + getReadError() + "]";
		}
	}
}