package etri.planet;



import java.lang.reflect.Method;

import planet.PlanetUtils;
import planet.SystemException;
import planet.idl.IgnoreLogging;

import org.apache.log4j.Logger;

import etri.planet.thread.TimedThread;


public class NotifyMessage extends InvocationMessage {
	private static final Logger s_logger = Logger.getLogger(NotifyMessage.class);
	
	public NotifyMessage(PlanetServerImpl planet, String path, Method method, Object[] args) {
		super(planet, MSG_NOTIFY, path, method, args);
	}
	
	public NotifyMessage(PlanetServerImpl planet, PlanetHeader pheader) {
		super(planet, pheader);
	}

	@Override
	public void onReceived(PlanetSessionImpl session) {
		Object servant = null;
		boolean ignoreLogging = m_method.isAnnotationPresent(IgnoreLogging.class);
		
		String invokString = RpcUtils.toInvocationString(m_method, m_arguments);
		TimedThread.setTaskDescription("invoking: " + invokString);
		
		if ( !ignoreLogging && RpcLoggers.NOTI.isDebugEnabled() ) {
			RpcLoggers.NOTI.debug("notifying: " + invokString);
		}

		try {
			servant = m_planet.getServant(m_path);
			Object result = m_method.invoke(servant, m_arguments);
			
			if ( !ignoreLogging && RpcLoggers.NOTI.isInfoEnabled() ) {
				RpcLoggers.NOTI.info("notified: " + invokString + ", result=" + result);
			}
		}
		catch ( Throwable cause ) {
			cause = PlanetUtils.unwrapThrowable(cause);
			
			if ( IllegalArgumentException.class.equals(cause.getClass())
					&& cause.getMessage().equals("object is not an instance of declaring class") ) {
				String msg = "expected(" + m_method.getDeclaringClass().getName()
							+ "), object("+ servant + ")";
				s_logger.error(msg, cause);
			}
			
			if ( cause.getClass().isAssignableFrom(SystemException.class) ) {
				RpcLoggers.NOTI.error("failed(system) " + invokString + ", cause=" + cause);
			}
			else {
				RpcLoggers.NOTI.error("failed(user) " + invokString + ", cause=" + cause);
			}
		}
	}

	@Override
	public void onErrorReadingPayload(PlanetSessionImpl session, Throwable cause) {
	}
	
	public String toString() {
		return "Notify[" + m_path + "#" + m_method.getName() + "()]";
	}
}