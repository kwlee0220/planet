package etri.planet;

import java.lang.reflect.Method;
import java.util.Arrays;

import planet.PlanetUtils;
import planet.RemoteSystemException;
import planet.SystemException;
import planet.idl.IgnoreLogging;

import etri.planet.thread.TimedThread;


/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
public class CallMessage extends InvocationMessage {
	public CallMessage(PlanetServerImpl planet, String path, Method method, Object[] args) {
		super(planet, MSG_CALL, path, method, args);
	}

	public CallMessage(PlanetServerImpl planet, PlanetHeader header) {
		super(planet,  header);
	}

	@Override
	public void onReceived(PlanetSessionImpl session) {
		Object servant = null;
		boolean interrupted = false;
		boolean ignoreLogging = m_method.isAnnotationPresent(IgnoreLogging.class);
		String invokeString = "...";

		if ( !ignoreLogging && RpcLoggers.CALL.isInfoEnabled() ) {
			invokeString = toInvocationString();
			TimedThread.setTaskDescription("invoking: " + invokeString);
		}

		if ( !ignoreLogging && RpcLoggers.CALL.isDebugEnabled() ) {
			RpcLoggers.CALL.debug("invoking: " + invokeString);
		}

		try {
			Object result = m_method.invoke(m_servant, m_arguments);

			if ( !ignoreLogging && RpcLoggers.CALL.isInfoEnabled() ) {
				String resultStr;
				if ( result != null ) {
					if ( result instanceof byte[] ) {
						resultStr = "binary[" + ((byte[])result).length + "]";
					}
					else if ( result.getClass().isArray() ) {
						resultStr = Arrays.asList(result).toString();
					}
					else {
						resultStr = "" + result;
					}
				}
				else {
					resultStr = "" + result;
				}
				
				RpcLoggers.CALL.info("invoked: " + invokeString + ", result=" + resultStr);
			}

			ReplyMessage reply = new ReplyMessage(m_header.m_reqId, m_method, result);
			try {
				session.sendMessage(reply);
			}
			catch ( Throwable e ) {
				RpcLoggers.CALL.error("fails to reply the call=" + invokeString
									+ ", cause=" + PlanetUtils.unwrapThrowable(e));
				
				throw e;
			}
		}
		catch ( Throwable cause ) {
			cause = PlanetUtils.unwrapThrowable(cause);

			if ( servant != null
					&& cause instanceof IllegalArgumentException
					&& cause.getMessage().equals("object is not an instance of declaring class") ) {
	System.err.println("servant interfaces...");
	for ( Class<?> intfc: PlanetInternalUtils.getInterfaceAllRecusively(servant.getClass()) ) {
		System.err.println("\t" + intfc.getName() + ", loader=" + intfc.getClassLoader());
	}
	System.err.println("called method=" + m_method);
	System.err.println("method declaring class=" + m_method.getDeclaringClass().getName()
						+ ", loader=" + m_method.getDeclaringClass().getClassLoader());
				String msg = "expected(" + m_method.getDeclaringClass().getName()
							+ "), class(" + servant.getClass().getName()
							+ "), obj(" + servant + ")";
				RpcLoggers.CALL.error("try to invoke (failed): " + invokeString);
				RpcLoggers.CALL.error(msg, cause);

				cause = new SystemException("unexpected servant type", cause);
			}
			else if ( cause instanceof InterruptedException ) {
				interrupted = true;

				cause = new SystemException("invocation interrupted");
			}
			else if ( cause instanceof Error ) {
				interrupted = true;

				cause = new SystemException("" + cause);
			}

			try {
				if ( cause instanceof SystemException && !(cause instanceof RemoteSystemException) ) {
					cause = new RemoteSystemException((SystemException)cause);
				}

				if ( !ignoreLogging && RpcLoggers.CALL.isInfoEnabled() ) {
					RpcLoggers.CALL.info("invoked: " + invokeString + ", throws=" + cause);
				}

				ErrorMessage error = new ErrorMessage(m_header.m_reqId, cause);
				session.sendMessage(error);
			}
			catch ( Throwable ignored ) { }
		}
		finally {
			if ( interrupted ) {
				Thread.currentThread().interrupt();
			}
		}
	}

	@Override
	public void onErrorReadingPayload(PlanetSessionImpl session, Throwable cause) {
		if ( cause instanceof RemoteSystemException ) { }
		if ( cause instanceof SystemException ) {
			cause = new RemoteSystemException((SystemException)cause);
		}
		else {
			cause = new RemoteSystemException(new SystemException("" + cause));
		}

		try {
			ErrorMessage error = new ErrorMessage(m_header.m_reqId, cause);
			session.sendMessage(error);
		}
		catch ( Throwable e ) {
			RpcLoggers.CALL.error("fails to send ERROR message, cause=" + e, e);
		}
	}

	public String toString() {
		return "Call[" + m_path + "#" + m_method.getName() + "()]";
	}

//	private boolean isHeavyCall(Method method) {
//		String name = method.getName();
//		Class<?>[] paramTypes = method.getParameterTypes();
//
//		if ( name.equals("getBytes") && paramTypes.length == 1 && paramTypes[0] == int.class ) {
//			return true;
//		}
//		else if ( name.equals("captureImage") && paramTypes.length == 0 ) {
//			return true;
//		}
//
//		return false;
//	}
}