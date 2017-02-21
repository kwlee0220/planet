package etri.planet.transport;

import java.io.IOException;
import java.nio.ByteBuffer;

import planet.SystemException;
import planet.transport.Connection;


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public class OutgoingDataMessage extends DataMessage {
	public OutgoingDataMessage(int session, int part, boolean isFinal, int length,
								ByteBuffer buf) {
		super(new TransportHeader(TransportHeader.CODE_DATA, session));
		
		m_header.m_blockNum = part;
		m_header.m_final = isFinal ? (byte)1 : (byte)0;
		m_header.m_length = length;
		
		m_buffer = buf;
	}

	@Override
	public void encode(ByteBuffer buf) throws IOException {
		throw new SystemException("panic");
	}

	@Override
	public void handle(ConnectionImpl conn) throws Exception {
		throw new SystemException("panic");
	}

	@Override
	public void readPayload(Connection conn, ByteBuffer buf) throws IOException {
		throw new SystemException("panic");
	}

	@Override
	public void writePayload(ByteBuffer buf) throws IOException {
		throw new SystemException("panic");
	}
}
