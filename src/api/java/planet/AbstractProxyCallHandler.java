package planet;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;


/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
public abstract class AbstractProxyCallHandler implements PlanetProxy, Remote, InvocationHandler {
	protected final PlanetServer m_planet;
	protected final Remote m_ref;
	
	public abstract Object call(Method method, Object... args) throws Throwable;

	protected AbstractProxyCallHandler(PlanetServer planet, Remote ref) {
		m_planet = planet;
		m_ref = ref;
	}
		
	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		Class<?> declared = method.getDeclaringClass();
		
		if ( declared.isAssignableFrom(getClass()) ) {
			return method.invoke(this, args);
		}
		else {
			return call(method, args);
		}
	}

	@Override
	public PlanetServer getPlanetServer() {
		return m_planet;
	}

	@Override
	public Remote getRemote() {
		return m_ref;
	}

	@Override
	public <T> T recast(Class<T> newType) {
		return m_planet.createProxy(m_ref.getPlanetId(), m_ref.getServantPath(), newType);
	}

	@Override
	public void removeServant() {
		RemotePlanet rplanet = m_planet.createProxy(m_ref.getPlanetId(), "/planet",
													RemotePlanet.class);
		rplanet.removeServant(m_ref.getServantPath());
	}

	@Override
	public String getPlanetId() {
		return m_ref.getPlanetId();
	}

	@Override
	public String getServantPath() {
		return m_ref.getServantPath();
	}

	@Override
	public String getTypeNames() {
		return m_ref.getTypeNames();
	}

	@Override
	public Class<?>[] getTypes() {
		return m_ref.getTypes();
	}

	@Override
	public String getUri() {
		return m_ref.getUri();
	}
	
	@Override
	public String toString() {
		return "PlanetProxy:" + m_ref;
	}

	@Override
	public int hashCode() {
		return m_ref.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		else if ( obj == null || !(obj instanceof PlanetProxy) ) {
			return false;
		}
		
		PlanetProxy other = (PlanetProxy)obj;
		return m_ref.equals(other.getRemote());
	}
}
