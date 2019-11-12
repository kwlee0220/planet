package etri.planet.transport;

import java.io.EOFException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.concurrent.GuardedBy;

import planet.DisconnectionHandler;
import planet.PlanetUtils;
import planet.SystemException;
import planet.transport.Connection;
import planet.transport.InputChannel;
import planet.transport.OutputChannel;
import planet.transport.TransportManager;

import etri.planet.TransportLoggers;
import etri.planet.thread.TimedThread;


/**
 * 내부적으로 세개의 mutex를 사용한다.
 * 동시 두개 이상의 쓰레드가 동시에 동일 소켓을 통해 데이타를 전송하는 것을 막기 위한 mutex (writeLock),
 * {@literal ConnectionImpl} 내부 공유 데이타의 접근의 동기화하기 위한 mutex ({@literal this}),
 * 그리고 내부적으로 write용 버퍼 풀에서 버퍼를 할당 받을 때 사용하는 mutex({@literal writeBufMutex})를
 * 사용한다.
 * <br>
 * {@literal writeLock}은 {@link #write(ByteBuffer)} 메소드 호출과 {@link #inspectForIdleness()}
 * 메소드 호출시 사용된다.
 * 만일 {@literal writeLock}과  {@literal this} mutex를 모두 잡아야 하는 경우는 반드시
 * {@literal writeLock}을 먼저 잡은 후 {@literal this} mutex를 잡는 순서를 따른다.
 * 
 * @author Kang-Woo Lee
 */
public class ConnectionImpl implements Connection, Delayed {
	static final int STATE_NOT_CONNECTED = 0;
	static final int STATE_CONNECTING = 1;
	static final int STATE_CONNECTED = 2;
	static final int STATE_DISCONNECTING = 3;
	static final int STATE_DISCONNECTED = 4;

	final TransportManagerImpl m_transport;
	private volatile String m_id;
	private volatile String m_toString;

	@GuardedBy("this") SocketChannel m_sockChannel;
	@GuardedBy("this") int m_state;
	@GuardedBy("this") private String m_description;
	@SuppressWarnings("unused")
	private volatile boolean m_isPrivateIp;
	
	private final IoScheduler m_scheduler;
	@GuardedBy("this") SelectionKey m_selectionKey;
	volatile int m_ops;
	final MessageBuilder m_msgBuilder;
	private final boolean m_active;
	private volatile boolean m_isDirty = false;
	private volatile boolean m_hbSent = false;

	private final ReentrantLock m_writerLock = new ReentrantLock();
	private final Condition m_writerCond = m_writerLock.newCondition();
	@GuardedBy("m_writerLock") private Thread m_writer;
	@GuardedBy("m_writerLock") private final List<WriteWaiter> m_writeWaiters
																= new ArrayList<WriteWaiter>();
	
	@GuardedBy("this") private long m_maxIdleMillis = -1;
	@GuardedBy("this") private long m_lastAccessMillis;
	@GuardedBy("this") private long m_nextDueInMillis;
	
	private final Object m_writeBufMutex = new Object();
	@GuardedBy("m_writeBufMutex") private boolean m_writeBufAvailable = false;
	
	private final AtomicInteger m_channelIdGen = new AtomicInteger(0);
	private final ConcurrentMap<Integer,MultiBlockOutputChannel> m_outChannels;
	private final Map<Integer,MultiBlockInputChannel> m_mbiChannels;
	private final CopyOnWriteArraySet<DisconnectionHandler> m_disconnListeners;
	private volatile Object m_attachment;
	
	// for accepted connection
	ConnectionImpl(TransportManagerImpl transport, SocketChannel sockChannel) throws IOException {
		m_transport = transport;
		
		m_scheduler = transport.m_scheduler;
		m_msgBuilder = new MessageBuilder(this);
		
		m_sockChannel = sockChannel;
		m_sockChannel.socket().setTcpNoDelay(true);
		m_sockChannel.configureBlocking(false);
		m_sockChannel.socket().setSendBufferSize(TransportManager.MAX_BLOCK_SIZE);
		m_sockChannel.socket().setReceiveBufferSize(TransportManager.MAX_BLOCK_SIZE);

		m_mbiChannels = new HashMap<Integer,MultiBlockInputChannel>();
		m_outChannels = new ConcurrentHashMap<Integer,MultiBlockOutputChannel>();
		m_disconnListeners = new CopyOnWriteArraySet<DisconnectionHandler>();
		
		m_active = false;
		m_state = STATE_NOT_CONNECTED;
	}
	
	// for connecting connection
	ConnectionImpl(TransportManagerImpl transport, String targetPlanetId) throws IOException {
		m_transport = transport;
		m_id = targetPlanetId;
		
		m_scheduler = transport.m_scheduler;
		m_msgBuilder = new MessageBuilder(this);

		m_mbiChannels = new HashMap<Integer,MultiBlockInputChannel>();
		m_outChannels = new ConcurrentHashMap<Integer,MultiBlockOutputChannel>();
		m_disconnListeners = new CopyOnWriteArraySet<DisconnectionHandler>();

		m_active = true;
		m_state = STATE_NOT_CONNECTED;
	}
	
	public final TransportManager getTransportManager() {
		return m_transport;
	}
	
	public final String getId() {
		return m_id;
	}
	
	void accept(String id) throws IOException {
		synchronized ( this ) {
			if ( m_state != STATE_NOT_CONNECTED ) {
				throw new IllegalStateException("should be 'not-open' state");
			}

			m_id = id;
			m_toString = m_id + ", local=" + m_sockChannel.socket().getLocalPort();
			
			m_state = STATE_CONNECTING;
			this.notifyAll();
		}
		
		m_transport.onConnectionAcceptedBegin(this);
		
		ByteBuffer buffer = ByteBuffer.allocate(TransportManager.MAX_BLOCK_SIZE);
		ConnectReplyMessage msg = new ConnectReplyMessage(m_id);
		msg.encode(buffer);
		buffer.flip();
		
		try {
			writeInLock(buffer);
		}
		catch ( InterruptedException ignored ) { }
	
		if ( TransportLoggers.MSG.isInfoEnabled() ) {
			TransportLoggers.MSG.info("sent: msg=" + msg);
		}
		
		synchronized ( this ) {
			m_state = STATE_CONNECTED;
			this.notifyAll();
		}
		
		if ( TransportLoggers.CONN.isDebugEnabled() ) {
			TransportLoggers.CONN.debug("accepted: " + this);
		}
	}
	
	void open(String host, int port) throws IOException, InterruptedException {
		synchronized ( this ) {
			if ( m_state != STATE_NOT_CONNECTED ) {
				throw new IllegalStateException("should be 'not-open' state");
			}
			
			m_state = STATE_CONNECTING;
			this.notifyAll();
		}
		
		InetSocketAddress addr = new InetSocketAddress(host, port);
		m_sockChannel = SocketChannel.open();
		
		long connectTimeout = m_transport.getConnectTimeout();
		if ( TransportLoggers.CONN.isDebugEnabled() ) {
			TransportLoggers.CONN.debug("connecting: " + addr);
		}
		
		Socket socket = m_sockChannel.socket();
		socket.setReuseAddress(true);

		if ( connectTimeout > 0 ) {
			socket.connect(addr, (int)connectTimeout);
		}
		else {
			socket.connect(addr);
		}
		socket.setTcpNoDelay(true);
		m_sockChannel.configureBlocking(false);
		m_sockChannel.socket().setSendBufferSize(TransportManager.MAX_BLOCK_SIZE);
		m_sockChannel.socket().setReceiveBufferSize(TransportManager.MAX_BLOCK_SIZE);
		
		m_scheduler.submitForRead(this);

		ConnectMessage msg = new ConnectMessage(m_transport.getId());
		ByteBuffer buf = ByteBuffer.allocate(TransportManager.MAX_BLOCK_SIZE);
		msg.encode(buf);
		buf.flip();
		writeInLock(buf);
		
		m_toString = m_id + ", local=" + m_sockChannel.socket().getLocalPort();
		
		// 상대방에서 connect 메시지 응답이 올 때가지 대기한다.
		synchronized ( this ) {
			while ( m_state == STATE_CONNECTING ) {
				this.wait();
			}
		}
	}
	
	synchronized int waitWhileConnecting() throws InterruptedException {
		while ( m_state == STATE_CONNECTING || m_state == STATE_NOT_CONNECTED ) {
			this.wait();
		}
		
		return m_state;
	}
	
	synchronized void notifyConnectReplied(int code, String details) {
		int idx1 = m_id.indexOf(':');
		int idx2 = details.indexOf(':');
		m_isPrivateIp = !m_id.substring(0, idx1).equals(details.substring(0, idx2));
		
		m_state = STATE_CONNECTED;
		this.notifyAll();
	}

	private static final MultiBlockInputChannel[] PROTO = new MultiBlockInputChannel[0];
	public void close() {
		synchronized ( this ) {
			while ( m_state == STATE_DISCONNECTING ) {
				try {
					this.wait();
				}
				catch ( InterruptedException e ) {
					return;
				}
			}
			
			if ( m_state == STATE_DISCONNECTED ) {
				return;
			}
			
			m_state = STATE_DISCONNECTING;
			this.notifyAll();
		}
		
		// 모든 output channel을 강제로 닫는다.
		for ( OutputChannel ch: m_outChannels.values() ) {
			try {
				ch.close(false);
			}
			catch ( IOException ignored ) { }
		}
		
		// output socket을 close하여 상대방에게 알린다.
		try {
			m_sockChannel.close();
		}
		catch ( IOException ignored ) { }
		
		// 모든 input channel을 강제로 닫는다.
		MultiBlockInputChannel[] inChannels;
		synchronized ( m_mbiChannels ) {
			inChannels = m_mbiChannels.values().toArray(PROTO);
		}
		
		for ( MultiBlockInputChannel ch: inChannels ) {
			try {
				ch.close(true);
			}
			catch ( Exception ignored ) { }
		}
		
		synchronized ( this ) {
			m_state = STATE_DISCONNECTED;
			this.notifyAll();
		}
		
		if ( TransportLoggers.CONN.isInfoEnabled() ) {
			TransportLoggers.CONN.info("closed: " + this);
		}
		
		m_transport.onConnectionClosed(this);
		
		// 등록된 모든 단절 핸들러를 호출한다.
		for ( final DisconnectionHandler listener: m_disconnListeners ) {
			m_transport.m_executor.execute(new Runnable() {
				public void run() {
					TimedThread.setTaskDescription("DisconnectionHandler.onDisconnected on "
													+ listener);
					try {
						listener.onDisconnected(ConnectionImpl.this);
					}
					catch ( Throwable ignored ) { }
				}
			});
		}
	}

	@Override
	public synchronized final boolean isClosed() {
		return m_state == STATE_DISCONNECTED || m_state == STATE_DISCONNECTING;
	}

	@Override
	public boolean isActive() {
		return m_active;
	}
	
	public InputChannel allocateSingleBlockInputChannel(int id, ByteBuffer buffer) {
		SingleBlockInputChannel channel = new SingleBlockInputChannel(this, id, buffer);
		
		if ( TransportLoggers.CHANNEL.isDebugEnabled() ) {
			TransportLoggers.CHANNEL.debug("allocated: " + channel + ", " + this);
		}
		
		return channel;
	}
	
	public MultiBlockInputChannel lookupInputChannel(int id) {
		synchronized ( m_mbiChannels ) {
			return m_mbiChannels.get(id);
		}
	}
	
	public void registerInputChannel(MultiBlockInputChannel ichannel) throws IOException {
		int id = ichannel.getId();
		synchronized ( m_mbiChannels ) {
			MultiBlockInputChannel prev = m_mbiChannels.put(id, ichannel);
			if ( prev != null ) {
				m_mbiChannels.put(id, prev);
				
				throw new SystemException("duplicated InputChannel id=" + id);
			}
		}
	}
	
	public void unregisterInputChannel(int id) {
		synchronized ( m_mbiChannels ) {
			m_mbiChannels.remove(id);
		}
	}
	
	public int getInputChannelCount() {
		synchronized ( m_mbiChannels ) {
			return m_mbiChannels.size();
		}
	}

	@Override
	public MultiBlockOutputChannel allocateOutputChannel() throws IOException {
		checkOpened();
		
		int id = m_channelIdGen.incrementAndGet();
		
		MultiBlockOutputChannel ochannel = new MultiBlockOutputChannel(this, id);
		m_outChannels.put(id, ochannel);
		
		if ( TransportLoggers.CHANNEL.isInfoEnabled() ) {
			TransportLoggers.CHANNEL.info("allocated: " + ochannel + ", " + this);
		}
		
		return ochannel;
	}
	
	public void _onOutputChannelClosed(int id) {
		m_outChannels.remove(id);
	}
	
	OutputChannel getOutputChannel(int channelId) {
		return m_outChannels.get(channelId);
	}
	
	void unpendingOutputChannel(int id) {
		MultiBlockOutputChannel outChannel = m_outChannels.get(id);
		if ( outChannel != null ) {
			outChannel.ackReceived();
		}
	}
	
	public Object getAttachment() {
		return m_attachment;
	}
	
	public void setAttachment(Object att) {
		m_attachment = att;
	}
	
	static class WriteWaiter {
		ByteBuffer m_buffer;
		boolean m_done;
		
		WriteWaiter(ByteBuffer buffer) {
			m_buffer = buffer;
		}
	}

	// 본 메소드는 반드시 'this' mutex를 hold하지 않은 상태에서 호출되어야 한다.
	public void write(ByteBuffer buffer) throws IOException, InterruptedException {
		checkOpened();
		
		// 현재 쓰는 쓰레드가 있는가를 조사하기 위해 m_writer가 null인가 확인한다.
		//
		m_writerLock.lock();
		if ( m_writer == null && m_writeWaiters.isEmpty() ) {
			// 사용 중인 쓰레드가 없는 경우, 바로 write를 시도한다.
			//
			writeAndUnlock(buffer);
		}
		else {
			// 사용 중인 쓰레드가 있는 경우는 대기 큐에 전송 요청을 삽입하고 대기한다.
			//
			WriteWaiter waiter = new WriteWaiter(buffer);
			m_writeWaiters.add(waiter);
			
			while ( true ) {
				try {
					m_writerCond.await();
				}
				catch ( InterruptedException e ) {
					m_writerLock.unlock();
					throw e;
				}
				
				if ( m_writer == null ) {
					// 전송 작업을 수행하던 쓰레드가 작업을 완료하고 깨운 경우, write를 시도한다.
					m_writeWaiters.remove(waiter);
					writeAndUnlock(buffer);
					
					return;
				}
			}
		}
	}
	
	public void write(byte[] bytes) throws IOException, InterruptedException {
		write(ByteBuffer.wrap(bytes));
	}

	//
	// 본 메소드는 반드시 'm_writeLock'을 hold한 상태에서 호출되어야 하고,
	// 메소드 반환 전에는 반드시 'm_writeLock'를 반환해야 한다.
	// 실제 write 할 때는 'm_writeLock'을 잠시 풀고 수행한다. 
	//
	private void writeAndUnlock(ByteBuffer buffer) throws IOException, InterruptedException {
		m_writer = Thread.currentThread();
		m_writerLock.unlock();
		
		writeInLock(buffer);
		
		m_writerLock.lock();
		try {
			m_writer = null;

			// write 중에 다른 write 요청으로 전송 큐에 대기 중인 쓰레드가 있는 경우
			// 이들 중 한 쓰레드를 깨워 데이타를 전송하도록 한다.
			//
			if ( m_writeWaiters.size() > 0 ) {
				m_writerCond.signal();
			}
		}
		finally {
			m_writerLock.unlock();
		}
	}

	//
	// 본 메소드는 반드시 'm_writeLock'을 hold하지 않은 상태에서 호출되고,
	// 호출시에는 반드시 'm_writer'에 호출하는 쓰레드가 기록되어야 한다.
	// 단, connection이 아직 open되지 않은 상태에서 'm_writeLock' hold 없이 호출될 수 있다.
	//
	void writeInLock(ByteBuffer buffer) throws IOException, InterruptedException {
		int total = 0;
		while ( buffer.hasRemaining() ) {
			synchronized ( this ) {
				if ( m_state != STATE_CONNECTED && m_state != STATE_CONNECTING ) {
					throw new IOException("not opened");
				}
			}

//			System.out.print(Thread.currentThread().getId() + "(SW): total=" + total + ", remains=" + buffer.remaining()); System.out.flush();
			int nwrite = m_sockChannel.write(buffer);
//			System.out.println(" -> total=" + (total+nwrite) + ", remains=" + buffer.remaining() + ", " + buffer.hasRemaining());
			if ( nwrite == 0 ) {
				// write가 당장 허락되지 않는 경우, ioscheduler에게 write 대기 요청을 전달한 후,
				// write 연산이 가능할 때까지 대기한다.
				//
				waitForWriteBuffer();
			}
			else {
				total += nwrite;
			}
		}
		
		if ( TransportLoggers.IO.isDebugEnabled() ) {
			TransportLoggers.IO.debug("sent: bytes=" + total + ", conn=" + this);
		}
	}

	//
	// 본 메소드는 반드시 'm_writeLock'을 hold하지 않은 상태에서 호출되고,
	// 호출시에는 반드시 'm_writer'에 호출하는 쓰레드가 기록되어야 한다.
	// 단, connection이 아직 open되지 않은 상태에서 'm_writeLock' hold 없이 호출될 수 있다.
	//
	void writeMultipleInLock(ByteBuffer... buffers) throws IOException, InterruptedException {
		int total = 0;
		while ( hasRemaining(buffers) ) {
			synchronized ( this ) {
				if ( m_state != STATE_CONNECTED && m_state != STATE_CONNECTING ) {
					throw new IOException("not opened");
				}
			}

			System.out.print(Thread.currentThread().getId() + "(MW=" + buffers.length + "): total=" + total + ", remains=" + remaining(buffers)); System.out.flush();
			long nwrite = m_sockChannel.write(buffers);
			System.out.println(" -> total=" + (total+nwrite) + ", remains=" + remaining(buffers) + ", " + hasRemaining(buffers));
			if ( nwrite == 0 ) {
				// write가 당장 허락되지 않는 경우, ioscheduler에게 write 대기 요청을 전달한 후,
				// write 연산이 가능할 때까지 대기한다.
				//
				waitForWriteBuffer();
			}
			else {
				total += nwrite;
			}
		}
		
		if ( TransportLoggers.IO.isDebugEnabled() ) {
			TransportLoggers.IO.debug("sent: bytes=" + total + ", conn=" + this);
		}
	}
	
	private boolean hasRemaining(ByteBuffer...buffers) {
		for ( int i =buffers.length-1; i >= 0; --i ) {
			if ( buffers[i].remaining() > 0 ) {
				return true;
			}
		}
		
		return false;
	}
	
	private int remaining(ByteBuffer...buffers) {
		int remains = 0;
		for ( int i =buffers.length-1; i >= 0; --i ) {
			remains += buffers[i].remaining();
		}
		
		return remains;
	}
	
	public synchronized int getMaxIdleSeconds() {
		return (int)TimeUnit.MILLISECONDS.toSeconds(m_maxIdleMillis); 
	}
	
	public void setMaxIdleSeconds(int seconds) {
		if ( seconds < 0 ) {
			synchronized ( this ) {
				m_maxIdleMillis = -1;
			}
		}
		else {
			synchronized ( this ) {
				m_maxIdleMillis = TimeUnit.SECONDS.toMillis(seconds);
				m_nextDueInMillis = System.currentTimeMillis() + m_maxIdleMillis;
			}
			
			m_transport.m_idleConnectionCollector.schedule(this);
		}
	}
	
	void onIdleTimeout() {
		boolean timedout;
		synchronized ( this ) {
			timedout = m_nextDueInMillis < System.currentTimeMillis();
		}
		
		if ( timedout ) {
			if ( TransportLoggers.CONN.isInfoEnabled() ) {
				TransportLoggers.CONN.info("disconnect due to idle timeout=" + this);
			}
			
			close();
		}
		else {
			m_transport.m_idleConnectionCollector.schedule(this);
		}
	}
	
	synchronized void updateDataAccessTime() {
		m_lastAccessMillis = System.currentTimeMillis();
		m_nextDueInMillis = m_lastAccessMillis + m_maxIdleMillis;
	}

	public synchronized long getDelay(TimeUnit unit) {
		long remains = m_maxIdleMillis - System.currentTimeMillis();
		
		return unit.convert(remains, TimeUnit.MILLISECONDS);
	}

	public synchronized String getDescription() {
		return m_description;
	}

	public synchronized void setDescription(String description) {
		m_description = description;
	}

	public int compareTo(Delayed other) {
		long gap;
		synchronized ( this ) {
			gap = m_nextDueInMillis;
		}
		synchronized ( other ) {
			gap -= ((ConnectionImpl)other).m_nextDueInMillis;
		}
		
		if ( gap > 0 ) {
			return 1;
		}
		else if ( gap < 0 ) {
			return -1;
		}
		else {
			return 0;
		}
	}
	
	void submitForRead() {
		try {
			m_scheduler.submitForRead(this);
		}
		catch ( CancelledKeyException expected ) {
			return;
		}
	}
	
	private static final int MAX_WRITE_WAIT_MILLIS = 10*1000;
	private void waitForWriteBuffer() throws InterruptedException, IOException {
		m_scheduler.submitForWrite(this);
//		System.out.println("\t" + Thread.currentThread().getId() + ": @@@@@@@@@@ IN");
		
		long due = System.currentTimeMillis() + MAX_WRITE_WAIT_MILLIS;
		synchronized ( m_writeBufMutex ) {
			while ( !m_writeBufAvailable ) {
				long remains = due - System.currentTimeMillis();
				if ( remains <= 0 ) {
//					Selector selector = m_scheduler.m_selector;
//					
//					for ( SelectionKey key: selector.keys() ) {
//						System.out.println("same channel=" + (key.channel().equals(m_sockChannel)));
//						System.out.println("key.ops=" + ((key.interestOps() & SelectionKey.OP_WRITE) != 0));
//					}
//					selector.wakeup();
//					int nselecteds = selector.selectNow();
//					Set<SelectionKey> selecteds = selector.selectedKeys();
//					if ( selecteds.size() > 0 ) {
//						SelectionKey key = selecteds.iterator().next();
//						System.out.println("is.writable=" + key.isWritable());
//					}
//					
//					
//					ByteBuffer buf = ByteBuffer.wrap(new byte[8]);
//					int nwrite = m_sockChannel.write(buf);
//					System.out.println("nwrite=" + nwrite);
					
					System.out.println("@@@@@@@@@@@@@@@@: SOCKET WRITE MIGHT NOT BE WORKING !!!!!!!");
					throw new IOException("socket write may be not working...");
				}
				
				m_writeBufMutex.wait(remains);
			}
			
			m_writeBufAvailable = false;
		}
//		System.out.println("\t" + Thread.currentThread().getId() + ": @@@@@@@@@@ OUT");
	}
	
	void notifyWriteIsReady() {
		synchronized ( m_writeBufMutex ) {
			m_writeBufAvailable = true;
			m_writeBufMutex.notify();
		}
	}
	
	synchronized void assertState(int state) {
		if ( m_state != state ) {
			throw new IllegalStateException("invalid state: expected=" + state
											+ ", actual=" + m_state);
		}
	}
	
	public final void addDisconnectionHandler(final DisconnectionHandler handler) {
		if ( handler == null ) {
			throw new NullPointerException("DisconnectionHandler was null");
		}
		
		if ( isClosed() ) {
			m_transport.m_executor.execute(new Runnable() {
				public void run() {
					try {
						handler.onDisconnected(ConnectionImpl.this);
					}
					catch ( Throwable ignored ) { }
				}
			});
		}
		else {
			m_disconnListeners.add(handler);
			if ( TransportLoggers.CONN.isDebugEnabled() ) {
				TransportLoggers.CONN.debug("added DisconnectionHandler[" + handler + "]");
			}
		}
	}
	
	public final void removeDisconnectionHandler(DisconnectionHandler listener) {
		m_disconnListeners.remove(listener);
	}
	
	synchronized void waitForOpened() throws InterruptedException {
		while ( m_state == STATE_NOT_CONNECTED ) {
			this.wait();
		}
	}
	
	void inspectForIdleness() {
		synchronized ( this ) {
			if ( m_state != STATE_CONNECTED ) {
				return;
			}
		}
		
		if ( !m_isDirty ) {
			if ( m_hbSent ) {
				if ( TransportLoggers.CONN.isInfoEnabled() ) {
					TransportLoggers.CONN.info("\tdisconnecting: timed-out conn=" + this);
				}
				
				close();
			}
			else {
				m_writerLock.lock();
				try {
					if ( m_writer == null ) {
						if ( TransportLoggers.CONN.isDebugEnabled() ) {
							TransportLoggers.CONN.debug("\tsending: HEARTBEAT thru conn=" + this);
						}
						
						m_hbSent = true;
						writeAndUnlock(ByteBuffer.wrap(m_transport.m_hbBytes));
					}
					else {
						m_writerLock.unlock();
						
						if ( TransportLoggers.CONN.isInfoEnabled() ) {
							TransportLoggers.CONN.info("skipped: sending HEARTBEAT to " + this
											+ " due to locked by others");
						}
					}
				}
				catch ( Throwable e ) {
					if ( TransportLoggers.CONN.isInfoEnabled() ) {
						TransportLoggers.CONN.info("fails to send HEARTBEAT thru " + this + ", cause="
										+ PlanetUtils.unwrapThrowable(e));
					}
					
					// 아직 open되기 이전의 connection도 inspection의 대상이 될 수 있기 때
					synchronized ( this ) {
						if ( m_state == STATE_CONNECTED ) {
							close();
						}
					}
				}
			}
		}
		else {
			m_isDirty = false;
		}
	}
	
	void setDirty() {
		m_isDirty = true;
		m_hbSent = false;
	}
	
	private synchronized void checkOpened() throws EOFException {
		if ( m_state == STATE_DISCONNECTED ) {
			throw new EOFException("" + this);
		}
	}
	
	private static final String EMPTY_STRING = "";
	public static String readString(ByteBuffer buf) {
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
	
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		else if ( obj == null || !(obj instanceof ConnectionImpl) ) {
			return false;
		}
		
		ConnectionImpl other = (ConnectionImpl)obj;
		return m_id.equals(other.m_id);
	}
	
	public int hashCode() {
		return m_id.hashCode();
	}
	
	public String toString() {
		return "conn[" + m_toString + "]";
	}
}
