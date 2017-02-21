package etri.planet;

import java.io.IOException;

import planet.transport.ProtocolException;


/**
 * 
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public class PlanetHeader {
	public static final short VERSION_MAJOR = 0x01;
	public static final short VERSION_MINOR = 0x02;
	public static final int SIZE = 8 + 4;
	private static final byte UNUSED = 0;

	public volatile int m_reqId;
	public volatile byte m_verMajor = VERSION_MAJOR;
	public volatile byte m_verMinor = VERSION_MINOR;
	public volatile byte m_code;
	
	public PlanetHeader(byte code, int reqId) {
		m_reqId = reqId;
		m_code = code;
	}
	
	private PlanetHeader() { }
	
	public static PlanetHeader deserialize(PlanetReader input) throws IOException {
		PlanetHeader header = new PlanetHeader();
		header._deserialize(input);
		
		return header;
	}

	public void serialize(PlanetWriter output) throws IOException {
		output.writeInt(0);
		output.writeInt(m_reqId);
		output.writeByte(m_verMajor);
		output.writeByte(m_verMinor);
		output.writeByte(m_code);
		output.writeByte(UNUSED);
	}

	private void _deserialize(PlanetReader input) throws IOException {
		input.readInt();
		m_reqId = input.readInt();
		
		m_verMajor = input.readByte();
		m_verMinor = input.readByte();
		if ( m_verMajor != VERSION_MAJOR || m_verMinor != VERSION_MINOR ) {
			throw new ProtocolException("version mismatch=" + this);
		}
		
		m_code = input.readByte();
		input.readByte();
	}
	
	public String toString() {
		return String.format("id=%d, version=%d.%d code=%x", m_reqId,
							m_verMajor, m_verMinor, m_code);
	}
}
