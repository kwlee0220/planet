package etri.planet;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import planet.PlanetRuntimeException;
import planet.PlanetServer;
import planet.PlanetThrowable;
import planet.PlanetUtils;
import planet.Remote;
import planet.RemoteReference;
import planet.RemoteSystemException;
import planet.Servant;
import planet.SystemException;
import planet.UndeclaredTypeException;
import planet.transport.InputChannel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

import etri.planet.serializer.Serializer;
import etri.planet.serializer.SerializerGlobals;
import etri.planet.serializer.TypeCode;
import event.Event;
import event.support.EventBuilder;

/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
public class PlanetReader {
	private static final Logger s_logger = LoggerFactory.getLogger(PlanetReader.class);

	private final PlanetSessionImpl m_session;
	protected final InputChannel m_ichannel;
	private final List<Object> m_refList = new ArrayList<Object>();

	public PlanetReader(PlanetSessionImpl session, InputChannel ichannel) {
		m_session = session;
		m_ichannel = ichannel;
	}

	public PlanetSessionImpl getPlanetSession() {
		return m_session;
	}

	public InputChannel getInputChannel() {
		return m_ichannel;
	}

	@SuppressWarnings("unchecked")
	public Object readObject(Class expected) throws IOException {
		byte code = readByte();
		if ( code == TypeCode.NULL || code == TypeCode.VOID ) {
			return null;
		}
		else if ( code == TypeCode.REFERENCE ) {
			int idx = readShort();
			return getReference(idx);
		}

		Object object;

		Serializer serializer = SerializerGlobals.CODE_TO_SERIALIZER[code];
		if ( serializer == null ) {
			throw new SystemException("Cannot find serializer: code=" + code);
		}

		object = serializer.deserializeUsingCode(code, this);

		return object;
	}

	public byte readByte() throws IOException {
		return m_ichannel.read1();
	}

	public short readShort() throws IOException {
		return m_ichannel.read2();
	}

	public int readInt() throws IOException {
		return m_ichannel.read4();
	}

	public long readLong() throws IOException {
		return m_ichannel.read8();
	}

	public float readFloat() throws IOException {
	    return Float.intBitsToFloat(readInt());
	}

	public double readDouble() throws IOException {
	    return Double.longBitsToDouble(readLong());
	}

	public boolean readBoolean() throws IOException {
		return m_ichannel.read1() != 0;
	}

	private static final String EMPTY_STRING = "";
	public String readString() throws IOException {
		int length = readInt();
		if ( length > 0 ) {
//			if ( m_buffer.hasArray() ) {
//				String s = new String(m_buffer.array(), m_buffer.arrayOffset() + m_buffer.position(), length, "utf-8");
//				m_buffer.position(m_buffer.position() + length);
//
//				return s;
//			}
//			else {
				byte[] bytes = new byte[length];
				m_ichannel.readBytes(bytes, 0, length);

				return new String(bytes, 0, length, "utf-8");
//			}
		}
		else if ( length == 0 ) {
			return EMPTY_STRING;
		}
		else {
			return null;
		}
	}

	protected static final byte[] EMPTY_BINARY = new byte[0];
	public byte[] readBinary() throws IOException {
		int length = readInt();

		if ( length > 0 ) {
			byte[] bytes = new byte[length];

			m_ichannel.readBytes(bytes, 0, length);
			return bytes;
		}
		else if ( length == 0 ) {
			return EMPTY_BINARY;
		}
		else {
			return null;
		}
	}

	protected static final ByteBuffer EMPTY_BYTE_BUFFER = ByteBuffer.wrap(EMPTY_BINARY);
	public ByteBuffer readByteBuffer() throws IOException {
		int length = readInt();

		if ( length > 0 ) {
			ByteBuffer buf = ByteBuffer.allocate(length);
			m_ichannel.readByteBuffer(buf);

			return buf;
		}
		else if ( length == 0 ) {
			return EMPTY_BYTE_BUFFER;
		}
		else {
			return null;
		}
	}

	public RemoteReference readRemoteReference(Class<?>... remoteTypes) throws IOException {
		String refPlanetId = readString();
		if ( refPlanetId == null ) {
			return null;
		}

		String path = readString();

		return RemoteReference.createReference(m_session.m_planet, refPlanetId, path, remoteTypes);
	}

	public Remote readRemote(Class<?>... remoteTypes) throws IOException {
		String typeNames = readString();
		if ( typeNames == null ) {
			return null;
		}

		String planetId = readString();
		String path = readString();

		PlanetServerImpl planet = m_session.m_planet;
		RemoteReference ref = RemoteReference.createReference(planet, planetId, path, typeNames);

		Class<?>[] types = ref.getTypes();
		if ( types.length >= 1 ) {
			return (Remote)planet.createProxy(ref.getPlanetId(), ref.getServantPath(), types);
		}
		else {
			return ref;
		}
	}

	public Object readRemote_(Class<?>... remoteTypes) throws IOException {
		String typeNames = readString();
		if ( typeNames == null ) {
			return null;
		}

		String planetId = readString();
		String path = readString();

		PlanetServerImpl planet = m_session.m_planet;
		
		if ( planet.getId().equals(planetId) ) {
			Servant servant = planet.getServant(path);
			
			return servant;
		}
		
		RemoteReference ref = RemoteReference.createReference(planet, planetId, path, typeNames);

		Class<?>[] types = ref.getTypes();
		if ( types.length >= 1 ) {
			return (Remote)planet.createProxy(ref.getPlanetId(), ref.getServantPath(), types);
		}
		else {
			return ref;
		}
	}

	@SuppressWarnings("unchecked")
	public <T> T readEnum(Class<T> enumClass) throws IOException {
		if ( !enumClass.isEnum() ) {
			throw new SystemException("class was not Enum: " + enumClass);
		}

		int ordinal = readInt();
		if ( ordinal < 0 ) {
			return null;
		}

		try {
			Method mid = enumClass.getMethod("values", (Class[])null);
			Object values = mid.invoke(null, (Object[])null);

			if ( ordinal >= Array.getLength(values) ) {
				throw new SystemException("invalid Enum[class=" + enumClass.getSimpleName()
												+ ", ordinal=" + ordinal + "]");
			}

			return (T)Array.get(values, ordinal);
		}
		catch ( SystemException e ) {
			throw e;
		}
		catch ( Throwable e ) {
			throw new SystemException("" + PlanetUtils.unwrapThrowable(e));
		}
	}

	public Class<?> readClass() throws IOException {
		byte tc = readByte();
		if ( tc == TypeCode.NULL ) {
			return null;
		}

		Class<?> cls = SerializerGlobals.CODE_TO_CLASS[tc];
		if ( cls != null ) {
			return cls;
		}

		if ( tc == TypeCode.REMOTE || tc == TypeCode.EVENT
			|| tc == TypeCode.VALUE || tc == TypeCode.ENUM ) {
			return readClassFromString();
		}
		else if ( tc == TypeCode.EXCEPTION ) {
			Class<?> exceptClass = readClassFromString();
			return ( exceptClass.equals(PlanetThrowable.class) )
					? Exception.class : exceptClass;
		}
		else if ( tc == TypeCode.SEQUENCE ) {
			Class<?> elmType = readClass();

			return Array.newInstance(elmType, 0).getClass();
		}
		else if ( tc == TypeCode.NULL ) {
			return null;
		}

		s_logger.error("invalid typecode=" + tc);
		throw new SystemException("Should have not been here");
	}

	public Class<?> readClassFromString() throws IOException, UndeclaredTypeException {
		String typeId = readString();

		return m_session.m_planet.loadClass(typeId);
	}

	public Class<?>[] readClassesFromString() throws IOException {
		return loadClassesFromNames(readString());
	}

	private static final Class<?>[] THROWABLE_CTOR_TYPES = new Class[]{String.class};
	public Throwable readException() throws IOException {
		String exTypeId = readString();
		if ( exTypeId == null ) {
			return null;
		}
		else {
			if ( exTypeId.equals(PlanetThrowable.class.getName()) ) {
				exTypeId = "java.lang.Exception";
			}

			String details = readString();

			if ( exTypeId.equals(RemoteSystemException.ID) ) {
				return new RemoteSystemException(details);
			}
			else {
				Class<?> cls = m_session.m_planet.loadClass(exTypeId);

				try {
					Constructor<?> ctor = cls.getConstructor(THROWABLE_CTOR_TYPES);
					return (Throwable)ctor.newInstance(new Object[]{details});
				}
				catch ( InvocationTargetException e ) {
					throw new SystemException("" + e.getTargetException());
				}
				catch ( Exception e ) {
					throw new SystemException("fails to read Exception=" + exTypeId + ", cause" + e);
				}
			}
		}
	}

	public Event readEvent() throws IOException {
		String concatedTypeNames = readString();
		if ( concatedTypeNames == null ) {
			return null;
		}

		String[] typeIds = PlanetUtils.splitTypeNames(concatedTypeNames);
		EventBuilder builder = newEventBuilder(m_session.m_planet, typeIds);

		int count = readInt();
		for ( int i =0; i < count; ++i ) {
			String key = readString();
			Object value = readObject(Object.class);

			builder.setProperty(key, value);
		}

		return builder.build();
	}

	public InputStream readInputStream() throws IOException {
		int streamId = readInt();
		if ( streamId >= 0 ) {
			InputChannel ichannel = m_session.getInputChannel(streamId);
			return new StreamClient(ichannel);
		}
		else {
			return null;
		}
	}

	public Object readSequence(Class<?> expected) throws IOException {
		byte seqType = readByte();
		switch ( seqType ) {
			case -1:
				return null;
			case 0:
				return readArray(expected);
			case 1:
				return readList();
			case 2:
				return readSet();
			case 3:
				return readVector();
			default:
				throw new PlanetRuntimeException("unknown sequence type=" + seqType);
		}
	}

	public Object readArray(Class<?> expected) throws IOException {
		int length = readInt();
		if ( length < 0 ) {
			return null;
		}

		Class<?> elmClass = readClass();
		Object array = Array.newInstance(elmClass, length);
		if ( length == 0 ) {
			return array;
		}

		for ( int i =0; i < length; ++i ) {
			Array.set(array, i, readObject(expected));
		}
		return array;
	}

	private void fillCollection(Collection<Object> coll, int length) throws IOException {
		for ( int i =0; i < length; ++i ) {
			coll.add(readObject(null));
		}
	}

	private static final Vector<Object> EMPTY_VECTOR = new Vector<Object>();
	public Vector<Object> readVector() throws IOException {
		int length = readInt();
		if ( length < 0 ) {
			return null;
		}

		readClass();
		if ( length == 0 ) {
			return EMPTY_VECTOR;
		}

		Vector<Object> vect = new Vector<Object>(length);
		fillCollection(vect, length);

		return vect;
	}

	private static final List<?> EMPTY_LIST = Collections.EMPTY_LIST;
	public List<?> readList() throws IOException {
		int length = readInt();
		if ( length < 0 ) {
			return null;
		}

		readClass();
		if ( length == 0 ) {
			return EMPTY_LIST;
		}

		List<Object> list = new ArrayList<Object>(length);
		fillCollection(list, length);

		return list;
	}

	private static final Set<?> EMPTY_SET = Collections.EMPTY_SET;
	public Set<?> readSet() throws IOException {
		int length = readInt();
		if ( length < 0 ) {
			return null;
		}

		readClass();
		if ( length == 0 ) {
			return EMPTY_SET;
		}

		Set<Object> set = new HashSet<Object>(length);
		fillCollection(set, length);

		return set;
	}

	@SuppressWarnings("unchecked")
	private static final Map<?,?> EMPTY_MAP = Collections.unmodifiableMap(new HashMap());
	public Map<?,?> readMap() throws IOException {
		int length = readInt();
		if ( length > 0 ) {
			Map<Object,Object> map = new HashMap<Object,Object>();
			for ( int i =0; i < length; ++i ) {
				Object key = readObject(null);
				Object value = readObject(null);

				map.put(key, value);
			}

			return map;
		}
		else if ( length == 0 ) {
			return EMPTY_MAP;
		}
		else {
			return null;
		}
	}

	public Object getReference(int idx) {
		return m_refList.get(idx);
	}

	public void addReference(Object obj) {
		m_refList.add(obj);
	}

	void clearReferenceAll() {
		m_refList.clear();
	}

	private Class<?>[] loadClassesFromNames(String typeNames) {
		PlanetServerImpl planet = m_session.m_planet;
		List<Class<?>> classList = new ArrayList<Class<?>>();

		int begin = 0;
		int end = typeNames.indexOf(';', begin);
		while ( end >= 0 ) {
			try {
				classList.add(planet.loadClass(typeNames.substring(begin, end)));
			}
			catch ( UndeclaredTypeException ignored ) { }
			end = typeNames.indexOf(';', begin = end + 1);
		}
		try {
			classList.add(planet.loadClass(typeNames.substring(begin)));
		}
		catch ( UndeclaredTypeException ignored ) { }

		return classList.toArray(new Class[classList.size()]);
	}

	private EventBuilder newEventBuilder(PlanetServer planet, String[] typeIds) {
		List<Class<?>> typeList = Lists.newArrayList();
		for ( int i =0; i < typeIds.length; ++i ) {
			try {
				typeList.add(planet.loadClass(typeIds[i]));
			}
			catch ( UndeclaredTypeException e ) {
				if ( s_logger.isDebugEnabled() ) {
					s_logger.debug("fails to load class=" + typeIds[i] + ", cause=" + e);
				}
			}
		}
		
		return new EventBuilder(typeList);
	}
}
