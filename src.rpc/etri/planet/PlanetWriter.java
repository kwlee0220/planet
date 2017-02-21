package etri.planet;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import planet.PersistentServant;
import planet.PlanetRuntimeException;
import planet.PlanetThrowable;
import planet.PlanetUtils;
import planet.Remote;
import planet.Servant;
import planet.SystemException;
import planet.idl.PlanetValue;
import planet.transport.OutputChannel;

import etri.planet.serializer.Serializer;
import etri.planet.serializer.SerializerGlobals;
import etri.planet.serializer.TypeCode;
import event.Event;


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public class PlanetWriter {
	private final PlanetSessionImpl m_session;
	private final OutputChannel m_ostream;
	private final Map<Object,Integer> m_references;
	
	public PlanetWriter(PlanetSessionImpl session, OutputChannel ostream) {
		m_session = session;
		m_ostream = ostream;
		
		m_references = new IdentityHashMap<Object,Integer>();
	}
	
	public void close() throws IOException {
		m_ostream.close(true);
	}
	
	public PlanetSessionImpl getPlanetSession() {
		return m_session;
	}
	
	public Serializer writeObjectWithCode(Object obj) throws IOException {
		if ( obj == null ) {
			writeByte(TypeCode.NULL);
			
			return null;
		}
		else {
			Serializer serializer = SerializerGlobals.getSerializer(obj);
			if ( serializer == null ) {
				throw new IOException("Serializer not found, class=" + obj.getClass());
			}
			
			serializer.serializeWithCode(obj, this);
			
			return serializer;
		}
	}
	
	public void writeObject(Object obj) throws IOException {
		if ( obj == null ) {
			throw new NullPointerException("object was null");
		}
		else {
			Serializer serializer = SerializerGlobals.getSerializer(obj);
			if ( serializer == null ) {
				throw new IOException("Serializer not found, class=" + obj.getClass());
			}
			
			serializer.serialize(obj, this);
		}
	}
	
	public void writeBoolean(boolean v) throws IOException {
		m_ostream.write1( v ? (byte)1 : (byte)0);
	}
	
	public void writeByte(byte v) throws IOException {
		m_ostream.write1(v);
	}
	
	public void writeShort(short v) throws IOException {
		m_ostream.write2(v);
	}

	public void writeInt(int v) throws IOException {
		m_ostream.write4(v);
	}

	public void writeLong(long v) throws IOException {
		m_ostream.write8(v);
	}

	public void writeFloat(float v) throws IOException {
		writeInt(Float.floatToIntBits(v));
	}

	public void writeDouble(double v) throws IOException {
		writeLong(Double.doubleToLongBits(v));
	}

	public void writeString(String v) throws IOException {
		if ( v == null ){
			writeInt(-1);
		}
		else {
			try {
				byte[] bytes = v.getBytes("utf-8");

				writeInt(bytes.length);
				writeBytes(bytes, 0, bytes.length);
			}
			catch ( UnsupportedEncodingException e ) {
				throw new SystemException("" + e);
			}
		}
	}
	
	public void writeBinary(byte[] bytes, int offset, int length) throws IOException {
		if ( bytes == null ) {
			writeInt(-1);
		}
		else {
			writeBinary(ByteBuffer.wrap(bytes, offset, length));
		}
	}
	
	public void writeBinary(byte[] bytes) throws IOException {
		writeBinary(bytes, 0, bytes.length);
	}
	
	public void writeBinary(ByteBuffer buffer) throws IOException {
		if ( buffer == null ) {
			writeInt(-1);
		}
		else {
			writeInt(buffer.remaining());
			if ( buffer.remaining() > 0 ) {
				writeByteBuffer(buffer);
			}
		}
	}
	
	public void writeRemote(Remote remote) throws IOException {
		if ( remote != null ) {
			writeString(remote.getTypeNames());
			writeString(remote.getPlanetId());
			writeString(remote.getServantPath());
		}
		else {
			writeString(null);
		}
	}
	
	public void writeServant(Servant servant) throws IOException {
		if ( servant != null ) {
			writeString(PlanetInternalUtils.toFilteredTypeNames(servant.getRemoteInterfaces()));
			writeString(m_session.m_planet.getId());
			
			if ( servant instanceof PersistentServant ) {
				String path = ((PersistentServant)servant).getServantPath();
				if ( path == null ) {
					throw new PlanetRuntimeException("Servant path was null: servant=" + servant);
				}
				
				writeString(((PersistentServant)servant).getServantPath());
			}
			else {
				String id = m_session.addServant(servant);
				writeString("/conn/" + m_session.getKey() + "/" + id);
			}
		}
		else {
			writeString(null);
		}
	}
	
	public void writeEnum(Enum<?> entry) throws IOException {
		if ( entry == null ) {
			writeString(null);
		}
		else {
			writeString(entry.getClass().getName());
			writeInt(entry.ordinal());
		}
	}
	
	public void writeEvent(Event event) throws IOException {
		if ( event == null ) {
			writeString(null);
		}
		else {
			writeString(PlanetUtils.concatTypeNames(event.getEventTypeIds()));
			
			String[] propNames = event.getPropertyNames();
			writeInt(propNames.length);
			for ( String name: propNames ) {
				writeString(name);
				writeObjectWithCode(event.getProperty(name));
			}
		}
	}
	
	public void writeException(Throwable e) throws IOException {
		if ( e.getClass().equals(Exception.class) ) {
			writeString(PlanetThrowable.class.getName());
		}
		else {
			writeString(e.getClass().getName());
		}
		
		writeString(e.getMessage());
	}
	
	public void writeClass(Class<?> cls) throws IOException {
		if ( cls == null ) {
			writeByte(TypeCode.NULL);
		}
		else {
			Byte codeObj = SerializerGlobals.CLASS_TO_CODE.get(cls);
			if ( codeObj != null ) {
				writeByte(codeObj);
			}
			else if ( cls.isAnnotationPresent(PlanetValue.class) ) {
				writeByte(TypeCode.VALUE);
				writeString(cls.getName());
			}
			else if ( Servant.class.isAssignableFrom(cls) || Remote.class.isAssignableFrom(cls) ) {
				writeByte(TypeCode.REMOTE);
				writeString(cls.getName());
			}
			else if ( Event.class.isAssignableFrom(cls) ) { 
				writeByte(TypeCode.EVENT);
				writeString(cls.getName());
			}
			else if ( Throwable.class.isAssignableFrom(cls) ) {
				writeByte(TypeCode.EXCEPTION);
				writeString(cls.getName());
			}
			else if ( cls.isArray() ) {
				writeByte(TypeCode.SEQUENCE);
				writeClass(cls.getComponentType());
			}
			else if ( Enum.class.isAssignableFrom(cls) ) {
				writeByte(TypeCode.ENUM);
				writeString(cls.getName());
			}
			else if ( InputStream.class.isAssignableFrom(cls) ) {
				writeByte(TypeCode.STREAM);
			}
			else if ( Map.class.isAssignableFrom(cls) ) {
				writeByte(TypeCode.MAP);
			}
			else if ( Collection.class.isAssignableFrom(cls) ) {
				writeByte(TypeCode.SEQUENCE);
				writeClass(null);
			}
			else if ( ByteBuffer.class.isAssignableFrom(cls) ) {
				writeByte(TypeCode.BYTE_BUFFER);
			}
			else {
				writeByte(TypeCode.REMOTE);
				writeString(cls.getName());
			}
		}
	}
	
	public void writeInputStream(InputStream is) throws IOException {
		if ( is != null ) {
			StreamServer server = new StreamServer(m_session, is);
			server.start();
			
			writeInt(server.getStreamId());
		}
		else {
			writeInt(-1);
		}
	}
	
	public void writeSequence(Object obj) throws IOException {
		if ( obj == null ) {
			writeByte((byte)-1);
			
			return;
		}
		
		Class<?> cls = obj.getClass();
		if ( cls.isArray() ) {
			writeByte((byte)0);
			writeNonNullArray(obj);
		}
		else if ( obj instanceof List || obj instanceof Collection ) {
			writeByte((byte)1);
			writeNonNullCollection((Collection<?>)obj);
		}
		else if ( obj instanceof Set ) {
			writeByte((byte)2);
			writeNonNullCollection((Collection<?>)obj);
		}
		else if ( obj instanceof Vector ) {
			writeByte((byte)3);
			writeNonNullCollection((Collection<?>)obj);
		}
		else {
			throw new PlanetRuntimeException("unknown sequence object=" + obj);
		}
	}
	
	private void writeNonNullArray(Object obj) throws IOException {
		Class<?> cls = obj.getClass();
		if ( !cls.isArray() ) {
			throw new SystemException("Array expected, but was " + cls);
		}
		
		int length = Array.getLength(obj);
		writeInt(length);
		
		writeClass(obj.getClass().getComponentType());
		
		for ( int i =0; i < length; ++i ) {
			Object elm = Array.get(obj, i);
			writeObjectWithCode(elm);
		}
	}
	
	private void writeNonNullCollection(Collection<?> coll) throws IOException {
		int length = coll.size();
		writeInt(length);
		
		writeClass(null);
		
		for ( Object elm: coll ) {
			writeObjectWithCode(elm);
		}
	}
	
	public void writeMap(Map<?,?> map) throws IOException {
		if ( map == null ) {
			writeInt(-1);
		}
		else {
			int remains = map.size();
			writeInt(remains);
			
			for ( Iterator<?> iter = map.entrySet().iterator(); remains > 0; --remains ) {
				Map.Entry<?,?> e = (Map.Entry<?,?>)iter.next();
				
				writeObjectWithCode(e.getKey());
				writeObjectWithCode(e.getValue());
			}
		}
	}
	
	private void writeBytes(byte[] bytes, int offset, int length) throws IOException {
		m_ostream.writen(ByteBuffer.wrap(bytes, offset, length));
	}
	
	private void writeByteBuffer(ByteBuffer buffer) throws IOException {
		m_ostream.writen(buffer);
	}
	
	public int getIndexOfReference(Object object) {
		Integer idx = m_references.get(object);
		return (idx != null) ? idx : -1;
	}
	
	public void addReference(Object obj) {
		int idx = m_references.size();
		m_references.put(obj, idx);
	}
	
	public String toString() {
		return "PlanetOutput[key=" + m_session.getKey() + ", " + m_ostream + "]";
	}
}
