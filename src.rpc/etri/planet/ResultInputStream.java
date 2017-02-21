package etri.planet;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;


/**
 * 
 * @author Kang-Woo Lee
 */
public class ResultInputStream extends InputStream {
	private List<byte[]> m_queue;
	private int m_offset;
	private int m_remains;
	private boolean m_producerClosed;
	
	public ResultInputStream() {
		m_queue = new ArrayList<byte[]>();
		m_offset = 0;
		m_remains = 0;
		
		m_producerClosed = false;
	}
	
	public synchronized void putBytes(byte[] block) {
		if ( m_queue != null ) {
			m_queue.add(block);
			if ( m_queue.size() == 1 ) {
				this.notifyAll();
			}
		}
	}
	
	public synchronized void setProducerClosed() {
		m_producerClosed = true;
		this.notifyAll();
	}

	@Override
	public synchronized int read(byte[] buf, int offset, int size) throws IOException {
		byte[] block;
		
		try {
			block = pinBlock();
			if ( block == null ) {
				return -1;
			}
		}
		catch ( InterruptedException e ) {
			return 0;
		}
		
		if ( size > m_remains ) {
			size = m_remains;
		}
		
		System.arraycopy(block, m_offset, buf, offset, size);
		m_remains -= size;
		m_offset += size;
		
		return size;
	}

	@Override
	public int read() throws IOException {
		byte[] block;
		
		try {
			block = pinBlock();
			if ( block == null ) {
				return -1;
			}
		}
		catch ( InterruptedException e ) {
			return 0;
		}
		
		byte v = block[m_offset++];
		--m_remains;
		
		return 0x000000ff & v;
	}

	@Override
	public int read(byte[] buf) throws IOException {
		return read(buf, 0, buf.length);
	}

	@Override
	public synchronized void close() throws IOException {
		m_queue = null;
		super.close();
		
		this.notifyAll();
	}
	
	private byte[] pinBlock() throws IOException, InterruptedException {
		if ( m_queue == null ) {
			return null;
		}
		
		byte[] block;
		if ( m_remains == 0 ) {
			while ( !m_producerClosed && m_queue != null && m_queue.size() == 0 ) {
				this.wait();
			}
			
			if ( m_queue == null  ) {
				return null;
			}
			if ( m_producerClosed && m_queue.size() == 0 ) {
				return null;
			}
			
			block = m_queue.remove(0);
			m_offset = 0;
			m_remains = block.length;
		}
		else {
			block = m_queue.get(0);
		}
		
		return block;
	}
}
