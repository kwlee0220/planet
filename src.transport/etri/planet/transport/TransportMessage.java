package etri.planet.transport;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import planet.transport.Connection;



/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public abstract class TransportMessage {
	public TransportHeader m_header;
	
	public abstract void readPayload(Connection conn, ByteBuffer buf) throws IOException;
	public abstract void writePayload(ByteBuffer buf) throws IOException;
	public abstract void handle(ConnectionImpl conn) throws Exception;
	
	public TransportMessage(TransportHeader header) {
		m_header = header;
	}
	
	protected TransportMessage() { }

	public void encode(ByteBuffer buf) throws IOException {
		ByteBuffer hdrBuf = buf.duplicate();
		buf.position(buf.position() + TransportHeader.SIZE);
		writePayload(buf);
		
		m_header.m_length = buf.position();
		m_header.serialize(hdrBuf);
	}
	
	public String toString() {
		return "[" + m_header + "]";
	}
	
	private static final String EMPTY_STRING = "";
	protected static String readString(ByteBuffer buf) {
		int length = buf.getInt();
		if ( length < 0 ) {
			return null;
		}
		else if ( length == 0 ) {
			return EMPTY_STRING;
		}
		
		byte[] bytes = new byte[length];
		buf.get(bytes);
		
		try {
			return new String(bytes, 0, length, "utf-8");
		}
		catch ( UnsupportedEncodingException e ) {
			throw new RuntimeException(e);
		}
	}
	
	protected static int writeString(String str, ByteBuffer buf) {
		if ( str == null ) {
			buf.putInt(-1);
			
			return 4;
		}
		else {
			try {
				byte[] bytes = str.getBytes("utf-8");
				buf.putInt(bytes.length);
				buf.put(bytes);
				
				return bytes.length + 4;
			}
			catch ( UnsupportedEncodingException e ) {
				throw new RuntimeException(e);
			}
		}
	}
}
