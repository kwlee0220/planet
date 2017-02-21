package etri.planet.transport;

import java.nio.ByteBuffer;



/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public abstract class DataMessage extends TransportMessage {
	public ByteBuffer m_buffer;
	
	public DataMessage(int session, int part, boolean isFinal, int payloadLength, ByteBuffer buf) {
		super(new TransportHeader(TransportHeader.CODE_DATA, session));
		
		m_header.m_blockNum = part;
		m_header.m_final = isFinal ? (byte)1 : (byte)0;
		m_header.m_length = payloadLength + TransportHeader.SIZE;
		m_buffer = buf;
	}
	
	public DataMessage(TransportHeader header) {
		super(header);
	}
	
	public DataMessage(String key) {
		super(new TransportHeader(TransportHeader.CODE_DATA, -1));
	}
	
	public String toString() {
		return "DATA[" + m_header.toShortString() + "]";
	}
}
