package etri.planet;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Set;

import planet.PersistentServant;
import planet.PersistentServants;
import planet.PlanetUtils;
import planet.Servants;


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public class WrapToPersistentServant {
	public static PersistentServant wrap(Object src, String path, Class<?>... remoteTypes) {
		if ( src == null ) {
			throw new IllegalArgumentException("source object was null");
		}
		if ( path == null ) {
			throw new IllegalArgumentException("servant path was null");
		}
		if ( remoteTypes == null || remoteTypes.length == 0 ) {
			throw new IllegalArgumentException("invalid proxy types");
		}

		Set<Class<?>> typeSet = PlanetInternalUtils.getInterfaceAllRecusively(src.getClass());
//		if ( !typeSet.containsAll(Arrays.asList(remoteTypes)) ) {
//			throw new IllegalArgumentException("un-implemented remote types="
//												+ Arrays.toString(remoteTypes));
//		}

		typeSet.add(PersistentServant.class);
		Class<?>[] augmented = typeSet.toArray(new Class<?>[typeSet.size()]);

		return (PersistentServant)Proxy.newProxyInstance(src.getClass().getClassLoader(), augmented,
												new Handler(src, path, remoteTypes));
	}

	public static PersistentServant wrap(Object src, String path) {
		if ( src == null ) {
			throw new IllegalArgumentException("source object was null");
		}
		if ( path == null ) {
			throw new IllegalArgumentException("servant path was null");
		}

		Set<Class<?>> typeSet = PlanetInternalUtils.getInterfaceAllRecusively(src.getClass());
		Class<?>[] remoteTypes = typeSet.toArray(new Class<?>[typeSet.size()]);

		typeSet.add(PersistentServant.class);
		Class<?>[] augmented = typeSet.toArray(new Class<?>[typeSet.size()]);

		return (PersistentServant)Proxy.newProxyInstance(src.getClass().getClassLoader(), augmented,
												new Handler(src, path, remoteTypes));
	}

	static class Handler implements InvocationHandler {
		private final Object m_src;
		private final String m_path;
		private final Class<?>[] m_remoteTypes;

		public Handler(Object src, String path, Class<?>[] remoteTypes) {
			m_src = src;
			m_path = path;
			m_remoteTypes = remoteTypes;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			if ( Servants.GET_REMOTE_INTERFACES.equals(method) ) {
				return m_remoteTypes;
			}
			else if ( PersistentServants.GET_SERVANT_PATH.equals(method) ) {
				return m_path;
			}
			else {
				try {
					return method.invoke(m_src, args);
				}
				catch ( Exception e ) {
					throw PlanetUtils.unwrapThrowable(e);
				}
			}
		}
	}
}
