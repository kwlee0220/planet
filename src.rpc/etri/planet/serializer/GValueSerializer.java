package etri.planet.serializer;


import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

import planet.UndeclaredTypeException;

import etri.planet.PlanetReader;
import etri.planet.PlanetWriter;
import etri.planet.RpcLoggers;


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public class GValueSerializer implements Serializer {
	static ConcurrentHashMap<Class<?>,ValueSerializer> SERIALIZER_MAP
											= new ConcurrentHashMap<Class<?>,ValueSerializer>();
	
	public GValueSerializer() {
	}
	
	@Override
	public byte getTypeCode() {
		return TypeCode.VALUE;
	}

	@Override
	public void serialize(Object obj, PlanetWriter out) throws IOException {
		throw new AssertionError("Should not be invoked: class=" + getClass().getName());
//		ValueSerializer vSerializer = getValueSerializer(obj.getClass());
//		vSerializer.serialize(obj, out);
	}

	@Override
	public void serializeWithCode(Object obj, PlanetWriter writer) throws IOException {
		throw new AssertionError("Should not be invoked: class=" + getClass().getName());
	}

	@Override
	public Object deserializeUsingClass(Class<?> expected, PlanetReader in) throws IOException {
		throw new AssertionError("Should not be invoked: class=" + getClass().getName());
	}

	@Override
	public Object deserializeUsingCode(byte code, PlanetReader in) throws IOException {
		try {
			Class<?> vClass = in.readClassFromString();
			ValueSerializer vSerializer = SERIALIZER_MAP.get(vClass);
			if ( vSerializer == null ) {
				vSerializer = new ValueSerializer(vClass);
				ValueSerializer prevSerializer = SERIALIZER_MAP.putIfAbsent(vClass, vSerializer);
				if ( prevSerializer != null ) {
					vSerializer = prevSerializer;
				}
				else {
					if ( RpcLoggers.CODEC.isInfoEnabled() ) {
						RpcLoggers.CODEC.info("created: ValueSerializer[class="
												+ vClass.getName() + "]");
					}
				}
			}
			
			return vSerializer.deserializeUsingClass(vClass, in);
		}
		catch ( UndeclaredTypeException e ) {
			throw e;
		}
	}
	
	public ValueSerializer getValueSerializer(Class<?> vClass) {
		ValueSerializer vSerializer = SERIALIZER_MAP.get(vClass);
		if ( vSerializer == null ) {
			vSerializer = new ValueSerializer(vClass);
			ValueSerializer prevSerializer = SERIALIZER_MAP.putIfAbsent(vClass, vSerializer);
			if ( prevSerializer != null ) {
				vSerializer = prevSerializer;
			}
			else {
				if ( RpcLoggers.CODEC.isInfoEnabled() ) {
					RpcLoggers.CODEC.info("created: ValueSerializer[class="
											+ vClass.getName() + "]");
				}
			}
		}
		
		return vSerializer;
	}
}
