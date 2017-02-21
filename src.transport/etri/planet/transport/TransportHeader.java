package etri.planet.transport;

import java.nio.ByteBuffer;

import planet.transport.ProtocolException;


/**
 * 
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public class TransportHeader {
	public static final int MAGIC = 0x970208;
	public static final byte VERSION_MAJOR = 0x01;
	public static final byte VERSION_MINOR= 0x00;
	public static final int SIZE = 20;
	
	public static final byte CODE_CONNECT = 0;
	public static final byte CODE_CONNECT_REPLY = 1;
	public static final byte CODE_HEARTBEAT = 2;
	public static final byte CODE_HEARTBEAT_ACK = 3;
	public static final byte CODE_DATA = 4;
	public static final byte CODE_DATA_CTRL = 5;
	
	public int m_magic;
	public int m_length;
	public int m_chId;			// 채널 식별자 (long 메시지를 위한)
	public int m_blockNum;		// 스트림내 블럭 순서 (0부터 시작, long 메시지가 아닌 경우는 -1 사용 가능)
	public byte m_final;		// 마지막 블럭 여부 (0: 중간, 1: 마지막)
	public byte m_verMajor;		// major 버전 번호
	public byte m_verMinor;		// minor 버전 번호
	public byte m_code;
	
	public TransportHeader(byte code, int chId) {
		m_magic = MAGIC;
		m_chId = chId;
		m_blockNum = 0;
		m_final = 1;
		m_verMajor = VERSION_MAJOR;
		m_verMinor = VERSION_MINOR;
		m_code = code;
	}
	
	public TransportHeader() { }
	
	public static TransportHeader read(ByteBuffer buf) throws ProtocolException {
		TransportHeader header = new TransportHeader();
		header.deserialize(buf);
		
		return header;
	}

	public void serialize(ByteBuffer buf) {
		buf.putInt(m_magic);
		buf.putInt(m_length);
		buf.putInt(m_chId);
		buf.putInt(m_blockNum);
		buf.put(m_final);
		buf.put(m_verMajor);
		buf.put(m_verMinor);
		buf.put(m_code);
	}

	public void deserialize(ByteBuffer buf) throws ProtocolException {
		m_magic = buf.getInt();
		if ( m_magic != MAGIC ) {
			throw new ProtocolException(String.format("Invalid magic number=0x%x", m_magic));
		}
		
		m_length = buf.getInt();
		m_chId = buf.getInt();
		m_blockNum = buf.getInt();
		m_final = buf.get();
		m_verMajor = buf.get();
		m_verMinor = buf.get();
		
		if ( m_verMajor != VERSION_MAJOR || m_verMinor != VERSION_MINOR ) {
			throw new ProtocolException("invalid version=" + m_verMajor + ":" + m_verMinor);
		}
		
		m_code = buf.get();
	}
	
	public String toString() {
		String magicStr = (m_magic != MAGIC) ? String.format("0x%x, ", m_magic) : "";
		String verStr = "";
		if ( m_verMajor != VERSION_MAJOR || m_verMinor != VERSION_MINOR ) {
			verStr = String.format("ver=%d:%d, ", m_verMajor, m_verMinor);
		}
		
		if ( m_chId >= 0 ) {
			return String.format("%s%sch=%d:%d:%d, code=%d, length=%d",
								magicStr, verStr, m_chId, m_blockNum, m_final, m_code, m_length);
		}
		else {
			return String.format("%s%scode=%d, length=%d", magicStr, verStr, m_code, m_length);
		}
	}
	
	public String toShortString() {
		String magicStr = (m_magic != MAGIC) ? String.format("0x%x, ", m_magic) : "";
		String verStr = "";
		if ( m_verMajor != VERSION_MAJOR || m_verMinor != VERSION_MINOR ) {
			verStr = String.format("ver=%d:%d, ", m_verMajor, m_verMinor);
		}
		
		if ( m_chId >= 0 ) {
			return String.format("%s%sch=%d:%d:%d, length=%d",
								magicStr, verStr, m_chId, m_blockNum, m_final, m_length);
		}
		else {
			return String.format("%s%slength=%d", magicStr, verStr, m_length);
		}
	}
}
