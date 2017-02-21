package etri.planet.transport;

import java.io.IOException;
import java.nio.ByteBuffer;

import planet.transport.Connection;


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
class ConnectReplyMessage extends TransportMessage {
	public byte m_code;
	public String m_details;
	
	ConnectReplyMessage(TransportHeader header) {
		super(header);
	}
	
	ConnectReplyMessage(String details) {
		super(new TransportHeader(TransportHeader.CODE_CONNECT_REPLY, -1));
		
		m_code = 0;
		m_details = details;
	}

	@Override
	public void handle(ConnectionImpl conn) throws Exception {
		conn.notifyConnectReplied(m_code, m_details);
	}

	@Override
	public void readPayload(Connection conn, ByteBuffer buf) throws IOException {
		m_code = buf.get();
		m_details = readString(buf);
	}

	@Override
	public void writePayload(ByteBuffer buf) throws IOException {
		buf.put(m_code);
		writeString(m_details, buf);
	}
	
	public String toString() {
		return "ConnectAck[" + m_header + ", code=" + m_code + ", details=" + m_details + "]";
	}
}
