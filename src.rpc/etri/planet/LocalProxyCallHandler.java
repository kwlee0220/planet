package etri.planet;

import java.lang.reflect.Method;

import planet.AbstractProxyCallHandler;
import planet.PlanetSession;
import planet.PlanetUtils;
import planet.RemoteReference;


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
class LocalProxyCallHandler extends AbstractProxyCallHandler {
	private final Object m_obj;
	
	LocalProxyCallHandler(PlanetServerImpl planet, RemoteReference ref, Object obj) {
		super(planet, ref);
		
		m_obj = obj;
	}

	@Override
	public PlanetSession getPlanetSession(boolean create) {
		return null;
	}

	@Override
	public Object call(Method method, Object... args) throws Throwable {
		try {
			return method.invoke(m_obj, args);
		}
		catch ( Exception e ) {
			throw PlanetUtils.unwrapThrowable(e);
		}
	}
}