package etri.planet;

import java.io.IOException;

import planet.PlanetMessage;
import planet.transport.TransportManager;

import etri.planet.transport.TransportHeader;



/**
 * 
 * @author Kang-Woo Lee
 */
public abstract class AbstractPlanetMessage implements PlanetMessage {
	public static final int MAX_DATA_SIZE = TransportManager.MAX_BLOCK_SIZE
											- TransportHeader.SIZE - PlanetHeader.SIZE;
	
	public abstract void readPayload(PlanetReader reader) throws IOException;
	public abstract void writePayload(PlanetWriter writer) throws IOException;
	public abstract void onReceived(PlanetSessionImpl session) throws Exception;
	public abstract void onErrorReadingPayload(PlanetSessionImpl session, Throwable cause);
	
	public PlanetHeader m_header;
	
	protected AbstractPlanetMessage(PlanetHeader header) {
		m_header = header;
	}
	
	public void write(PlanetWriter output) throws IOException {
		m_header.serialize(output);
		writePayload(output);
	}
}