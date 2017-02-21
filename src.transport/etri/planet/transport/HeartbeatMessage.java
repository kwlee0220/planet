package etri.planet.transport;


import java.io.IOException;
import java.nio.ByteBuffer;

import planet.transport.Connection;


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
class HeartbeatMessage extends TransportMessage {
	HeartbeatMessage(TransportHeader header) {
		super(header);
	}
	
	HeartbeatMessage() {
		super(new TransportHeader(TransportHeader.CODE_HEARTBEAT, -1));
	}

	@Override
	public void handle(ConnectionImpl conn) throws Exception {
		conn.write(conn.m_transport.m_hbAckBytes);
	}

	@Override
	public void readPayload(Connection conn, ByteBuffer buf) throws IOException {
	}

	@Override
	public void writePayload(ByteBuffer buf) throws IOException {
	}
	
	public String toString() {
		return "Heartbeat[" + m_header + "]";
	}
}
