package etri.planet.transport;

import java.io.IOException;
import java.nio.ByteBuffer;

import planet.transport.Connection;


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
class ConnectMessage extends TransportMessage {
	public String m_key;
	
	ConnectMessage(TransportHeader header) {
		super(header);
	}
	
	ConnectMessage(String key) {
		super(new TransportHeader(TransportHeader.CODE_CONNECT, -1));
		
		m_key = key;
	}

	@Override
	public void handle(final ConnectionImpl conn) throws Exception {
		conn.accept(m_key);
	}

	@Override
	public void readPayload(Connection conn, ByteBuffer buf) throws IOException {
		m_key = readString(buf);
	}

	@Override
	public void writePayload(ByteBuffer buf) throws IOException {
		writeString(m_key, buf);
	}
	
	public String toString() {
		return "Connect[" + m_header + ", id=" + m_key + "]";
	}
}
