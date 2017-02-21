package etri.planet.transport;


import java.io.IOException;
import java.nio.ByteBuffer;

import planet.transport.Connection;
import planet.transport.InputChannel;

import etri.planet.TransportLoggers;


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public class SingleBlockInputChannel implements InputChannel {
	private final ConnectionImpl m_conn;
	private final int m_id;
	private boolean m_closed;
	private volatile Listener m_listener;	// guarded by 'this'

	private final ByteBuffer m_buffer;
	
	SingleBlockInputChannel(ConnectionImpl conn, int channelId, ByteBuffer buffer) {
		m_conn = conn;
		m_id = channelId;
		m_buffer = buffer;
	}
	
	public void close() {
		Listener listener = null;
		synchronized ( this ) {
			listener = m_listener;
			m_closed = true;
			
			this.notifyAll();
		}
		
		if ( TransportLoggers.CHANNEL.isDebugEnabled() ) {
			TransportLoggers.CHANNEL.debug("released: " + this + ", " + m_conn);
		}
		
		if ( listener != null ) {
			listener.onClosed(this);
		}
	}
	
	public Connection getConnection() {
		return m_conn;
	}
	
	public int getId() {
		return m_id;
	}

	@Override
	public synchronized void setListener(Listener listener) {
		m_listener = listener;
	}
	
	public synchronized void waitForClosed() throws InterruptedException {
		while ( !m_closed ) {
			this.wait();
		}
	}
	
	public synchronized byte read1() throws IOException {
		assertSpace(1);
		
		return m_buffer.get();
	}
	
	public synchronized short read2() throws IOException {
		assertSpace(2);
		
		return m_buffer.getShort();
	}
	
	public synchronized int read4() throws IOException {
		assertSpace(4);
		
		return m_buffer.getInt();
	}
	
	public synchronized long read8() throws IOException {
		assertSpace(8);
		
		return m_buffer.getLong();
	}
	
	public synchronized int readByteBuffer(ByteBuffer buf) throws IOException {
		if ( m_closed ) {
			throw new IOException("closed already");
		}
		
		int nbytes = Math.min(m_buffer.remaining(), buf.remaining());
		TransportUtils.copyTo(m_buffer, buf, nbytes);
		
		return nbytes;
	}
	
	public synchronized int readBytes(byte[] bytes, int offset, int length) throws IOException {
		if ( m_closed ) {
			throw new IOException("closed already");
		}

		if ( m_buffer.remaining() == 0 ) {
			return -1;
		}
		else if ( m_buffer.remaining() < length ) {
			length = m_buffer.remaining();
		}
		m_buffer.get(bytes, offset, length);

		return length;
	}
	
	private void assertSpace(int size) throws IOException {
		if ( m_closed ) {
			throw new IOException("closed already");
		}
		
		if ( m_buffer.remaining() < size ) {
			throw new IOException("EOF reached");
		}
	}
	
	public String toString() {
		return String.format("channel[in:s:%d]", m_id); 
	}
}
