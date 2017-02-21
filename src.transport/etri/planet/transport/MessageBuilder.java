package etri.planet.transport;


import java.io.IOException;
import java.nio.ByteBuffer;

import planet.transport.ProtocolException;
import planet.transport.TransportManager;

import etri.planet.TransportLoggers;


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
class MessageBuilder {
	private static final int MIN_BUFFER_LEFT = TransportHeader.SIZE;

	private final ConnectionImpl m_conn;
	private volatile TransportHeader m_header = null;
	
	private final ByteBuffer m_producerBuffer;
	private final ByteBuffer m_consumerBuffer;
	
	MessageBuilder(ConnectionImpl conn) {
		m_conn = conn;
		
		m_producerBuffer = ByteBuffer.allocate(TransportManager.MAX_BLOCK_SIZE);
		m_consumerBuffer = m_producerBuffer.duplicate();
	}
	
	void fillBuffer() throws IOException {
		int nbytes = m_conn.m_sockChannel.read(m_producerBuffer);
		if ( nbytes <= 0 ) {
			throw new IOException("peer closed");
		}
		
		m_conn.setDirty();
		m_consumerBuffer.limit(m_producerBuffer.position());
		
//		s_logger.debug("**********[" + nbytes + "]: " + m_channel.m_msgBuilder);
	}
	
	TransportMessage build() throws IOException {
		if ( !checkFullMessageReceived() ) {
			return null;
		}
		
		//
		// 하나의 메시지 block이 모두 도착한 경우.
		//
		
//		System.out.println(PlanetInternalUtils.prefixString(m_buffer, 20));
//		if ( m_buffer.remaining() > 90 ) {
//			ByteBuffer tmp = m_buffer.slice();
//			tmp.position(70);
//			System.out.println(PlanetInternalUtils.prefixString(tmp, 20));
//		}
		
		TransportMessage msg;
		switch ( m_header.m_code ) {
			case TransportHeader.CODE_DATA:
				msg = new IncomingDataMessage(m_header);
				break;
			case TransportHeader.CODE_DATA_CTRL:
				msg = new DataControlMessage(m_header);
				break;
			case TransportHeader.CODE_HEARTBEAT:
				msg = new HeartbeatMessage(m_header);
				break;
			case TransportHeader.CODE_HEARTBEAT_ACK:
				msg = new HeartbeatAckMessage(m_header);
				break;
			case TransportHeader.CODE_CONNECT:
				msg = new ConnectMessage(m_header);
				break;
			case TransportHeader.CODE_CONNECT_REPLY:
				msg = new ConnectReplyMessage(m_header);
				break;
			default:
				throw new ProtocolException("unknown TransportMessage: code=" + m_header.m_code);
		}
		
		msg.readPayload(m_conn, m_consumerBuffer);
		if ( TransportLoggers.IO.isDebugEnabled() ) {
			TransportLoggers.IO.debug("rcvd: " + msg + ", conn=" + m_conn);
		}
		
		return msg;
	}
	
	int remaining() {
		return m_producerBuffer.position() - m_consumerBuffer.position();
	}
	
	void resetHeader() {
		m_header = null;
	}
	
	void compact() {
		int dataLeft = m_consumerBuffer.remaining();
		if ( dataLeft == 0 || m_producerBuffer.remaining() < MIN_BUFFER_LEFT ) {
			m_producerBuffer.position(m_consumerBuffer.remaining());
			m_consumerBuffer.compact().flip();
		}
	}
	
	private boolean checkFullMessageReceived() {
		if ( m_header == null ) {
			if ( m_consumerBuffer.remaining() < TransportHeader.SIZE ) {
				return false;
			}

			m_header = TransportHeader.read(m_consumerBuffer);
		}
		
		return ( m_consumerBuffer.remaining() >= (m_header.m_length - TransportHeader.SIZE) );
	}
	
	public String toString() {
		return String.format("buffer[%d..%d:%d], header[%x]",
							m_consumerBuffer.position(), m_producerBuffer.position(),
							m_producerBuffer.position() - m_consumerBuffer.position(),
							(m_header != null) ? System.identityHashCode(m_header) : 0);
	}
}
