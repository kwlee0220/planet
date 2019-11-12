package etri.planet.transport;


import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.concurrent.GuardedBy;

import planet.transport.Connection;
import planet.transport.OutputChannel;
import planet.transport.TransportManager;

import etri.planet.TransportLoggers;

/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public class MultiBlockOutputChannel implements OutputChannel {
	private static final int MAX_PENDINGS = TransportManager.BUFFER_COUNT + 1;
	
	private static final int STATE_RUNNING = 0;
	private static final int STATE_CLOSING = 1;
	private static final int STATE_CLOSED = 2;
	
	private final ConnectionImpl m_conn;
	private final int m_id;
	private final ByteBuffer m_buffer;
	private volatile int m_part =0;
	private int m_length;
	
	private final ReentrantLock m_lock;
	private final Condition m_cond;
	@GuardedBy("m_lock") private int m_state;
	@GuardedBy("m_lock") private int m_pendingCount;
	private Listener m_listener;
	
	public MultiBlockOutputChannel(ConnectionImpl conn, int channelId) {
		m_conn = conn;
		m_state = STATE_RUNNING;
		m_id = channelId;
		m_buffer = ByteBuffer.allocate(TransportManager.MAX_BLOCK_SIZE);
		m_buffer.position(TransportHeader.SIZE);
		m_length = 0;
		m_pendingCount = 0;
		
		m_lock = new ReentrantLock();
		m_cond = m_lock.newCondition();
	}
	
	// flush가 false인 경우는 blocking되는 경우가 없다.
	@Override
	public void close(boolean flush) throws IOException {
		m_lock.lock();
		try {
			if ( m_state > STATE_RUNNING ) {
				return;
			}
			
			m_state = STATE_CLOSING;
			m_cond.signalAll();
		
			if ( flush ) {
				try {
					flush(true);
				}
				catch ( IOException ignored ) { }
			}
			
			// flush까지 완료되었으므로, 소속된 connection에서 등록된 OutputStream 객체를 해제시킨다.
			m_conn._onOutputChannelClosed(m_id);

			// flush 중 대기하는 쓰레드를 깨움
			m_state = STATE_CLOSED;
			m_cond.signalAll();
		}
		finally {
			m_lock.unlock();
		}
		
		if ( TransportLoggers.CHANNEL.isInfoEnabled() ) {
			TransportLoggers.CHANNEL.info("closed: " + this + ", " + m_conn);
		}
	}

	// 'closingByOther()' 호출이 반환되지 않은 경우 channel에 대한 close가
	// 시도되지 않을 수 있다.
	@Override
	public void closeByOther() throws IOException {
		Listener listener = null;
		m_lock.lock();
		try {
			listener = m_listener;
		}
		finally {
			m_lock.unlock();
		}
		
		if ( listener != null ) {
			try {
				listener.beforeClosedByOther();
			}
			catch ( Exception e ) { }
		}
		
		close(false);
	}

	@Override
	public void setListener(Listener listener) {
		m_lock.lock();
		try {
			m_listener = listener;
		}
		finally {
			m_lock.unlock();
		}
	}
	
	public void flush() throws IOException {
		flush(false);
	}
	
	public int getId() {
		return m_id;
	}
	
	public Connection getConnection() {
		return m_conn;
	}
	
	public void write1(byte v) throws IOException {
		assertSpace(1);
		m_buffer.put(v);
	}
	
	public void write2(short v) throws IOException {
		assertSpace(4);
		m_buffer.putShort(v);
	}

	public void write4(int v) throws IOException {
		assertSpace(4);
		m_buffer.putInt(v);
	}

	public void write8(long v) throws IOException {
		assertSpace(8);
		m_buffer.putLong(v);
	}
	
	public void writen(byte[] bytes, int offset, int length) throws IOException {
		writeByteBuffer(ByteBuffer.wrap(bytes, offset, length));
	}
	
	public void writen(byte[] bytes) throws IOException {
		writen(bytes, 0, bytes.length);
	}
	
	public void writen(ByteBuffer buffer) throws IOException {
		writeByteBuffer(buffer);
	}
	
	void ackReceived() {
		m_lock.lock();
		try {
			--m_pendingCount;
			m_cond.signalAll();
		}
		finally {
			m_lock.unlock();
		}
	}

	// 이미 전송되었지만, 아직 ack를 받지 못한 block의 갯수가 'MAX_PENDINGS' 보다
	// 같거나 큰 경우는 blocking될 수 있다.
	private void flush(boolean isFinal) throws IOException {
		m_lock.lock();
		try {
			// 이미 전송되었지만, 아직 ack를 받지 못한 block의 갯수가 'MAX_PENDINGS' 보다
			// 같거나 큰 경우는 wait될 수 있다.
			while ( m_pendingCount >= MAX_PENDINGS && m_state == STATE_RUNNING ) {
				try {
					m_cond.await();
				}
				catch ( InterruptedException e ) {
					throw new IOException("I/O interrupted");
				}
			}
			
			if ( m_state == STATE_CLOSED ) {
				throw new IOException("closed already");
			}
			
			m_buffer.flip();
			m_length += (m_buffer.limit() - TransportHeader.SIZE);
			
			OutgoingDataMessage msg = new OutgoingDataMessage(m_id, m_part++, isFinal,
																m_buffer.limit(), m_buffer);
			msg.m_header.serialize(m_buffer.duplicate());
			
			m_conn.write(m_buffer);
			m_conn.updateDataAccessTime();
			
			++m_pendingCount;
			
			if ( TransportLoggers.CHANNEL.isDebugEnabled() ) {
				TransportLoggers.CHANNEL.debug("sent: msg=" + msg + ", " + m_conn);
			}
			
			m_buffer.clear().position(TransportHeader.SIZE);
		}
		catch ( InterruptedException e ) {
			throw new IOException("IO interrupted");
		}
		finally {
			m_lock.unlock();
		}
	}
	
	private void writeByteBuffer(ByteBuffer buffer) throws IOException {
		if ( m_buffer.remaining() >= buffer.remaining() ) {
			m_buffer.put(buffer);
		}
		else {
			while ( buffer.remaining() > 0 ) {
				int size = Math.min(m_buffer.remaining(), buffer.remaining());
				
				TransportUtils.copyTo(buffer, m_buffer, size);
				if ( m_buffer.remaining() == 0 ) {
					flush(false);
				}
			}
		}
	}
	
	private void assertSpace(int size) throws IOException {
		if ( m_state == STATE_CLOSED ) {
			throw new IOException("closed already");
		}
		
		if ( m_buffer.remaining() < size ) {
			flush(false);
		}
	}
	
	public String toString() {
		return String.format("channel[out:%d, pendings=%d]", m_id, m_pendingCount);
	}
}
