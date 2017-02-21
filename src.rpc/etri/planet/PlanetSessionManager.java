package etri.planet;

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;

import planet.PlanetRuntimeException;
import planet.PlanetServerListener;
import planet.PlanetUtils;
import planet.SystemException;
import planet.transport.Connection;
import planet.transport.InputChannel;
import planet.transport.TransportListener;

import etri.planet.thread.TimedThread;
import etri.planet.transport.ConnectionImpl;
import etri.planet.transport.TransportManagerImpl;


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public class PlanetSessionManager implements TransportListener {
	private final PlanetServerImpl m_planet;
	private volatile TransportManagerImpl m_transport;
	private final ConcurrentMap<String,PlanetSessionImpl> m_sessions;
	private volatile CopyOnWriteArraySet<PlanetServerListener> m_listeners;
	
	public PlanetSessionManager(PlanetServerImpl planet) {
		m_planet = planet;
		m_sessions = new ConcurrentHashMap<String,PlanetSessionImpl>();
    	m_listeners = new CopyOnWriteArraySet<PlanetServerListener>();
	}
    
    public void start(TransportManagerImpl transManager) throws IOException {
    	m_transport = transManager;
    }
    
    public void stop() {
    }

    public Collection<String> getPlanetSessionKeys() {
    	return m_sessions.keySet();
    }
    
	public boolean existsPlanetSession(String key) {
		return m_sessions.get(key) != null;
	}
	
	public PlanetSessionImpl getPlanetSession(String key, boolean create)
		throws IOException, InterruptedException {
		if ( m_transport == null ) {
			throw new PlanetRuntimeException("planet has not been started yet.");
		}
		
		PlanetSessionImpl session = m_sessions.get(key);
		if ( session != null || !create ) {
			return session;
		}
		
		ConnectionImpl conn = m_transport.getConnection(key, true);
		session = new PlanetSessionImpl(m_planet, conn);
		PlanetSessionImpl prev = m_sessions.putIfAbsent(key, session);
		return ( prev == null ) ? session : prev;
	}
	
	public boolean addPlanetServerListener(PlanetServerListener listener) {
		// 내부 호출만 고려하여 인자의 null 여부는 별도로 검사하지 않는다.
		//
		boolean done = m_listeners.add(listener);
		if ( done ) {
			if ( RpcLoggers.SESSION.isDebugEnabled() ) {
				RpcLoggers.SESSION.debug("added PlanetServerListener=" + listener);
			}
		}
		
		return done;
	}
	
	public boolean removePlanetServerListener(PlanetServerListener listener) {
		// 내부 호출만 고려하여 인자의 null 여부는 별도로 검사하지 않는다.
		//
		boolean done = m_listeners.remove(listener);
		if ( done ) {
			if ( RpcLoggers.SESSION.isDebugEnabled() ) {
				RpcLoggers.SESSION.debug("removed PlanetServerListener=" + listener);
			}
		}
		
		return done;
	}

	@Override
	public void onConnected(Connection conn) {
		final PlanetSessionImpl session = new PlanetSessionImpl(m_planet, (ConnectionImpl)conn);
		m_sessions.putIfAbsent(conn.getId(), session);
		
		m_planet.execute(new Runnable() {
			public void run() {
				TimedThread.setTaskDescription("PlanetServerListener.onConnected");
				
				for ( PlanetServerListener listener: m_listeners ) {
					try {
						listener.onSessionOpened(session);
					}
					catch ( Throwable ignored ) { }
				}
			}
		});
	}

	@Override
	public void onDisconnected(Connection conn) {
		String connId = conn.getId();
		if ( connId == null ) {
			return;
		}
		
		final PlanetSessionImpl session = m_sessions.remove(connId);
		if ( session != null ) {
			session.onConnectionClosed(conn);
		
			m_planet.execute(new Runnable() {
				public void run() {
					TimedThread.setTaskDescription("PlanetServerListener.onDisconnected");
					
					for ( PlanetServerListener listener: m_listeners ) {
						try {
							listener.onSessionClosed(session);
						}
						catch ( Throwable ignored ) { }
					}
				}
			});
		}
	}

	@Override
	public void onInputChannelCreated(InputChannel inChannel, boolean nonblocking) {
		final String key = inChannel.getConnection().getId();
		
		PlanetSessionImpl session = m_sessions.get(key);
		if ( session != null ) {
			try {
				PlanetMessageHandler.execute(session, inChannel, nonblocking);
			}
			catch ( Throwable e ) {
				RpcLoggers.SESSION.warn("fails to build message: ch=" + inChannel
										+ ", cause=" + PlanetUtils.unwrapThrowable(e));
				
				session.close();
			}
		}
		else {
			RpcLoggers.SESSION.warn("Target PlanetSession not found: key=" + key);

			throw new SystemException("Target PlanetSession not found: key=" + key);
		}
	}
}
