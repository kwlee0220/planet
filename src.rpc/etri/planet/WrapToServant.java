package etri.planet;

import java.lang.reflect.Proxy;
import java.util.Set;

import planet.Servant;


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public class WrapToServant {
	public static Servant wrap(Object src, Class<?>... remoteTypes) {
		if ( src == null ) {
			throw new IllegalArgumentException("source object was null");
		}
		if ( remoteTypes == null || remoteTypes.length == 0 ) {
			throw new IllegalArgumentException("invalid proxy types");
		}

		Set<Class<?>> typeSet = PlanetInternalUtils.getInterfaceAllRecusively(src.getClass());
		typeSet.add(Servant.class);
		Class<?>[] augmented = typeSet.toArray(new Class<?>[typeSet.size()]);
		
		return (Servant)Proxy.newProxyInstance(src.getClass().getClassLoader(), augmented,
												new ServantCallInterceptor(src, remoteTypes));
	}

	public static Servant wrap(Object src, String path) {
		if ( src == null ) {
			throw new IllegalArgumentException("source object was null");
		}
		if ( path == null ) {
			throw new IllegalArgumentException("servant path was null");
		}

		Set<Class<?>> typeSet = PlanetInternalUtils.getInterfaceAllRecusively(src.getClass());
		Class<?>[] remoteTypes = typeSet.toArray(new Class<?>[typeSet.size()]);

		typeSet.add(Servant.class);
		Class<?>[] augmented = typeSet.toArray(new Class<?>[typeSet.size()]);

		return (Servant)Proxy.newProxyInstance(src.getClass().getClassLoader(), augmented,
												new ServantCallInterceptor(src, remoteTypes));
	}
}
