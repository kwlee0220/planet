package etri.planet.transport;

import java.io.IOException;
import java.nio.ByteBuffer;

import planet.transport.Connection;


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
class HeartbeatAckMessage extends TransportMessage {
	
	HeartbeatAckMessage(TransportHeader header) {
		super(header);
	}
	
	HeartbeatAckMessage() {
		super(new TransportHeader(TransportHeader.CODE_HEARTBEAT_ACK, -1));
	}

	@Override
	public void handle(ConnectionImpl conn) throws Exception {
	}

	@Override
	public void readPayload(Connection conn, ByteBuffer buf) throws IOException {
	}

	@Override
	public void writePayload(ByteBuffer buf) throws IOException {
	}
	
	public String toString() {
		return "HeartbeatAck[" + m_header + "]";
	}
}
