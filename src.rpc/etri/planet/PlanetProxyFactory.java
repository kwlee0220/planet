package etri.planet;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Set;

import planet.InvalidArgumentException;
import planet.PlanetProxy;
import planet.PlanetUtils;
import planet.Remote;
import planet.RemoteReference;
import planet.Servant;


/**
 *
 * @author Kang-Woo Lee
 */
public class PlanetProxyFactory {
	private final PlanetServerImpl m_planet;

	public PlanetProxyFactory(PlanetServerImpl planet) {
		m_planet = planet;
	}

	public PlanetProxy newProxy(String key, String path, Class<?>... types) {
		if ( key == null ) {
			throw new InvalidArgumentException("remote PlanetServer id was null");
		}
		if ( path == null ) {
			throw new InvalidArgumentException("servant path was null");
		}
		if ( types == null ) {
			throw new InvalidArgumentException("fails to create proxy: types are null");
		}

		key = PlanetUtils.resolovePlanetKey(key);

		if ( key.equals(m_planet.getId()) ) {
			Servant servant = m_planet.getServant(path);
			return createLocalPlanetProxy(servant, path, types);
		}
		else {
			return createRemotePlanetProxy(key, path, types);
		}
	}

	public PlanetProxy createPlanetProxy(Remote remote) {
		return newProxy(remote.getPlanetId(), remote.getServantPath(), remote.getTypes());
	}

	private PlanetProxy createRemotePlanetProxy(String planetId, String path,
												Class<?>... remoteTypes) {
		if ( planetId == null ) {
			throw new IllegalArgumentException("target planet key was null");
		}
		if ( path == null ) {
			throw new IllegalArgumentException("servant path was null");
		}
		if ( remoteTypes == null ) {
			throw new IllegalArgumentException("fails to create proxy: remoteTypes was null");
		}

		Class<?>[] proxyTypes = PlanetInternalUtils.concatClasses(remoteTypes, PlanetProxy.class, Remote.class);
		RemoteReference ref = RemoteReference.createReference(m_planet, planetId, path, remoteTypes);

		RemoteProxyCallHandler handler = new RemoteProxyCallHandler(m_planet, ref);
		return createProxy(proxyTypes, handler);
	}

	public PlanetProxy createLocalPlanetProxy(Object src, String path, Class<?>... remoteTypes) {
		if ( src == null ) {
			throw new InvalidArgumentException("source object was null");
		}
		if ( path == null ) {
			throw new InvalidArgumentException("servant path was null");
		}
		if ( remoteTypes == null || remoteTypes.length == 0 ) {
			throw new InvalidArgumentException("invalid proxy types");
		}

		if ( src instanceof PlanetProxy || src instanceof Remote ) {
			return (PlanetProxy)src;
		}
		else {
			remoteTypes = PlanetInternalUtils.collectRemoteTypeAll(remoteTypes);
			RemoteReference ref = RemoteReference.createLocalReference(m_planet, path, remoteTypes);

			Set<Class<?>> typeSet = PlanetInternalUtils.getInterfaceAllRecusively(src.getClass());
			typeSet.addAll(Arrays.asList(remoteTypes));
			typeSet.add(PlanetProxy.class);
			typeSet.add(Remote.class);
			Class<?>[] augmented = typeSet.toArray(new Class<?>[typeSet.size()]);

			LocalProxyCallHandler handler = new LocalProxyCallHandler(m_planet, ref, src);
			return createProxy(augmented, handler);
		}
	}
	
	private PlanetProxy createProxy(Class<?>[] augmented, InvocationHandler handler) {
		IllegalArgumentException fault = null;
		ClassLoader prevLoader = null;
		for ( Class<?> cls: augmented ) {
			ClassLoader loader = cls.getClassLoader();
			if ( loader != null && !loader.equals(prevLoader) ) {
				try {
					return (PlanetProxy)Proxy.newProxyInstance(cls.getClassLoader(), augmented,
																handler);
				}
				catch ( IllegalArgumentException e ) {
					fault = e;
					prevLoader = loader;
				}
			}
		}
		
		throw fault;
	}
}
