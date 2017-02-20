package planet;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;

import planet.transport.Connection;



/**
 *
 * @author Kang-Woo Lee
 */
public class PlanetUtils {
	private PlanetUtils() {
		throw new AssertionError("Should not be invoked!!: class=" + PlanetUtils.class.getName());
	}

	public static final String LOCAL_HOST;
    static {
		try {
			LOCAL_HOST = InetAddress.getLocalHost().getHostAddress();
		}
		catch ( UnknownHostException e ) {
			throw new RuntimeException(e);
		}
    }

	public static String resolveLocalhost(String host) {
		if ( host.equals("localhost") ) {
			return LOCAL_HOST;
		}
		else {
			return host;
		}
	}

	public static String resolovePlanetKey(String key) {
		int idx = key.indexOf(':');	
		if ( idx < 0 ) {
			throw new IllegalArgumentException("invalid PLANET key=" + key);
		}

		return resolveLocalhost(key.substring(0, idx)) + ":"
				+ key.substring(idx+1);
	}

	// planet://111.22.33.44:1234/aaa/bbb/ccc?type=asdfasdfasdfasdfasdfasdfasd
	public static RemoteReference parsePlanetUrl(PlanetServer planet, String url) {
		if ( !url.startsWith("planet://") ) {
			throw new IllegalArgumentException("invalid PLANET URL form=" + url);
		}

		int beginIndex = 9;
		int endIndex = url.indexOf('/', beginIndex);
		if ( endIndex <= 0 ) {
			throw new IllegalArgumentException("invalid PLANET URL form=" + url);
		}
		String id = url.substring(beginIndex, endIndex);

		beginIndex = endIndex;
		endIndex = url.indexOf('?', endIndex);
		if ( endIndex < 0 ) {
			throw new IllegalArgumentException("invalid PLANET URL form (type is not specified)=" + url);
		}
		String path = url.substring(beginIndex, endIndex);
			
		beginIndex = endIndex + 6;
		String typeId = url.substring(beginIndex);

		return RemoteReference.createReference(planet, id, path, typeId);
	}
    
    public static boolean isPlanetProxy(Object obj) {
    	return ( obj instanceof PlanetProxy );
    }
	
	public static Connection addDisconnectionHandlerOnCaller(DisconnectionHandler handler) {
		Connection conn = getConnectionOfCaller();
		if ( conn != null ) {
			conn.addDisconnectionHandler(handler);
		}

		return conn;
	}

	public static Connection getConnectionOfObject(Object obj) {
		if ( obj instanceof PlanetProxy ) {
			PlanetSession session = ((PlanetProxy)obj).getPlanetSession(true);
			if ( session != null ) {
				return session.getConnection();
			}
		}
		
		return null;
	}
	
	public static Connection getConnectionOfCaller() {
		PlanetContext context = PlanetServer.s_context.get();
		if ( context != null ) {
			PlanetSession session = context.getPlanetSession();
			if ( session != null ) {
				return session.getConnection();
			}
		}
		
		return null;
	}

	public static boolean addDisconnectionHandler(Object obj, DisconnectionHandler handler) {
		Connection conn = getConnectionOfObject(obj);
		if ( conn != null ) {
			conn.addDisconnectionHandler(handler);

			return true;
		}
		else {
			return false;
		}
	}

	public static boolean removeDisconnectionHandler(Object obj, DisconnectionHandler handler) {
		Connection conn = getConnectionOfObject(obj);
		if ( conn != null ) {
			conn.removeDisconnectionHandler(handler);

			return true;
		}
		else {
			return false;
		}
	}

	public static String getParentPath(String path) {
		int idx = path.lastIndexOf('/');
		if ( idx < 0 ) {
			return null;
		}

		return (idx > 0) ? path.substring(0, idx) : "/";
	}
	
	public static String getLastIdFromPath(String path) {
		int idx = path.lastIndexOf('/');
		if ( idx < 0 ) {
			return path;
		}
		
		return path.substring(idx+1);
	}

	public static String newChildServantPath(String parent, String id) {
		return parent + "/" + id;
	}

	public static String newServantPath(String... parts) {
		StringBuilder builder = new StringBuilder("/");
		for ( int i =0; i < parts.length-1; ++i ) {
			builder.append(parts[i]).append('/');
		}
		builder.append(parts[parts.length-1]);

		return builder.toString();
	}

	public static String newServantPath(Collection<String> parts) {
		StringBuilder builder = new StringBuilder("/");
		for ( String part: parts ) {
			builder.append(part).append('/');
		}
		builder.setLength(builder.length()-1);

		return builder.toString();
	}

	public static String newServantPath(List<String> parts, int length) {
		StringBuilder builder = new StringBuilder("/");
		if ( length > 1 ) {
			for ( int i =0; i < length-1; ++i ) {
				builder.append(parts.get(i)).append('/');
			}
		}
		builder.append(parts.get(length-1));

		return builder.toString();
	}

	public static String concatTypeNames(String... typeNames) {
		if ( typeNames.length > 0 ) {
			StringBuffer buf = new StringBuffer();
			for ( int i =0; i < typeNames.length-1; ++i ) {
				buf.append(typeNames[i]).append(';');
			}
			buf.append(typeNames[typeNames.length-1]);
			return buf.toString();
		}
		else {
			return "";
		}
	}

	public static String concatTypeNames(Class<?>... types) {
		if ( types.length > 0 ) {
			StringBuffer buf = new StringBuffer();
			for ( int i =0; i < types.length-1; ++i ) {
				buf.append(types[i].getName()).append(';');
			}
			buf.append(types[types.length-1].getName());
			return buf.toString();
		}
		else {
			return "";
		}
	}

	private static final String[] EMPTY_TYPE_NAMES = new String[0];
	public static String[] splitTypeNames(String typeNames) {
		if ( typeNames == null || typeNames.length() == 0 ) {
			return EMPTY_TYPE_NAMES;
		}

		List<String> classNameList = new ArrayList<String>();

		int begin = 0;
		int end = typeNames.indexOf(';', begin);
		while ( end >= 0 ) {
			classNameList.add(typeNames.substring(begin, end));
			end = typeNames.indexOf(';', begin = end + 1);
		}
		classNameList.add(typeNames.substring(begin));

		return classNameList.toArray(new String[classNameList.size()]);
	}

	public static Throwable unwrapThrowable(Throwable e) {
		while ( true ) {
			if ( e instanceof InvocationTargetException ) {
				e = ((InvocationTargetException)e).getTargetException();
			}
			else if ( e instanceof UndeclaredThrowableException ) {
				e = ((UndeclaredThrowableException)e).getUndeclaredThrowable();
			}
			else if ( e instanceof ExecutionException ) {
				e = ((ExecutionException)e).getCause();
			}
			else {
				return e;
			}
		}
	}

	public static String toString(PersistentServant servant) {
		return "Servant[path=" + servant.getServantPath() + ", class="
				+ servant.getClass().getSimpleName() + "]";
	}

	public static String toString(String path, Servant servant) {
		return "Servant[path=" + path + ", class="
				+ servant.getClass().getSimpleName() + "]";
	}
    
//    @SuppressWarnings("unchecked")
//	public static <T> T recast(Object obj, Class<T> targetClass) {
//    	if ( obj instanceof PlanetProxy ) {
//    		return ((PlanetProxy)obj).recast(targetClass);
//    	}
//    	else {
//    		return (T)obj;
//    	}
//    }
//    
//    public static <T> T createProxy(PlanetServer planet, String host, int port, String path,
//    									Class<T> cls) {
//    	String planetId = PlanetUtils.resolveLocalhost(host) + ":" + port;
//    	return planet.createProxy(planetId, path, cls);
//    }

//	public static String getPlanetUrl(PlanetServer planet, String path, Class<?> type) {
//		return getPlanetUrl(planet.getPlanetServerHost(), planet.getPlanetServerPort(),
//							path, type);
//	}
//
//	public static String getPlanetUrl(String host, int port, String path, Class<?> type) {
//		return String.format("planet://%s:%d%s?type=%s", host, port, path, type.getName());
//	}

//	public static List<String> parseServantPath(String path) {
//		return ServantUtils.parseServantPath(path);
//	}

//	public static void setRemoteDescription(PlanetServer planet, String phn,
//											String description) {
//		RemotePlanet remote = planet.createProxy(phn, "/planet", RemotePlanet.class);
//		remote.setConnectionDescription(description);
//	}

//	private static final int MAX_LENGTH = 20;
//	public static String toString(Event event) {
//    	StringBuilder builder = new StringBuilder();
//
//		String name = event.getEventTypeIds()[0];
//		int idx = name.lastIndexOf('.');
//		if ( idx >= 0 ) {
//			builder.append(name.substring(idx+1));
//		}
//		else {
//			builder.append(name);
//		}
//    	builder.append(":[");
//
//    	String[] propNames = event.getPropertyNameAll();
//    	int remains = propNames.length;
//        for ( int i =0; i < propNames.length; ++i ) {
//        	builder.append(propNames[i]).append('=');
//        	Object value = event.getProperty(propNames[i]);
//
//        	if ( value instanceof byte[] ) {
//        		builder.append("byte[").append(((byte[])value).length).append("]");
//        	}
//        	else {
//        		builder.append(toShortString("" + value));
//        	}
//
//        	if ( --remains > 0 ) {
//        		builder.append(",");
//        	}
//        }
//        builder.append(']');
//
//        return builder.toString();
//	}
//
//	private static String toShortString(String msg) {
//		if ( msg.length() > MAX_LENGTH ) {
//			return msg.substring(0, MAX_LENGTH-3) + "...";
//		}
//		else {
//			return msg;
//		}
//	}
}