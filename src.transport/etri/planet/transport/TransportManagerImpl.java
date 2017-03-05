package etri.planet.transport;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.locks.ReentrantLock;

import planet.PlanetUtils;
import planet.SystemException;
import planet.transport.Connection;
import planet.transport.TransportListener;
import planet.transport.TransportManager;

import etri.planet.TransportLoggers;


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public class TransportManagerImpl implements TransportManager {
	private static final int DEFAULT_CHANNEL_QUEUE_LENGTH = 128;
	
	private static final int STATE_NOT_STARTED = 0;
	private static final int STATE_RUNNING = 1;
	private static final int STATE_STOPPING = 2;
	private static final int STATE_STOPPED = 3;
	
	private volatile String m_id = null;
	private volatile String m_host = null;
	
	volatile SocketAcceptor m_acceptor;
	volatile IoScheduler m_scheduler;
	volatile IoProcessor[] m_processors;
	volatile IdleConnectionInspector m_idleConnectionCollector;
	
	volatile BlockingQueue<ConnectionImpl> m_ioReadyQ;
	volatile int m_ioReadyQLength = DEFAULT_CHANNEL_QUEUE_LENGTH;
	volatile int m_ioProcessorCount;
	volatile Executor m_executor;
	private ConnectionInspector m_inspector;
	final byte[] m_hbBytes;
	final byte[] m_hbAckBytes;
	
	private volatile int m_connectTimeout = 5000;			// 5 seconds
	private volatile long m_hbInterval;
	private int m_state;									// guarded by this
	
	private final ReentrantLock m_connLock;
	private final Map<String,ConnectionImpl> m_connections;	// guarded by 'm_connLock'
	volatile TransportListener m_listener;
	
	public TransportManagerImpl() {
		this(null);
	}
	
	public TransportManagerImpl(String id) {
		m_id = id;
		m_state = STATE_NOT_STARTED;
		m_connLock = new ReentrantLock();
		m_connections = new HashMap<String,ConnectionImpl>();

		m_ioProcessorCount = Runtime.getRuntime().availableProcessors();
    	
		try {
			m_hbBytes = new byte[TransportHeader.SIZE];
			ByteBuffer buffer = ByteBuffer.wrap(m_hbBytes);
			TransportMessage hbMsg= new HeartbeatMessage();
			hbMsg.m_header.m_length = TransportHeader.SIZE;
			hbMsg.encode(buffer);
			
			m_hbAckBytes = new byte[TransportHeader.SIZE];
			buffer = ByteBuffer.wrap(m_hbAckBytes);
			TransportMessage hbAckMsg= new HeartbeatAckMessage();
			hbAckMsg.m_header.m_length = TransportHeader.SIZE;
			hbAckMsg.encode(buffer);
		}
		catch ( IOException e ) {
			throw new SystemException("panic: cause=" + e);
		}
	}
	
	public String getId() {
		return m_id;
	}
	
	public String getHost() {
		return m_host;
	}
	
	public void setHost(String host) {
		m_host = host;
	}
    
    public int start(int port, Executor executor, TransportListener listener)
    	throws IOException {
    	if ( executor == null ) {
    		throw new NullPointerException("executor was null");
    	}
    	
    	synchronized ( this ) {
    		if ( m_state != STATE_NOT_STARTED ) {
    			throw new IllegalStateException("already started");
    		}
        	
        	m_listener = listener;
        	m_executor = executor;
        	
        	m_ioReadyQ = new ArrayBlockingQueue<ConnectionImpl>(m_ioReadyQLength);
        	
        	m_processors = new IoProcessor[m_ioProcessorCount];
        	for ( int i = 0; i < m_ioProcessorCount; ++i ) {
        		m_processors[i] = new IoProcessor(m_ioReadyQ, executor);
        		new Thread(m_processors[i], "planet:io-processor-" + (i+1)).start();
        	}
        	
        	m_scheduler = new IoScheduler(m_ioReadyQ);
        	m_scheduler.start();
        	
        	m_acceptor = new SocketAcceptor(this);
        	port = m_acceptor.start(port);
        	
        	if ( m_id == null ) {
        		if ( m_host == null ) {
        			m_host = PlanetUtils.LOCAL_HOST;
        		}
        		
        		m_id = m_host + ":" + port;
        	}
        	
        	m_idleConnectionCollector = new IdleConnectionInspector(this);
        	m_idleConnectionCollector.start();
        	
        	m_state = STATE_RUNNING;
        	
        	return port;
    	}
    }
    
    public void stop() {
    	synchronized ( this ) {
	    	if ( m_state != STATE_RUNNING ) {
	    		return;
	    	}
	    	
	    	// stop heartbeat handler
	    	if ( m_inspector != null ) {
	    		m_inspector.stop();
	    	}
	    	
	    	// stop idle connection collector handler
	    	if ( m_idleConnectionCollector != null ) {
	    		m_idleConnectionCollector.stop();
	    	}
	    	
	    	// stop accepting no more connections
	    	m_acceptor.stop();
	    	
	    	m_state = STATE_STOPPING;
	    	this.notifyAll();
    	}
    	
    	// close all the connections established
    	if ( TransportLoggers.CONN.isDebugEnabled() ) {
    		TransportLoggers.CONN.debug("closing all connections...");
    	}
    	
    	Connection[] conns = getConnections();
    	for ( int i =0; i < conns.length; ++i ) {
    		try {
    			conns[i].close();
    		}
    		catch ( Throwable ignored ) { }
    	}
    	
    	if ( TransportLoggers.CONN.isInfoEnabled() ) {
    		TransportLoggers.CONN.info("closed: all connections");
    	}
    	
    	// stop IoProcessors
    	for ( int i =0; i < m_processors.length; ++i ) {
    		m_processors[i].stop();
    	}
    	
    	// stop I/O scheduler
    	m_scheduler.shutdown();
    	
    	synchronized ( this ) {
    		m_state = STATE_STOPPED;
	    	this.notifyAll();
    	}
    }
    
    public ServerSocketChannel getServerSocketChannel() {
    	return m_acceptor.getServerSocketChannel();
    }
	
	public synchronized void setChannelQueueLength(int length) {
		if ( length <= 0 ) {
			throw new IllegalArgumentException("invalid channel queue length: " + length);
		}
		
		if ( m_state == STATE_NOT_STARTED ) {
			m_ioReadyQLength = length;
		}
		else {
			throw new IllegalStateException("already started");
		}
	}
	
	public synchronized void setIoProcessorCount(int count) {
		if ( count <= 0 ) {
			throw new IllegalArgumentException("invalid IoProcessor count: " + count);
		}
		
		if ( m_state == STATE_NOT_STARTED ) {
			m_ioProcessorCount = count;
		}
		else {
			throw new IllegalStateException("already started");
		}
	}
	
	@Override
	public int getConnectTimeout() {
		return m_connectTimeout;
	}

	public void setConnectTimeout(int timeout) throws IllegalArgumentException {
		if ( timeout < 0 ) {
			throw new IllegalArgumentException("invalid timeout=" + timeout + "s");
		}
		
		m_connectTimeout = timeout;
	}
	
	public long getHeartbeatInterval() {
		return m_hbInterval;
	}
    
    public void setHeartbeatInterval(long interval) {
    	if ( interval <= 0 ) {
    		if ( m_inspector != null ) {
    			m_inspector.stop();
    			m_inspector = null;
    		}
    		
    		return;
    	}
    	
    	if ( m_inspector != null ) {
    		m_inspector.stop();
    		m_inspector = null;
    	}

		m_inspector = new ConnectionInspector(this, m_hbInterval = interval);
    	new Thread(m_inspector, "planet:heartbeat").start();
    }

	public boolean existsConnection(String key) {
		m_connLock.lock();
		try {
			return m_connections.get(key) != null;
		}
		finally {
			m_connLock.unlock();
		}
	}
	
	public ConnectionImpl getConnection(String planetId, boolean create)
		throws IOException, InterruptedException {
		if ( m_id.equals(planetId) ) {
			throw new IllegalArgumentException("Cannot get the connection to myself: id=" + planetId);
		}
		
		ConnectionImpl conn = null;
		synchronized ( this ) {
			if ( m_state != STATE_RUNNING ) {
				throw new IOException("TransportManager is not running");
			}
		}
		
		planetId = PlanetUtils.resolovePlanetKey(planetId);
			
		m_connLock.lock();
		try {
			while ( true ) {
				conn = m_connections.get(planetId);
				if ( conn != null ) {
					m_connLock.unlock();
					int state = conn.waitWhileConnecting();
					m_connLock.lock();
					
					if ( state == ConnectionImpl.STATE_CONNECTED ) {
						return conn;
					}
				}
				else if ( !create ) {
					return null;
				}
				else {
					break;
				}
			}
	
			conn = new ConnectionImpl(this, planetId);
			m_connections.put(planetId, conn);
		}
		finally {
			m_connLock.unlock();
		}
		
		//
		// create a new Connection and wait until that is open
		//	
		int idx = planetId.indexOf(':');
		if ( idx < 0 ) {
			throw new IllegalArgumentException("invalid PLANET id=" + planetId);
		}
		String host = planetId.substring(0, idx);
		int port = Integer.parseInt(planetId.substring(idx+1));

		try {
			conn.open(host, port);
		}
		catch ( IOException e ) {
			m_connLock.lock();
			try {
				m_connections.remove(planetId);
			}
			finally {
				m_connLock.unlock();
			}
			
			synchronized ( conn ) {
				conn.m_state = ConnectionImpl.STATE_DISCONNECTED;
				conn.notifyAll();
			}
			
			throw e;
		}
		catch ( InterruptedException e ) {
			m_connLock.lock();
			try {
				m_connections.remove(planetId);
			}
			finally {
				m_connLock.unlock();
			}
			
			synchronized ( conn ) {
				conn.m_state = ConnectionImpl.STATE_DISCONNECTED;
				conn.notifyAll();
			}
			
			throw e;
		}
		catch ( RuntimeException e ) {
			m_connLock.lock();
			try {
				m_connections.remove(planetId);
			}
			finally {
				m_connLock.unlock();
			}
			
			synchronized ( conn ) {
				conn.m_state = ConnectionImpl.STATE_DISCONNECTED;
				conn.notifyAll();
			}
			
			throw e;
		}
		
		if ( TransportLoggers.CONN.isInfoEnabled() ) {
			TransportLoggers.CONN.info("connected: " + conn);
		}
		
		return conn;
	}
    
    public ConnectionImpl[] getConnections() {
    	m_connLock.lock();
    	try {
	    	ConnectionImpl[] conns = new ConnectionImpl[m_connections.size()];
	    	return m_connections.values().toArray(conns);
    	}
    	finally {
    		m_connLock.unlock();
    	}
    }
	
	// 외부에서 connection 요청으로 channel이 생성된 경우만 호출
	void onConnectionAcceptedBegin(final ConnectionImpl conn) throws IOException {
		synchronized ( this ) {
			if ( m_state != STATE_RUNNING ) {
				throw new IOException("TransportManager is not running");
			}
		}
			
		m_connLock.lock();
		ConnectionImpl prev;
		try {
			prev = m_connections.put(conn.getId(), conn);
		}
    	finally {
    		m_connLock.unlock();
    	}
		if ( prev != null ) {
			TransportLoggers.CONN.warn("duplicated connection (older discarded): conn=" + conn);
			
			prev.close();
		}
		
		if ( m_listener != null ) {
			m_listener.onConnected(conn);
		}
	}
	
	void onConnectionClosed(final Connection conn) {
		m_connLock.lock();
		try {
			m_connections.remove(conn.getId());
		}
    	finally {
    		m_connLock.unlock();
    	}
		
		m_executor.execute(new Runnable() {
			public void run() {
				m_listener.onDisconnected(conn);
			}
		});
	}
}
