package planet;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * <code>RemoteReference</code>는 PlanetServer 구현 내부에서 사용하는
 * {@link Remote} 인터페이스의 구현물이다.
 *
 * @author Kang-Woo Lee (ETRI)
 */
public class RemoteReference implements Remote {
	public static final Logger CLSLDR = LoggerFactory.getLogger("PLANET.RPC.CLASS");
	
	private final PlanetServer m_planet;

	private final String m_url;
	private final String m_planetId;
	private final String m_path;
	private volatile String m_typeNames;
	private volatile Class<?>[] m_types;

	private int m_hashCode = 0;

	public static Method METHOD_GET_URI;
	public static Method METHOD_PLANETID;
	public static Method METHOD_SERVANT_PATH;
	public static Method METHOD_TYPE_NAMES;
	public static Method METHOD_TYPES;

	static {
		try {
			METHOD_GET_URI = Remote.class.getDeclaredMethod("getUri", (Class[])null);
			METHOD_PLANETID = Remote.class.getDeclaredMethod("getPlanetId", (Class[])null);
			METHOD_SERVANT_PATH = Remote.class.getDeclaredMethod("getServantPath", (Class[])null);
			METHOD_TYPE_NAMES = Remote.class.getDeclaredMethod("getTypeNames", (Class[])null);
			METHOD_TYPES = Remote.class.getDeclaredMethod("getTypes", (Class[])null);
		}
		catch ( Exception e ) {
			System.err.println(e);
		}
	}

	public static RemoteReference createLocalReference(PlanetServer planet, PersistentServant servant) {
		return createLocalReference(planet, servant.getServantPath(), servant.getRemoteInterfaces());
	}

	public static RemoteReference createLocalReference(PlanetServer planet, String path,
														String typeNames) {
		return new RemoteReference(planet, planet.getId(), path, typeNames);
	}

	public static RemoteReference createLocalReference(PlanetServer planet, String path,
														Class<?>... types) {
		String typeNames = PlanetUtils.concatTypeNames(types);
		return new RemoteReference(planet, planet.getId(), path, typeNames);
	}

	public static RemoteReference createReference(PlanetServer planet, String remotePlanetId,
												String path, String typeNames) {
		return new RemoteReference(planet, remotePlanetId, path, typeNames);
	}

	public static RemoteReference createReference(PlanetServer planet, String remotePlanetId,
												String path, Class<?>... types) {
		String typeNames = PlanetUtils.concatTypeNames(types);
		return new RemoteReference(planet, remotePlanetId, path, typeNames);
	}

	public static RemoteReference createReference(PlanetServer planet, String refUrl) {
		return PlanetUtils.parsePlanetUrl(planet, refUrl);
	}

	public Object createProxy() {
		return m_planet.createProxy(this);
	}

	public <T> T createProxy(Class<T> type) {
		if ( m_types != null ) {
			for ( Class<?> remoteType: m_types ) {
				if ( type.isAssignableFrom(remoteType) ) {
					return (T)m_planet.createProxy(this);
				}
			}
		}

		return m_planet.createProxy(m_planetId, m_path, type);
	}

	private RemoteReference(PlanetServer planet, String planetId, String path, String typeNames) {
		m_planet = planet;

		m_planetId = planetId;
		m_path = path;
		m_url = "planet://" + m_planetId + m_path + "?types=" + typeNames;
		m_typeNames = typeNames;
	}

	public PlanetServer getPlanetServer() {
		return m_planet;
	}

	public String getUri() {
		return m_url;
	}

	public String getPlanetId() {
		return m_planetId;
	}

	public String getServantPath() {
		return m_path;
	}

	public String getTypeNames() {
		if ( m_typeNames == null ) {
			m_typeNames = PlanetUtils.concatTypeNames(m_types);
		}

		return m_typeNames;
	}

	public Class<?>[] getTypes() {
		if ( m_types == null ) {
			List<Class<?>> classList = new ArrayList<Class<?>>();
			for ( String typeName: PlanetUtils.splitTypeNames(m_typeNames) ) {
				try {
					classList.add(m_planet.loadClass(typeName));
				}
				catch ( UndeclaredTypeException ignored ) {
					CLSLDR.warn("failed to load class: id=" + m_typeNames);
				}
			}

			m_types = classList.toArray(new Class[classList.size()]);
		}

		return m_types;
	}

	public String toString() {
		return getUri();
	}

	public int hashCode() {
		if ( m_hashCode == 0 ) {
	    	m_hashCode = 17;

	    	m_hashCode = (31 * m_hashCode) + m_url.hashCode();
	    	m_hashCode = (31 * m_hashCode) + Arrays.hashCode(m_types);
		}

		return m_hashCode;
	}

	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		else if ( obj == null || !(obj instanceof Remote) ) {
			return false;
		}

		Remote other = (Remote)obj;
		return ( m_planetId.equals(other.getPlanetId())
					&& m_path.equals(other.getServantPath()) );
	}
}