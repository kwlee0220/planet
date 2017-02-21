package etri.planet.transport;


import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import planet.PlanetUtils;
import planet.SystemException;
import planet.transport.Connection;
import planet.transport.InputChannel;
import planet.transport.TransportManager;

import etri.planet.TransportLoggers;
import net.jcip.annotations.GuardedBy;


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
class MultiBlockInputChannel implements InputChannel {
	public static final int MAX_DATA_LENGTH = TransportManager.MAX_BLOCK_SIZE
											- TransportHeader.SIZE - 7;
	private final ConnectionImpl m_conn;
	private final int m_id;
	
	private final ReentrantLock m_lock = new ReentrantLock();
	private final Condition m_cond = m_lock.newCondition();
	
	@GuardedBy("m_lock") private boolean m_endOfProduce;
	@GuardedBy("m_lock") private boolean m_closed;
	@GuardedBy("m_lock") private Listener m_listener;

	@GuardedBy("m_lock") private ByteBuffer m_current;
	@GuardedBy("m_lock") private List<ByteBuffer> m_buffers;
	private final DataControlMessage m_dataCtrlMsg;
	private final byte[] m_dataCtrlBytes;
	
	MultiBlockInputChannel(ConnectionImpl conn, int id) {
		m_conn = conn;
		m_id = id;
		
		m_buffers = new ArrayList<ByteBuffer>(TransportManager.BUFFER_COUNT);
		
		m_dataCtrlBytes = new byte[TransportHeader.SIZE + 8];
		m_dataCtrlMsg = new DataControlMessage(m_id, DataControlMessage.NEXT_DATA);
		ByteBuffer buf = ByteBuffer.wrap(m_dataCtrlBytes);
		try {
			m_dataCtrlMsg.encode(buf);
		}
		catch ( IOException e ) {
			throw new SystemException("" + e);
		}
	}
	
	public void close() {
		close(false);
	}
	
	public void close(boolean forced) {
		Listener listener =null;
		
		m_lock.lock();
		try {
			if ( m_closed ) {
				return;
			}
			
			m_buffers.clear();
			m_current = null;
			m_closed = true;
			listener = m_listener;
			
			if ( !m_endOfProduce && !forced ) {
				byte[] closeMsgBytes = new byte[TransportHeader.SIZE + 8];
				DataControlMessage closeMsg = DataControlMessage.newCloseChannelMessage(m_id); 
				ByteBuffer buf = ByteBuffer.wrap(closeMsgBytes);
	
				try {
					closeMsg.encode(buf);
					sendMessage(closeMsgBytes, closeMsg);
				}
				catch ( Exception e ) {
					if ( TransportLoggers.CHANNEL.isDebugEnabled() ) {
						TransportLoggers.CHANNEL.debug("fails to send close-channel message: ch=" + this
													+ ", cause=" + PlanetUtils.unwrapThrowable(e));
					}
				}
			}
			
			m_cond.signalAll();
		}
		finally {
			m_lock.unlock();
		}
		
		if ( TransportLoggers.CHANNEL.isInfoEnabled() ) {
			TransportLoggers.CHANNEL.info("closed: " + this + ", " + m_conn);
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
	public void setListener(Listener listener) {
		m_lock.lock();
		try {
			m_listener = listener;
		}
		finally {
			m_lock.unlock();
		}
	}
	
	public byte read1() throws IOException {
		m_lock.lock();
		try {
			assertSpace(1);
			
			return m_current.get();
		}
		finally {
			m_lock.unlock();
		}
	}
	
	public short read2() throws IOException {
		m_lock.lock();
		try {
			assertSpace(2);
			
			return m_current.getShort();
		}
		finally {
			m_lock.unlock();
		}
//		m_lock.lock();
//		try {
//			return readShort();
//		}
//		finally {
//			m_lock.unlock();
//		}
	}
	
	public int read4() throws IOException {
		m_lock.lock();
		try {
			assertSpace(4);
			
			return m_current.getInt();
		}
		finally {
			m_lock.unlock();
		}
//		m_lock.lock();
//		try {
//			return readInt();
//		}
//		finally {
//			m_lock.unlock();
//		}
	}
	
	public long read8() throws IOException {
		m_lock.lock();
		try {
			assertSpace(8);
			
			return m_current.getLong();
		}
		finally {
			m_lock.unlock();
		}
	}
	
	public int readByteBuffer(ByteBuffer buf) throws IOException {
		m_lock.lock();
		try {
			if ( m_closed ) {
				throw new IOException("closed already");
			}
			
			int total = buf.remaining();
			while ( buf.remaining() > 0 ) {
				if ( m_current == null || m_current.remaining() == 0 ) {
					if ( waitMoreBytes() < 0 ) {
						break;
					}
				}
				
				if ( m_current.remaining() > buf.remaining() ) {
					TransportUtils.copyTo(m_current, buf, buf.remaining());
				}
				else {
					buf.put(m_current);
				}
			}
			
			return total - buf.remaining();
		}
		finally {
			m_lock.unlock();
		}
	}
	
	public int readBytes(byte[] bytes, int offset, int length) throws IOException {
		m_lock.lock();
		try {
			if ( m_closed ) {
				throw new IOException("closed already");
			}
			
			int remains = length;
			while ( remains > 0 ) {
				if ( m_current == null || m_current.remaining() == 0 ) {
					if ( waitMoreBytes() < 0 ) {
						int nread = (length - remains);
						if ( length > 0 && nread == 0 ) {
							return -1;
						}
						else {
							return nread;
						}
					}
				}
				
				int nbytes = Math.min(m_current.remaining(), remains);
				m_current.get(bytes, offset, nbytes);
				remains -= nbytes;
				offset += nbytes;
			}
	
			return length - remains;
		}
		finally {
			m_lock.unlock();
		}
	}
	
	public void appendBlock(ByteBuffer buf, boolean endOfProduce) {
		m_lock.lock();
		try {
			if ( !m_closed ) {
				m_buffers.add(buf);
				m_endOfProduce = endOfProduce;
				
				m_cond.signalAll();
			}
		}
		finally {
			m_lock.unlock();
		}
	}
	
	// 'm_lock'을 획득한 상태에서 호출되어야 한다.
	@GuardedBy("m_lock")
	private short readShort() throws IOException {
		if ( m_closed ) {
			throw new IOException("closed already");
		}
		
		if ( m_current != null && m_current.remaining() >= 2 ) {
			return m_current.getShort();
		}
		
		int remains = 2;
		
		byte[] buf = null;
		if ( m_current != null && m_current.remaining() == 1 ) {
			buf = new byte[2];
			m_current.get(buf, 0, 1);
			
			remains -= 1;
		}
		
		if ( waitMoreBytes() < 0 ) {
			throw new IOException("EOF reached");
		}
		
		if ( m_current == null || m_current.remaining() < remains ) {
			throw new IOException("EOF reached");
		}
		
		if ( buf != null ) {
			m_current.get(buf, 1, 1);
			
			return ByteBuffer.wrap(buf).getShort();
		}
		else {
			return m_current.getShort();
		}
	}
	
	// 'm_lock'을 획득한 상태에서 호출되어야 한다.
	@GuardedBy("m_lock")
	private int readInt() throws IOException {
		if ( m_closed ) {
			throw new IOException("closed already");
		}
		
		if ( m_current != null && m_current.remaining() >= 4 ) {
			return m_current.getInt();
		}
		
		int remains = 4;
		
		byte[] buf = null;
		if ( m_current != null && m_current.remaining() > 0 && m_current.remaining() < 4 ) {
			remains -= m_current.remaining();
			
			buf = new byte[4];
			m_current.get(buf, 0, m_current.remaining());
		}
		
		if ( waitMoreBytes() < 0 ) {
			throw new IOException("EOF reached");
		}
		
		if ( m_current == null || m_current.remaining() < remains ) {
			throw new IOException("EOF reached");
		}
		
		if ( buf != null ) {
			m_current.get(buf, 4-remains, remains);
			
			return ByteBuffer.wrap(buf).getInt();
		}
		else {
			return m_current.getInt();
		}
	}
	
	private static final int BYTES_LONG = 8;
	
	// 'm_lock'을 획득한 상태에서 호출되어야 한다.
	@GuardedBy("m_lock")
	private long readLong() throws IOException {
		if ( m_closed ) {
			throw new IOException("closed already");
		}
		
		if ( m_current != null && m_current.remaining() >= BYTES_LONG ) {
			return m_current.getLong();
		}
		
		int remains = BYTES_LONG;
		
		byte[] buf = null;
		if ( m_current != null && m_current.remaining() > 0 && m_current.remaining() < BYTES_LONG ) {
			remains -= m_current.remaining();
			
			buf = new byte[BYTES_LONG];
			m_current.get(buf, 0, m_current.remaining());
		}
		
		if ( waitMoreBytes() < 0 ) {
			throw new IOException("EOF reached");
		}
		
		if ( m_current == null || m_current.remaining() < remains ) {
			throw new IOException("EOF reached");
		}
		
		if ( buf != null ) {
			m_current.get(buf, BYTES_LONG-remains, remains);
			
			return ByteBuffer.wrap(buf).getLong();
		}
		else {
			return m_current.getLong();
		}
	}
	
	// 'm_lock'을 획득한 상태에서 호출되어야 한다.
	@GuardedBy("m_lock")
	private void assertSpace(int size) throws IOException {
		if ( m_closed ) {
			throw new IOException("closed already");
		}
		
		if ( m_current == null || m_current.remaining() == 0 ) {
			if ( waitMoreBytes() < 0 ) {
				throw new IOException("EOF reached");
			}
		}
		
		if ( m_current == null || m_current.remaining() < size ) {
			throw new IOException("EOF reached");
		}
	}

	// 'm_lock'을 획득한 상태에서 호출되어야 한다.
	@GuardedBy("m_lock")
	private int waitMoreBytes() throws IOException {
		try {
			// 데이타가 추가로 도착하거나 producer가 데이타 송신을 종료할 때까지 대기한다.
			while ( m_buffers.size() == 0 && !m_endOfProduce ) {
				m_cond.await();
			}
		}
		catch ( InterruptedException e ) {
			throw new IOException("I/O interrupted");
		}
		
		if ( m_buffers.size() == 0 && m_endOfProduce ) {
			return -1;
		}
		
		m_current = m_buffers.remove(0);
		if ( m_current.remaining() >= MAX_DATA_LENGTH && !m_endOfProduce ) {
			try {
				// 다음 메시지 송신 요청을 보낸다.
				sendMessage(m_dataCtrlBytes, m_dataCtrlMsg);
			}
			catch ( InterruptedException ignored ) { }
		}
		
		return m_current.remaining();
	}
	
	public String toString() {
		return String.format("channel[in:m:%d]", m_id);
	}
	
	private void sendMessage(byte[] bytes, DataControlMessage msg) throws InterruptedException {
		try {
			m_conn.write(ByteBuffer.wrap(bytes));
		}
		catch ( IOException e ) {
			TransportLoggers.MSG.error("fails to send 'DataCtrl': msg=" + msg + ", cause=" + e);
		}
		if ( TransportLoggers.CHANNEL.isDebugEnabled() ) {
			TransportLoggers.CHANNEL.debug("sent: msg=" + msg);
		}
	}
}
