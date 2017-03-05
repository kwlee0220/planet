package etri.planet;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import planet.AbstractProxyCallHandler;
import planet.PlanetSession;
import planet.RemoteReference;
import planet.SystemException;
import planet.UndeclaredUserException;
import planet.idl.Const;
import planet.transport.ClosedConnectionException;


class RemoteProxyCallHandler extends AbstractProxyCallHandler {
	private static final Object NULL = new Object();
	
	private volatile PlanetSessionImpl m_session;
	
	RemoteProxyCallHandler(PlanetServerImpl planet, RemoteReference ref) {
		super(planet, ref);
		
		m_session = null;
	}

	@Override
	public PlanetSession getPlanetSession(boolean create) {
		if ( m_session != null ) {
			return m_session;
		}
		else {
			try {
				return m_planet.getPlanetSession(m_ref, create);
			}
			catch ( Exception e ) {
				RpcLoggers.PROXY.warn("" + e);
				
				return null;
			}
		}
	}

	@Override
	public Object call(Method method, Object... args) throws Throwable {
		if ( m_session == null || m_session.isClosed() ) {
			try {
				m_session = (PlanetSessionImpl)m_planet.getPlanetSession(m_ref,
									m_session == null ? true : m_session.isActive());
			}
			catch ( IOException e ) {
				throw new ClosedConnectionException("fails to connect: ref=" + m_ref.getPlanetId()
													+ ", cause=" + e);
			}
			
			if ( m_session == null || m_session.isClosed() ) {
				throw new ClosedConnectionException("key=" + m_ref.getPlanetId());
			}
		}
		
		boolean isConst = method.isAnnotationPresent(Const.class);
		if ( isConst ) {
			Object cached = m_session.lookupCache(m_ref.getServantPath(), method);
			if ( cached != null ) {
				if ( RpcLoggers.PROXY.isDebugEnabled() ) {
					RpcLoggers.PROXY.debug("cache hit: method=" + method + ", value=" + cached);
				}
				
				return (cached == NULL) ? null : cached;
			}
		}
		
		try {
			if ( RpcLoggers.PROXY.isInfoEnabled() ) {
				RpcLoggers.PROXY.info("invoking remote: "
									+ RpcUtils.toInvocationString(method, args));
			}

			Object result = m_session.invoke(m_ref.getServantPath(), method, args);
			if ( isConst ) {
				m_session.cacheResult(m_ref.getServantPath(), method,
										result==null ? NULL : result);
			}
			
			return result;
		}
		catch ( ExecutionException e ) {
			Throwable cause = e.getCause();
			
			Class<?> causerCls = cause.getClass();
			if ( ALLOWED_UNDECLARED_EXCEPTIONS.contains(causerCls) ) {
				throw cause;
			}
			
			for ( Class<?> exceptCls: method.getExceptionTypes() ) {
				if ( exceptCls.isAssignableFrom(causerCls) ) {
					throw cause;
				}
			}
			
			throw new UndeclaredUserException("" + cause);
		}
		catch ( IOException e ) {
			throw new SystemException("" + e);
		}
	}
	
	private static final Set<Class> ALLOWED_UNDECLARED_EXCEPTIONS = new HashSet<Class>();
	static {
		ALLOWED_UNDECLARED_EXCEPTIONS.add(IllegalArgumentException.class);
		ALLOWED_UNDECLARED_EXCEPTIONS.add(IllegalStateException.class);
	}
}