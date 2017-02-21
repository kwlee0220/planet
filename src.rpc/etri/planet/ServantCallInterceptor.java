package etri.planet;

import java.lang.reflect.Method;

import planet.CallInterceptor;
import planet.Servants;



/**
 *
 * @author Kang-Woo Lee
 */
public class ServantCallInterceptor implements CallInterceptor {
	private final Object m_src;
	private final Class<?>[] m_remoteTypes;

	ServantCallInterceptor(Object src, Class<?>[] remoteTypes) {
		m_src = src;
		m_remoteTypes = remoteTypes;
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		if ( Servants.GET_REMOTE_INTERFACES.equals(method) ) {
			return m_remoteTypes;
		}
		else {
			return method.invoke(m_src, args);
		}
	}

	@Override
	public Object getSourceObject() {
		return m_src;
	}
}