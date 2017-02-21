package etri.planet;

import java.io.IOException;
import java.io.InputStream;

import planet.transport.InputChannel;



/**
 * 
 * @author Kang-Woo Lee
 */
public class StreamClient extends InputStream {
	private final InputChannel m_ichannel;
	private final byte[] m_oneByte = new byte[1];
	
	public StreamClient(InputChannel channel) throws IOException {
		m_ichannel = channel;
	}

	@Override
	public synchronized int read() throws IOException {
		return ( read(m_oneByte, 0, 1) > 0 ) ? ((int)m_oneByte[0] & 0xFF) : -1;
	}
	
	public int read(byte[] buf) throws IOException {
		return read(buf, 0, buf.length);
	}

	@Override
	public synchronized int read(byte[] buf, int offset, int size) throws IOException {
		return m_ichannel.readBytes(buf, offset, size);
	}
	
	@Override
	public void close() throws IOException {
		m_ichannel.close();
	}

	@Override
	public String toString() {
		return "Stream[" + m_ichannel + "]";
	}
}
