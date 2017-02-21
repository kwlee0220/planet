package etri.planet;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.RandomAccess;
import java.util.Set;

import planet.CallInterceptor;
import planet.Directory;
import planet.PersistentServant;
import planet.PlanetRuntimeException;
import planet.PlanetServer;
import planet.PlanetUtils;
import planet.Servant;
import planet.SystemException;
import planet.idl.PlanetLocal;

import etri.planet.servant.MappedDirectory;
import etri.planet.servant.ServantUtils;



/**
 *
 * @author Kang-Woo Lee
 */
public class PlanetInternalUtils {
	private PlanetInternalUtils() {
		throw new AssertionError("Should not be invoked!!: class=" + PlanetInternalUtils.class.getName());
	}

	public static Set<Class<?>> getInterfaceAllRecusively(Class<?> cls) {
		Set<Class<?>> intfcSet = new HashSet<Class<?>>();

		intfcSet.addAll(Arrays.asList(cls.getInterfaces()));
		while ( (cls = cls.getSuperclass()) != Object.class ) {
			intfcSet.addAll(Arrays.asList(cls.getInterfaces()));
		}

		return intfcSet;
	}

	/**
	 * 주어진 클래스들이 구현하는 모든 외부 공개 가능 인터페이스를 반환한다.
	 * <p>
	 * 인터페이스의 외부 공개 가능성은 다음과 같은 기준으로 판단한다.
	 * <ul>
	 * 	<li> <code>java.io.Serializable</code> 인터페이스가 아닌 것
	 * 	<li> 인터페이스 이름이 <code>sf.cglib.proxy.Factory</code>로 시작하지 않는 것, 그리고
	 * 	<li> {@link PlanetLocal} annotation이 부여되지 않는 것
	 * </ul>
	 * 위 세가지 조건을 모두 만족하는 인터페이스만을 외부 공개 가능 인터페이스로 간주한다.
	 * 
	 * @param types	대상 클래스 배열 객체.
	 * @return	인터페이스 배열.
	 */
	public static Class<?>[] collectRemoteTypeAll(Class<?>... types) {
		Collection<Class<?>> collected = collectRemoteTypeAll(Arrays.asList(types));
		return collected.toArray(new Class<?>[collected.size()]);
	}

	/**
	 * 주어진 객체가 구현하는 모든 외부 공개 가능 인터페이스를 반환한다.
	 * <p>
	 * 인터페이스의 외부 공개 가능성은 다음과 같은 기준으로 판단한다.
	 * <ul>
	 * 	<li> <code>java.io.Serializable</code> 인터페이스가 아닌 것
	 * 	<li> 인터페이스 이름이 <code>sf.cglib.proxy.Factory</code>로 시작하지 않는 것, 그리고
	 * 	<li> {@link PlanetLocal} annotation이 부여되지 않는 것
	 * </ul>
	 * 위 세가지 조건을 모두 만족하는 인터페이스만을 외부 공개 가능 인터페이스로 간주한다.
	 * 
	 * @param obj	대상 객체.
	 * @return	인터페이스 배열.
	 */
	public static Class<?>[] collectRemoteTypeAll(Object obj) {
		Set<Class<?>> intfcSet = getInterfaceAllRecusively(obj.getClass());
		Collection<Class<?>> coll = collectRemoteTypeAll(intfcSet);
		return coll.toArray(new Class<?>[coll.size()]);
	}

	/**
	 * 주어진 클래스들이 구현하는 모든 외부 공개 가능 인터페이스를 반환한다.
	 * <p>
	 * 인터페이스의 외부 공개 가능성은 다음과 같은 기준으로 판단한다.
	 * <ul>
	 * 	<li> <code>java.io.Serializable</code> 인터페이스가 아닌 것
	 * 	<li> 인터페이스 이름이 <code>sf.cglib.proxy.Factory</code>로 시작하지 않는 것, 그리고
	 * 	<li> {@link PlanetLocal} annotation이 부여되지 않는 것
	 * </ul>
	 * 위 세가지 조건을 모두 만족하는 인터페이스만을 외부 공개 가능 인터페이스로 간주한다.
	 * 
	 * @param types	대상 클래스 모음 객체.
	 * @return	인터페이스 배열.
	 */
	public static Collection<Class<?>> collectRemoteTypeAll(Collection<Class<?>> types) {
		List<Class<?>> filteredList = new ArrayList<Class<?>>();

		fillCollectedRemoteTypeAll(filteredList, types);

		return filteredList;
	}

	private static void fillCollectedRemoteTypeAll(List<Class<?>> filteredList,
													Collection<Class<?>> intfcs) {
		for ( Class<?> intfc: intfcs ) {
			if ( intfc.isAnnotationPresent(PlanetLocal.class) ) {
				Class<?>[] superIntfcs = intfc.getInterfaces();
				if ( superIntfcs.length > 0 ) {
					fillCollectedRemoteTypeAll(filteredList, Arrays.asList(superIntfcs));
				}
			}
			else if ( intfc.equals(Serializable.class)
				|| intfc.equals(Cloneable.class)
				|| intfc.equals(RandomAccess.class)
				|| intfc.equals(Collection.class)
				|| "sf.cglib.proxy.Factory".equals(intfc.getName()) ) {
			}
			else {
				filteredList.add(intfc);
			}
		}
	}

	public static String toFilteredTypeNames(Class<?>[] types) throws IOException {
		Class<?>[] filtered = collectRemoteTypeAll(types);
		if ( filtered.length == 0 ) {
			throw new SystemException("no exportable interfaces: interfaces="
											+ Arrays.toString(types));
		}

		return PlanetUtils.concatTypeNames(filtered);
	}
	
	public static void addServant(PersistentServant servant, PlanetServer planet) {
		final String path = servant.getServantPath();

		String parentPath = PlanetUtils.getParentPath(path);
		if ( !existsDirectory(parentPath, planet) ) {
			allocateDirectory(parentPath, planet);
		}
		
		planet.addServant(servant);
	}
	
	public static boolean existsDirectory(String path, PlanetServer planet) {
		Directory dir = planet.getRootDirectory();
		if ( path == null ) {
			throw new PlanetRuntimeException("cannot allocate directory for root");
		}

		List<String> pathElms = ServantUtils.parseServantPath(path);
		for ( int i =0; i < pathElms.size(); ++i ) {
			PersistentServant child = dir.getServant(pathElms.get(i));
			if ( !(child instanceof Directory) ) {
				return false;
			}

			dir = (Directory)child;
		}
		
		return (dir instanceof Directory);
	}

	public static UpdatableDirectory allocateDirectory(String path, PlanetServer planet) {
		Directory dir = planet.getRootDirectory();
		if ( path == null ) {
			throw new PlanetRuntimeException("cannot allocate directory for root");
		}

		List<String> pathElms = ServantUtils.parseServantPath(path);
		for ( int i =0; i < pathElms.size(); ++i ) {
			PersistentServant child = dir.getServant(pathElms.get(i));
			if ( child == null ) {
				if ( dir instanceof UpdatableDirectory ) {
					String childPath = ServantUtils.toPathString(pathElms, i+1);

					child = new MappedDirectory(childPath);
					((UpdatableDirectory)dir).addServant(pathElms.get(i), child);
				}
				else {
					throw new PlanetRuntimeException("non-updatable directory exists: "
													+ PlanetUtils.toString(dir));
				}
			}
			else if ( !(child instanceof Directory) ) {
				throw new PlanetRuntimeException("already exists: " + PlanetUtils.toString(child));
			}

			dir = (Directory)child;
		}

		if ( dir instanceof UpdatableDirectory ) {
			return (UpdatableDirectory)dir;
		}
		else {
			throw new PlanetRuntimeException("non-updatable directory exists: "
											+ PlanetUtils.toString(dir));
		}
	}

	public static Servant wrapToServant(Object obj) {
		if ( obj == null ) {
			throw new IllegalArgumentException("target object was null");
		}

		Class<?>[] remoteTypes = PlanetInternalUtils.collectRemoteTypeAll(obj);
		return WrapToServant.wrap(obj, remoteTypes);
	}

	public static Servant wrapToServant(Object obj, Class<?>... remoteTypes) {
		return WrapToServant.wrap(obj, remoteTypes);
	}

	/**
	 * 주어진 객체를 wrapping하여 {@link PersistentServant} 객체를 생성한다.
	 * <p>
	 * 생성된 객체는 <code>PersistentServant</code> 인터페이스와 인자로 전달된
	 * <code>remoteTypes</code> 인터페이스를 함께 구현하게 된다.
	 *
	 * @param obj			wrapping할 대상 객체.
	 * @param path			생성될 <code>PersistentServant</code>에게 부여할 path.
	 * @param remoteTypes	servant가 제공할 원격 타입 배열.
	 * @return				<code>obj</code>를 wrapping한 <code>PersistentServant</code> 객체.
	 * @throws IllegalArgumentException	전달된 임의의 인자가 <code>null</code>인 경우 또는
	 * 						인자 <code>remoteType</code>이 인터페이스가 아닌 클래스를 포함한 경우.
	 */
	public static PersistentServant wrapToPersistentServant(Object obj, String path,
															Class<?>... remoteTypes) {
		return WrapToPersistentServant.wrap(obj, path, remoteTypes);
	}

	/**
	 * 주어진 두 클래스 배열을 하나의 클래스 배열로 통합한다. 이때 중복되는 클래스는 제거된다.
	 * <p>
	 * 통합 작업시 클래스간 상속 관계는 고려하지 않는다.
	 *
	 * @param left	통합할 첫번째 클래스 배열.
	 * @param right	통합할 두번째 클래스 배열.
	 * @return	통합된 클래스 배열.
	 * @throws	IllegalArgumentException	첫번째 또는 두번째 배열이 <code>null</code>인 경우.
	 */
	public static Class<?>[] concatClasses(Class<?>[] left, Class<?>... right) {
		if ( left == null || right == null ) {
			throw new IllegalArgumentException("argument was null");
		}

		Set<Class<?>> typeSet = new HashSet<Class<?>>();
		typeSet.addAll(Arrays.asList(left));
		typeSet.addAll(Arrays.asList(right));

		return typeSet.toArray(new Class[typeSet.size()]);
	}

	public static Object getCallInterceptorSource(Object proxy) {
		if ( Proxy.isProxyClass(proxy.getClass()) ) {
			InvocationHandler h = Proxy.getInvocationHandler(proxy);
			if ( h instanceof CallInterceptor ) {
				return ((CallInterceptor)h).getSourceObject();
			}
		}

		return null;
	}

//	public static String createPath(String parent, String id) {
//		return parent + "/" + id;
//	}
//
//	public static String createPath(String... parts) {
//		StringBuilder builder = new StringBuilder();
//		for ( int i =0; i < parts.length-1; ++i ) {
//			builder.append(parts[i]).append('/');
//		}
//		builder.append(parts[parts.length-1]);
//
//		return builder.toString();
//	}

//	public static void setRemoteDescription(PlanetServer planet, String phn,
//											String description) {
//		RemotePlanet remote = planet.createProxy(phn, "/planet", RemotePlanet.class);
//		remote.setConnectionDescription(description);
//	}
}
