package etri.planet;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import planet.ConnectionInfo;
import planet.Directory;
import planet.PersistentServant;
import planet.PlanetContext;
import planet.PlanetServer;
import planet.PlanetServerListener;
import planet.PlanetSession;
import planet.PlanetUtils;
import planet.Remote;
import planet.RemotePlanet;
import planet.RemoteReference;
import planet.Servant;
import planet.ServantExistsException;
import planet.ServantNotFoundException;
import planet.SystemException;
import planet.UndeclaredTypeException;
import planet.transport.Connection;
import planet.transport.TransportManager;

import org.apache.log4j.Logger;

import etri.planet.servant.MappedDirectory;
import etri.planet.servant.ServantManager;
import etri.planet.servant.ServantUtils;
import etri.planet.thread.AbstractTimedExecutor;
import etri.planet.transport.ConnectionImpl;
import etri.planet.transport.TransportManagerImpl;

/**
 *
 * @author Kang-Woo Lee
 */
public class PlanetServerImpl implements PlanetServer, RemotePlanet, PersistentServant {
	private static final Logger s_logger = Logger.getLogger("PLANET.SERVER");

	public static final int DEFAULT_PORT = 0;
    private static final int DEFAULT_THREADS = 8;
    private static final int DELAYED_Q_LENGTH = 128;

	private static final int NOT_STARTED = 0;
	private static final int RUNNING = 1;
	private static final int FINISHED = 2;

    private volatile String m_id;
    private volatile String m_host;
    private volatile int m_port;
	private volatile int m_state;

	private volatile long m_defaultCallTimeout = 0;

	private final TransportManagerImpl m_transport;
	private final PlanetSessionManager m_sessionManager;
	private final ServantManager m_servantManager;
	private final PlanetProxyFactory m_factory;
	private final PlanetClassLoader m_classLoader;
	private volatile ExecutorService m_executor;
	private volatile boolean m_localExecutorFlag = false;
	private volatile int m_delayedQLength = DELAYED_Q_LENGTH;
	private final List<String> m_delayedServantList = new LinkedList<String>();

	public PlanetServerImpl() {
		this(null, new MappedDirectory(ROOT_PATH));
	}

	public PlanetServerImpl(String id) {
		this(id, new MappedDirectory(ROOT_PATH));
	}

	public PlanetServerImpl(String id, Directory rootDir) {
		if ( rootDir == null ) {
			throw new IllegalArgumentException("Root ServantDirectory was null.");
		}

		m_host = PlanetUtils.LOCAL_HOST;
		m_port = DEFAULT_PORT;
		m_id = id;

		m_transport = new TransportManagerImpl(id);
		m_sessionManager = new PlanetSessionManager(this);
		m_factory = new PlanetProxyFactory(this);
		m_classLoader = new PlanetClassLoader();

		m_servantManager = new ServantManager(rootDir);

    	m_state = NOT_STARTED;
	}

	@Override
	public synchronized void start() throws IOException {
//		GregorianCalendar now = new GregorianCalendar();
//		GregorianCalendar expired = new GregorianCalendar(2007, 3, 15);
//		if ( now.after(expired) ) {
//			System.exit(-1);
//		}

		// 사용자가 명시적으로 executor를 설정하지 않을 수도 있기 때문.
		if ( m_executor == null ) {
			m_executor = Executors.newScheduledThreadPool(DEFAULT_THREADS);
			m_localExecutorFlag = true;
		}

		m_servantManager.addServant(this);

		m_sessionManager.start(m_transport);
		m_port = m_transport.start(m_port, m_executor, m_sessionManager);
		if ( !m_transport.getId().equals(m_id) ) {
			m_id = m_transport.getId();
		}

    	m_state = RUNNING;

    	Runtime.getRuntime().addShutdownHook(new Thread() {
    		public void run() {
    			if ( s_logger.isInfoEnabled() ) {
    				s_logger.info("start: ShutdownHook for PlanetServer...");
    			}

    			synchronized ( m_shutdownHooks ) {
    				while ( m_shutdownHooks.size() > 0 ) {
    					try {
							Runnable hook = m_shutdownHooks.remove(0);
							hook.run();
						}
						catch ( Throwable e ) {
							s_logger.error(e);
						}
    				}
    			}

    			PlanetServerImpl.this.stop();
    			if ( s_logger.isInfoEnabled() ) {
    				s_logger.info("finished: ShutdownHook for PlanetServer...");
    			}
    		}
    	});

    	if ( m_executor instanceof AbstractTimedExecutor ) {
    		AbstractTimedExecutor texec = (AbstractTimedExecutor)m_executor;

    		int max = texec.getMaxThreadCount();
    		if ( max == Integer.MAX_VALUE ) {
    			max = -1;
    		}

    		System.out.printf("started: PlanetServer[%s:%d, "
					+ "heartbeat=%ds, thread[count=%d, max=%d, timeout=%ds], call.timeout=%ds\n",
					getPlanetServerHost(), getPlanetServerPort(),
					(getHeartbeatInterval() > 0) ? getHeartbeatInterval()/1000 : -1,
					texec.getThreadCount(), max,
					(texec.getThreadTimeout() > 0) ? texec.getThreadTimeout()/1000 : -1,
					(getDefaultCallTimeout() > 0) ? getDefaultCallTimeout()/1000 : -1);
    	}
    	else if ( m_executor instanceof ThreadPoolExecutor ) {
    		ThreadPoolExecutor pool = (ThreadPoolExecutor)m_executor;

    		int max = pool.getMaximumPoolSize();
    		if ( max == Integer.MAX_VALUE ) {
    			max = -1;
    		}

    		System.out.printf("started: PlanetServer[%s:%d, "
					+ "heartbeat=%ds, thread[core=%d, max=%d], call.timeout=%ds\n",
					getPlanetServerHost(), getPlanetServerPort(),
					(getHeartbeatInterval() > 0) ? getHeartbeatInterval()/1000 : -1,
					pool.getCorePoolSize(), max,
					(getDefaultCallTimeout() > 0) ? getDefaultCallTimeout()/1000 : -1);
    	}
    	else {
    		System.out.printf("started: PlanetServer[%s:%d, "
					+ "heartbeat=%ds, thread[count=unknown], call.timeout=%ds\n",
					getPlanetServerHost(), getPlanetServerPort(),
					(getHeartbeatInterval() > 0) ? getHeartbeatInterval()/1000 : -1,
					(getDefaultCallTimeout() > 0) ? getDefaultCallTimeout()/1000 : -1);
		}
	}

	private List<Runnable> m_shutdownHooks = new ArrayList<Runnable>();
	public void addShutdownHook(Runnable hook) {
		synchronized ( m_shutdownHooks ) {
			m_shutdownHooks.add(hook);
		}
	}

	@Override
	public synchronized void stop() {
    	if ( m_state != RUNNING ) {
    		return;
    	}

    	m_transport.stop();
    	m_sessionManager.stop();

    	m_state = FINISHED;

    	if ( m_localExecutorFlag ) {
    		m_executor.shutdown();
    	}
	}

	@Override
	public synchronized boolean isRunning() {
		return m_state == RUNNING;
	}

	@Override
	public String getId() {
		return m_id;
	}

	@Override
	public String getPlanetServerHost() {
		return m_host;
	}

	@Override
	public synchronized void setPlanetServerHost(String host) throws SystemException {
		if ( host == null ) {
			throw new IllegalArgumentException("host was null");
		}

		if ( m_state != NOT_STARTED ) {
			throw new SystemException("PlanetServer has been started already");
		}

		m_host = PlanetUtils.resolveLocalhost(host);
		m_transport.setHost(m_host);
	}

	@Override
	public int getPlanetServerPort() {
		return m_port;
	}

	@Override
	public synchronized void setPlanetServerPort(int port) {
		if ( m_state != NOT_STARTED ) {
			throw new SystemException("PlanetServer has been started already");
		}

		m_port = port;
	}
	
	public void setDelayedServantQueue(int qLength) {
		m_delayedQLength = qLength;
	}

	@Override
	public Object createProxy(String planetUri) {
		return createProxy(RemoteReference.createReference(this, planetUri));
	}

	@Override
	public Object createProxy(Remote rmt) {
		return createProxy(rmt.getPlanetId(), rmt.getServantPath(), rmt.getTypes());
	}

	@Override @SuppressWarnings("unchecked")
	public <T> T createProxy(String phn, String path, Class<T> type) {
		if ( type == null ) {
			throw new IllegalArgumentException("proxy type was null");
		}

		return (T)createProxy(phn, path, new Class<?>[]{type});
	}

	@Override
	public Object createProxy(String phn, String path, Class<?>... types) {
		return m_factory.newProxy(phn, path, types);
	}

	@Override
	public Object createLocalProxy(Object obj, String path, Class<?>... types) {
		return m_factory.createLocalPlanetProxy(obj, path, types);
	}

	public TransportManager getTransportManager() {
		return m_transport;
	}

	@Override
	public PlanetSession getPlanetSession(String remoteKey, boolean create)
		throws IOException, InterruptedException {
		if ( remoteKey == null ) {
			throw new IllegalArgumentException("remoteKey was null");
		}

		return m_sessionManager.getPlanetSession(remoteKey, create);
	}

	@Override
	public PlanetSession getPlanetSession(Remote remote, boolean create)
		throws IOException, InterruptedException {
		if ( remote == null ) {
			throw new IllegalArgumentException("remote was null");
		}

		return m_sessionManager.getPlanetSession(remote.getPlanetId(), create);
	}

	@Override
	public boolean existsPlanetSession(String remoteKey) {
		if ( remoteKey == null ) {
			throw new IllegalArgumentException("remoteHost was null");
		}

		return m_transport.existsConnection(remoteKey);
	}
	
	@Override
	public Directory getRootDirectory() {
		return m_servantManager.getRootDirectory();
	}

	@Override
	public Servant getServant(String path) throws ServantNotFoundException {
		if ( path == null ) {
			return m_servantManager.getRootDirectory();
		}

		if ( path.startsWith("/conn/") ) {
			List<String> nodeList = ServantUtils.parseServantPath(path);

			PlanetSessionImpl session = getPlanetSessionOfServantPath(nodeList);
			if ( session != null ) {
				Servant servant = session.getServant(nodeList.get(2));
				if ( servant != null ) {
					return servant;
				}
			}

			throw new ServantNotFoundException("path=" + path);
		}
		else {
			return m_servantManager.getServant(path);
		}
	}

	private PlanetSessionImpl getPlanetSessionOfServantPath(List<String> nodeList) {
		String node = nodeList.get(1);

		int idx = node.indexOf(':');
		if ( idx < 0 ) {
			throw new IllegalArgumentException("invalid connection servant path="
												+ ServantUtils.toPathString(nodeList));
		}

		try {
			return m_sessionManager.getPlanetSession(node, false);
		}
		catch ( NumberFormatException e ) {
			throw new IllegalArgumentException("invalid servant path="
											+ ServantUtils.toPathString(nodeList));
		}
		catch ( IOException neverHappens ) {
			throw new SystemException("Should not be here!!, cause=" + neverHappens);
		}
		catch ( InterruptedException ignored ) {
			return null;
		}
	}

	@Override
	public boolean existsServant(String path) {
		if ( path == null ) {
			return true;
		}
		else if ( path.startsWith("/conn/") ) {
			List<String> nodeList = ServantUtils.parseServantPath(path);

			PlanetSessionImpl session = getPlanetSessionOfServantPath(nodeList);
			return session != null && session.getServant(nodeList.get(2)) != null;
		}
		else {
			return m_servantManager.existsServant(path);
		}
	}

	@Override
	public void addServant(PersistentServant servant)
		throws ServantNotFoundException, ServantExistsException {
		if ( servant == null ) {
			throw new IllegalArgumentException("servant was null");
		}

		m_servantManager.addServant(servant);
	}

	@Override
	public void removeServant(String path) {
//		if ( delayed ) {
//			synchronized ( m_delayedServantList ) {
//				m_delayedServantList.add(path);
//				if ( m_delayedServantList.size() <= m_delayedQLength ) {
//					return;
//				}
//				
//				path = m_delayedServantList.remove(0);
//			}
//		}
		
		if ( path.startsWith("/conn/") ) {
			List<String> nodeList = ServantUtils.parseServantPath(path);

			PlanetSessionImpl session = getPlanetSessionOfServantPath(nodeList);
			if ( session != null ) {
				session.removeServant(nodeList.get(2));
			}
		}
		else {
			m_servantManager.removeServant(path);
		}
	}

	@Override
	public long getHeartbeatInterval() {
		return m_transport.getHeartbeatInterval();
	}

	@Override
    public void setHeartbeatInterval(long interval) {
    	m_transport.setHeartbeatInterval(interval);
    }

	@Override
	public int getConnectTimeout() {
		return m_transport.getConnectTimeout();
	}

	@Override
	public void setConnectTimeout(int timeout) throws IllegalArgumentException {
		m_transport.setConnectTimeout(timeout);
	}

	@Override
	public long getDefaultCallTimeout() {
		return m_defaultCallTimeout;
	}

	@Override
	public void setDefaultCallTimeout(long timeout) {
    	if ( timeout == 0 ) {
    		throw new IllegalArgumentException("timeout should not be zero");
    	}

    	m_defaultCallTimeout = timeout;

    	if ( s_logger.isDebugEnabled() ) {
    		if ( timeout > 0 ) {
    			s_logger.debug("set call default timeout=" + timeout + "msecs");
    		}
    		else {
    			s_logger.debug("unset call default timeout");
    		}
    	}
	}

	public void createDefaultPlanetExecutor() {
		m_executor = Executors.newScheduledThreadPool(DEFAULT_THREADS);
	}

	@Override
	public Connection getConnection(String planetId, boolean create) throws IOException,
			InterruptedException {
		return m_transport.getConnection(planetId, create);
	}

	@Override
	public ConnectionInfo[] getConnectionInfos() {
		ConnectionImpl[] impls = m_transport.getConnections();
		
		ConnectionInfo[] infos = new ConnectionInfo[impls.length];
		for ( int i =0; i < infos.length; ++i ) {
			final ConnectionImpl conn = impls[i];
			
			infos[i] = new ConnectionInfo();
			infos[i].m_id = conn.getId();
			infos[i].m_active = conn.isActive();
		}
		
		return infos;
	}

	@Override
	public ExecutorService getPlanetExecutor() {
		return m_executor;
	}

	@Override
	public void setPlanetExecutor(ExecutorService executor) {
		m_executor = executor;
	}

	@Override
	public void addClassLoader(String id, ClassLoader loader) {
		m_classLoader.addClassLoader(id, loader);
	}

	@Override
	public void removeClassLoader(String id) {
		m_classLoader.removeClassLoader(id);
	}

	public void execute(Runnable task) {
		m_executor.execute(task);
	}

	@Override
	public Class<?> loadClass(String className) throws UndeclaredTypeException {
		try {
			return m_classLoader.loadClass(className);
		}
		catch ( ClassNotFoundException e ) {
			throw new UndeclaredTypeException(className);
		}
	}

	public boolean addPlanetServerListener(PlanetServerListener listener) {
		if ( listener == null ) {
			throw new IllegalArgumentException("listener was null");
		}

		return m_sessionManager.addPlanetServerListener(listener);
	}

	public boolean removePlanetServerListener(PlanetServerListener listener) {
		if ( listener == null ) {
			throw new IllegalArgumentException("listener was null");
		}

		return m_sessionManager.removePlanetServerListener(listener);
	}

	private static final Class<?>[] REMOTE_INTERFACES = new Class<?>[]{RemotePlanet.class};
    public Class<?>[] getRemoteInterfaces() {
    	return REMOTE_INTERFACES;
    }

	public String getServantPath() {
		return "/planet";
	}

	/**
	 * 현 PlanetServer에서 사용되는 쓰레드 풀이 보유한 전체 쓰레드 수.
	 *
	 * @return	쓰레드 수.
	 */
	@Override
	public int getThreadCount() {
		if ( m_executor instanceof ThreadPoolExecutor ) {
			return ((ThreadPoolExecutor)m_executor).getPoolSize();
		}
		else if ( m_executor instanceof AbstractTimedExecutor ) {
			return ((AbstractTimedExecutor)m_executor).getThreadCount();
		}
		else {
			return -1;
		}
	}

	/**
	 * 현 PlanetServer에서 사용하는 쓰레드들 중에서 현재 사용 중인 쓰레드의 수.
	 *
	 * @return	쓰레드 수.
	 */
	@Override
	public int getActiveThreadCount() {
		if ( m_executor instanceof ThreadPoolExecutor ) {
			return ((ThreadPoolExecutor)m_executor).getActiveCount();
		}
		else if ( m_executor instanceof AbstractTimedExecutor ) {
			return ((AbstractTimedExecutor)m_executor).getActiveThreadCount();
		}
		else {
			return -1;
		}
	}

	@Override
	public void setConnectionDescription(String description) {
		PlanetContext context = s_context.get();
		context.getPlanetSession().getConnection().setDescription(description);
	}

	@Override
	public void closeSession(String id) {
		if ( id == null ) {
			throw new IllegalArgumentException("session key was null");
		}		
		try {
			PlanetSession session = m_sessionManager.getPlanetSession(id, false);
			session.close();
		}
		catch ( Throwable e ) {
			throw new SystemException("fails to access session[id=" + id + "]");
		}
	}

	@Override
	public Collection<String> getServantIdsOfSession(String id) {
		if ( id == null ) {
			throw new IllegalArgumentException("session key was null");
		}		
		try {
			PlanetSession session = m_sessionManager.getPlanetSession(id, false);
			return session.getServantIds();
		}
		catch ( Throwable e ) {
			throw new SystemException("fails to access session[id=" + id + "]");
		}
	}

	@Override
	public Collection<String> getServantsIds() {
		return null;
	}

	@Override
	public Collection<String> getSessionIds() {
		return m_sessionManager.getPlanetSessionKeys();
	}
}
