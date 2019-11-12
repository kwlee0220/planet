package etri.planet;


import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.concurrent.GuardedBy;

import planet.CallTimeoutException;
import planet.DisconnectionHandler;
import planet.PlanetContext;
import planet.PlanetSession;
import planet.RemoteSystemException;
import planet.Servant;
import planet.SystemException;
import planet.idl.Oneway;
import planet.idl.PlanetLocal;
import planet.transport.ClosedConnectionException;
import planet.transport.Connection;
import planet.transport.InputChannel;
import planet.transport.OutputChannel;

import etri.planet.servant.SessionBoundServant;
import etri.planet.transport.ConnectionImpl;


/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
public class PlanetSessionImpl implements PlanetSession, InputChannel.Listener {
	final PlanetServerImpl m_planet;
	final ConnectionImpl m_conn;

	private final AtomicInteger m_reqIdGen;
	private final ConcurrentMap<String,Servant> m_servants;
	private final List<ReplyWaiter> m_waiters;	// guarded by 'm_waiters'
	final Map<Integer,InputChannel> m_istreamChannels;
	final Map<Integer,StreamServer> m_strmServers;

	private final ConcurrentMap<CacheKey,Object> m_constCache;
	static final class CacheKey {
		String m_path;
		Method m_method;

		CacheKey(String path, Method method) {
			m_path = path;
			m_method = method;
		}

		public int hashCode() {
	    	int hash = 17;

	    	hash = (31 * hash) + m_path.hashCode();
	    	hash = (31 * hash) + m_method.hashCode();

	    	return hash;
		}

		public boolean equals(Object obj) {
			if ( this == obj ) {
				return true;
			}
			else if ( obj == null || obj.getClass() != CacheKey.class ) {
				return false;
			}

			CacheKey other = (CacheKey)obj;
			return m_path.equals(other.m_path) && m_method.equals(other.m_method);
		}
	}

	public PlanetSessionImpl(PlanetServerImpl planet, ConnectionImpl conn) {
		m_planet = planet;
		m_conn = conn;

		m_reqIdGen = new AtomicInteger(0);
		m_servants = new ConcurrentHashMap<String,Servant>();
		m_waiters = new ArrayList<ReplyWaiter>();
		m_istreamChannels = new HashMap<Integer,InputChannel>();
		m_strmServers = new HashMap<Integer,StreamServer>();
		m_constCache = new ConcurrentHashMap<CacheKey,Object>();
	}

	public void close() {
		m_conn.close();
	}

	public boolean isClosed() {
		return m_conn.isClosed();
	}

	public String getKey() {
		return m_conn.getId();
	}

	@Override
	public PlanetServerImpl getPlanetServer() {
		return m_planet;
	}

	@Override
	public Connection getConnection() {
		return m_conn;
	}

	@Override
	public boolean isActive() {
		return m_conn.isActive();
	}

	public Object lookupCache(String path, Method method) {
		return m_constCache.get(new CacheKey(path, method));
	}

	public void cacheResult(String path, Method method, Object result) {
		m_constCache.putIfAbsent(new CacheKey(path, method), result);
	}

	public void sendMessage(AbstractPlanetMessage msg) throws IOException {
		PlanetWriter write = new PlanetWriter(this, m_conn.allocateOutputChannel());
		msg.write(write);
		write.close();
	}

	public Object invoke(String path, Method method, Object[] args)
		throws ExecutionException, IOException, InterruptedException {
		checkOpened();

		if ( method.getDeclaringClass().isAnnotationPresent(PlanetLocal.class) ) {
			throw new SystemException("Cannot invoke remote method for 'PlanetLocal': class="
										+ method.getDeclaringClass().getName() + ", method="
										+ method.getName());
		}

		AbstractPlanetMessage msg = ( method.isAnnotationPresent(Oneway.class) )
							? new NotifyMessage(m_planet, path, method, args)
							: new CallMessage(m_planet, path, method, args);

		if ( msg instanceof CallMessage ) {
			return invokeCall((CallMessage)msg);
		}
		else if ( msg instanceof NotifyMessage ) {
			invokeNotify((NotifyMessage)msg);

			return null;
		}
		else {
			throw new SystemException("invalid message=" + msg);
		}
	}

	public void notifyReplyReceived(ReturnMessage message) {
		int reqId = message.m_header.m_reqId;

		synchronized ( m_waiters ) {
			for ( ReplyWaiter waiter: m_waiters ) {
				if ( reqId == waiter.m_requestId ) {
					waiter.signal(message);

					return;
				}
			}
		}

		if ( RpcLoggers.SESSION.isInfoEnabled() ) {
			RpcLoggers.SESSION.info("fails to find waiting invoker: id=" + reqId + ", ignored");
		}
	}

	@Override
	public Collection<String> getServantIds() {
		return m_servants.keySet();
	}	
	
	public final Servant getServant(String id) {
		return m_servants.get(id);
	}

	public String addServant(Servant servant) throws IOException {
		checkOpened();

		Object src = PlanetInternalUtils.getCallInterceptorSource(servant);
		String id = ( src != null )
					? String.valueOf(System.identityHashCode(src))
					: String.valueOf(System.identityHashCode(servant));

		SessionBoundServant caServant = null;
		if ( servant instanceof SessionBoundServant ) {
			caServant = (SessionBoundServant)servant;

			if ( equals(caServant.getBoundSession()) ) {
				return id;
			}
		}

		m_servants.put(id, servant);
		if ( caServant != null ) {
			caServant.bindToSession(this);

			if ( caServant instanceof DisconnectionHandler ) {
				m_conn.addDisconnectionHandler((DisconnectionHandler)caServant);
			}
		}

		return id;
	}

	public boolean removeServant(Object servant) {
		String id = String.valueOf(System.identityHashCode(servant));

		return removeServant(id);
	}

	public boolean removeServant(String id) {
		Object removed = m_servants.remove(id);
		if ( removed != null ) {
			if ( removed instanceof SessionBoundServant ) {
				((SessionBoundServant)removed).unbindFromSession();
			}

			return true;
		}
		else {
			return false;
		}
	}

	public OutputChannel newOutputChannel() throws IOException {
		return m_conn.allocateOutputChannel();
	}

	private static final long MAX_CHANNEL_WAIT = TimeUnit.SECONDS.toNanos(5);
	public InputChannel getInputChannel(int id) {
		long dueNanos = System.nanoTime() + MAX_CHANNEL_WAIT;

		synchronized ( m_istreamChannels ) {
			while ( true ) {
				InputChannel ichannel = m_istreamChannels.get(id);
				if ( ichannel != null ) {
					return ichannel;
				}

				long remainNanos = dueNanos - System.nanoTime();
				if ( remainNanos > 0 ) {
					try {
						m_istreamChannels.wait(remainNanos/1000000, (int)remainNanos%1000000);
					}
					catch ( InterruptedException e ) {
						throw new SystemException("fails to get input stream channel, id=" + id);
					}
				}
				else {
					throw new SystemException(String.format("channel[in,%d], timeout=%dms",
																id, MAX_CHANNEL_WAIT));
				}
			}
		}
	}

	@Override
	public void onClosed(InputChannel ichannel) {
		unregisterInputChannel(ichannel);
	}

	void registerInputChannel(InputChannel channel) {
		synchronized ( m_istreamChannels ) {
			InputChannel prev = m_istreamChannels.put(channel.getId(), channel);
			if ( prev != null ) {
				m_istreamChannels.put(channel.getId(), prev);
				
				throw new SystemException("duplicated input channel: id=" + channel.getId());
			}

			channel.setListener(this);

			m_istreamChannels.notifyAll();
		}
	}

	void unregisterInputChannel(InputChannel channel) {
		synchronized ( m_istreamChannels ) {
			m_istreamChannels.remove(channel.getId());
		}
	}

	 void onConnectionClosed(Connection conn) {
		ClosedConnectionException cce = new ClosedConnectionException("id=" + conn.getId());
		RemoteSystemException rse = new RemoteSystemException(cce);

		synchronized ( m_waiters ) {
			for ( ReplyWaiter waiter: m_waiters ) {
				waiter.signal(new ErrorMessage(waiter.m_requestId, rse));
			}
		}

		 for ( Servant servant : m_servants.values() ) {
			 if ( servant instanceof SessionBoundServant ) {
				 ((SessionBoundServant)servant).unbindFromSession();
			 }
		 }
		 m_servants.clear();

		 m_istreamChannels.clear();
		 m_strmServers.clear();
		 m_constCache.clear();

		if ( RpcLoggers.SESSION.isInfoEnabled() ) {
			RpcLoggers.SESSION.info("closed: " + this);
		}
	}

	public String toString() {
		return "session[" + m_conn.getId() + "]";
	}

	private void checkOpened() {
		if ( m_conn.isClosed() ) {
			throw new IllegalStateException("closed already: session=" + this);
		}
	}

	private Object invokeCall(CallMessage msg) throws IOException, ExecutionException,
													RemoteSystemException, InterruptedException {
		ReplyWaiter waiter = null;
		try {
			msg.m_header.m_reqId = m_reqIdGen.incrementAndGet();

			// request message send전에 waiter list에 등록을 먼저해서
			// reply가 도착했을 때, waiter list에 waiter정보 항상 있도록 한다.
			waiter = new ReplyWaiter(msg.m_header.m_reqId);
			synchronized ( m_waiters ) {
				m_waiters.add(waiter);
			}

			sendMessage(msg);

			if ( RpcLoggers.SESSION.isDebugEnabled() ) {
				RpcLoggers.SESSION.debug(String.format("sent: %s, conn[%s]", msg, this));
			}

			try {
				// wait하는 동안은 m_lock을 잠시 놓고,
				// reply가 도착하면 다신 m_lock을 잡는다.
				//
				waiter.waitForReply();
			}
			catch ( TimeoutException e ) {
				RpcLoggers.SESSION.error("call timeout: " + e.getMessage()
										+ ", msg=" + msg.toInvocationString()
										+ ", channel=" + this);

				throw new CallTimeoutException(msg.toInvocationString());
			}
		}
		finally {
			if ( waiter != null ) {
				synchronized ( m_waiters ) {
					m_waiters.remove(waiter);
				}
			}
		}

		ReturnMessage reply = waiter.m_message;

		// 반환 메시지 read 중 오류가 발생하였는지 먼저 확인한다.
		if ( reply.getReadError() != null ) {
			Throwable cause = reply.getReadError();

			if ( cause instanceof IOException ) {
				throw (IOException)cause;
			}
			else {
				throw (RuntimeException)cause;
			}
		}

		// 반환된 메시지가 ErrorMessage면, 원격 호출된 메소드가 예외를 발생한 경우이므로
		// 이를 처리한다.
		if ( reply.getClass() == ErrorMessage.class ) {
			Throwable cause = ((ErrorMessage)reply).getCause();

			if ( cause == null ) {
				cause = new SystemException("ErrorMessage has no cause Exception");
			}
			else {
				cause.fillInStackTrace();
			}

			if ( cause instanceof RemoteSystemException ) {
				throw (RemoteSystemException)cause;
			}
			else {
				throw new ExecutionException(cause);
			}
		}
		else if ( reply.getClass() == ReplyMessage.class ) {
			Object ret = ((ReplyMessage)reply).getResult();

			// Collection<?>이 반환 값 타입인 경우, 배열 타입으로 결과 값이 올 수 있기 때문에
			// 이에 대한 처리를 수행한다.
			if ( ret != null && !msg.m_method.getReturnType().isAssignableFrom(ret.getClass()) ) {
				if ( ret.getClass().isArray()
					&& msg.m_method.getReturnType().equals(Collection.class) ) {
					int length = Array.getLength(ret);

					List<Object> list = new ArrayList<Object>(length);
					for ( int i =0; i < length; ++i ) {
						list.add(Array.get(ret, i));
					}

					return list;
				}
			}

			return ((ReplyMessage)reply).getResult();
		}
		else {
			throw new SystemException("Unexpected Message[" + msg.m_header.m_reqId
										+ "]=" + reply);
		}
	}

	private void invokeNotify(NotifyMessage noti) throws IOException, InterruptedException {
		noti.m_header.m_reqId = m_reqIdGen.incrementAndGet();

		sendMessage(noti);
		if ( RpcLoggers.SESSION.isDebugEnabled() ) {
			RpcLoggers.SESSION.debug("sent: " + noti + " conn=" + this);
		}
	}

	private class ReplyWaiter {
		private int m_requestId;
		@GuardedBy("this") private boolean m_received;
		private volatile ReturnMessage m_message;

		ReplyWaiter(int requestId) {
			m_requestId = requestId;
			m_received = false;
			m_message = null;
		}

		synchronized void signal(ReturnMessage msg) {
			m_message = msg;
			m_received = true;

			this.notify();
		}

		synchronized void waitForReply() throws InterruptedException, TimeoutException {
			if ( !m_received ) {
				PlanetContext context = PlanetServerImpl.s_context.get();

				long timeout = context.getCallTimeout();
				if ( timeout > 0 ) {
					this.wait(timeout);
				}
				else {
					timeout = m_planet.getDefaultCallTimeout();
					if ( timeout > 0 ) {
						this.wait(timeout);
					}
					else {
						this.wait();
					}
				}

				if ( !m_received ) {
					throw new TimeoutException("req.id=" + m_requestId);
				}
			}
		}
	}
}
