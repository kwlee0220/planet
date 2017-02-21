package etri.planet.serializer;


import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

import planet.Remote;
import planet.Servant;
import planet.idl.PlanetValue;

import etri.planet.serializer.ext.ByteBufferSerializer;
import etri.planet.serializer.ext.ClassSerializer;
import etri.planet.serializer.ext.MapSerializer;
import event.Event;


/**
 * 
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public class SerializerGlobals {
	public static final ByteSerializer BYTE = new ByteSerializer();
	public static final ShortSerializer SHORT = new ShortSerializer();
	public static final IntSerializer INT = new IntSerializer();
	public static final LongSerializer LONG = new LongSerializer();
	public static final FloatSerializer FLOAT = new FloatSerializer();
	public static final DoubleSerializer DOUBLE = new DoubleSerializer();
	public static final BooleanSerializer BOOLEAN = new BooleanSerializer();
	public static final StringSerializer STRING = new StringSerializer();
	public static final BinarySerializer BINARY = new BinarySerializer();

	public static final RemoteSerializer REMOTE = new RemoteSerializer();
	public static final GValueSerializer GVALUE = new GValueSerializer();
	public static final SequenceSerializer SEQUENCE = new SequenceSerializer();
	public static final ExceptionSerializer EXCEPTION = new ExceptionSerializer();
	public static final EventSerializer EVENT = new EventSerializer();
	
	public static final StreamSerializer STREAM = new StreamSerializer();

	public static final MapSerializer MAP = new MapSerializer();
	
	public static final ByteBufferSerializer BYTE_BUFFER = new ByteBufferSerializer();
	public static final ClassSerializer CLASS = new ClassSerializer();
	public static final EnumSerializer ENUM = new EnumSerializer();

	public static final ServantSerializer SERVANT = new ServantSerializer();
	
//	static IdentityHashMap<Class<?>,ValueSerializer> s_valueSerializerMap
//									= new IdentityHashMap<Class<?>,ValueSerializer>();
	
	public static IdentityHashMap<Class<?>,Byte> CLASS_TO_CODE
													= new IdentityHashMap<Class<?>,Byte>();
	static {
		CLASS_TO_CODE.put(byte.class, TypeCode.BYTE);
		CLASS_TO_CODE.put(Byte.class, TypeCode.BYTE);
		CLASS_TO_CODE.put(short.class, TypeCode.SHORT);
		CLASS_TO_CODE.put(Short.class, TypeCode.SHORT);
		CLASS_TO_CODE.put(int.class, TypeCode.INT);
		CLASS_TO_CODE.put(Integer.class, TypeCode.INT);
		CLASS_TO_CODE.put(long.class, TypeCode.LONG);
		CLASS_TO_CODE.put(Long.class, TypeCode.LONG);
		CLASS_TO_CODE.put(float.class, TypeCode.FLOAT);
		CLASS_TO_CODE.put(Float.class, TypeCode.FLOAT);
		CLASS_TO_CODE.put(double.class, TypeCode.DOUBLE);
		CLASS_TO_CODE.put(Double.class, TypeCode.DOUBLE);
		CLASS_TO_CODE.put(boolean.class, TypeCode.BOOLEAN);
		CLASS_TO_CODE.put(Boolean.class, TypeCode.BOOLEAN);
		CLASS_TO_CODE.put(String.class, TypeCode.STRING);
		CLASS_TO_CODE.put(byte[].class, TypeCode.BINARY);
		
		CLASS_TO_CODE.put(void.class, TypeCode.VOID);
		CLASS_TO_CODE.put(Void.class, TypeCode.VOID);
	}
	
	public static Class<?>[] CODE_TO_CLASS = new Class[] {
		/*NULL*/	null,
		
	    /*BYTE*/	byte.class,
	    /*SHORT*/	short.class,
	    /*INT*/		int.class,
	    /*LONG*/	long.class,
	    /*FLOAT*/	float.class,
	    /*DOUBLE*/	double.class,
	    /*BOOLEAN*/	boolean.class,
	    /*STRING*/	String.class,
	    /*BINARY*/	byte[].class,
	    /*ENUM*/	null,
	    /*EXCEPTION*/ null,
	    /*CLASS*/	null,
	    
	    /*REMOTE*/	null,
	    /*VALUE*/	null,
	    /*EVENT*/	null,
	    /*STREAM*/	InputStream.class,
	    /*SEQ*/		null,
		
		/*VOID*/	void.class,
	    /*REF*/		null,

	    /*MAP*/		HashMap.class,
	    /*BYTE_BUFFER*/	ByteBuffer.class,
	};
	
	public static Serializer[] CODE_TO_SERIALIZER = {
		/*NULL*/	null,
		
		/*BYTE*/	BYTE,
		/*SHORT*/	SHORT,
		/*INT*/		INT,
		/*LONG*/	LONG,
		/*FLOAT*/	FLOAT,
		/*DOUBLE*/	DOUBLE,
		/*BOOLEAN*/	BOOLEAN,
		/*STRING*/	STRING,
		/*BINARY*/	BINARY,
		/*ENUM*/	ENUM,
		/*EXCEPTION*/	EXCEPTION,
		/*CLASS*/	CLASS,

		/*REMOTE*/	REMOTE,
		/*VALUE*/	GVALUE,
	    /*EVENT*/	EVENT,
		/*STREAM*/	STREAM,
		/*SEQ*/		SEQUENCE,
		
		/*VOID*/	null,
		/*REF*/		null,

		/*MAP*/		MAP,
		/*BYTE_BUFFER*/	BYTE_BUFFER,
	};
	
	@SuppressWarnings("unchecked")
	public static Serializer getSerializerFromClass(Class cls) {
		Byte code = CLASS_TO_CODE.get(cls);
		if ( code != null ) {
			return CODE_TO_SERIALIZER[code];
		}
		
		if ( cls.isAnnotationPresent(PlanetValue.class) ) {
			return SerializerGlobals.GVALUE.getValueSerializer(cls);
//			ValueSerializer vSerializer = s_valueSerializerMap.get(cls);
//			if ( vSerializer == null ) {
//				vSerializer = new ValueSerializer(cls);
//				s_valueSerializerMap.put(cls, vSerializer);
//				
//				if ( RpcLoggers.CODEC.isInfoEnabled() ) {
//					RpcLoggers.CODEC.info("created: PlanetValueSerializer[class=" + cls.getName() + "]");
//				}
//			}
//			
//			return vSerializer;
		}
		else if ( Servant.class.isAssignableFrom(cls) ) {
			return SerializerGlobals.SERVANT;
		}
		else if ( Remote.class.isAssignableFrom(cls) ) {
			return SerializerGlobals.REMOTE;
		}
		else if ( Event.class.isAssignableFrom(cls) ) {
			return SerializerGlobals.EVENT;
		}
		else if ( Throwable.class.isAssignableFrom(cls) ) {
			return SerializerGlobals.EXCEPTION;
		}
		else if ( Map.class.isAssignableFrom(cls) ) {
			throw new AssertionError("Cannot support MAP now");
//			return SerializerGlobals.MAP;
		}
		else if ( cls.isArray() || Collection.class.isAssignableFrom(cls) ) {
			return SerializerGlobals.SEQUENCE;
		}
		else if ( Enum.class.isAssignableFrom(cls) ) {
			return SerializerGlobals.ENUM;
		}
		else if ( Class.class.isAssignableFrom(cls) ) {
			return SerializerGlobals.CLASS;
		}
		else if ( InputStream.class.isAssignableFrom(cls) ) {
			return SerializerGlobals.STREAM;
		}
		else if ( ByteBuffer.class.isAssignableFrom(cls) ) {
			return SerializerGlobals.BYTE_BUFFER;
		}
		else {
			return SerializerGlobals.SERVANT;
		}
	}
	
	public static Serializer getSerializer(Object obj) {
		return getSerializerFromClass(obj.getClass());
	}
}