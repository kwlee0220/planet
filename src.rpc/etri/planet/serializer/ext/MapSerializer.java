package etri.planet.serializer.ext;

import java.io.IOException;
import java.util.Map;

import etri.planet.PlanetReader;
import etri.planet.PlanetWriter;
import etri.planet.serializer.Serializer;
import etri.planet.serializer.TypeCode;



public class MapSerializer implements Serializer {
	@Override
	public byte getTypeCode() {
		return TypeCode.MAP;
	}
	
	public void serialize(Object obj, PlanetWriter out) throws IOException {
		if ( !(obj instanceof Map) ) {
			throw new IllegalArgumentException("Map expected, but was " + obj.getClass());
		}
		
		out.writeMap((Map<?,?>)obj);
	}

	public void serializeWithCode(Object obj, PlanetWriter out) throws IOException {
		if ( obj == null ) {
			out.writeByte(TypeCode.NULL);
		}
		else {
			out.writeByte(TypeCode.MAP);
			serialize(obj, out);
		}
	}

	public Object deserializeUsingClass(Class<?> expected, PlanetReader in) throws IOException {
		return in.readMap();
	}

	public Object deserializeUsingCode(byte code, PlanetReader in) throws IOException {
		if ( code == TypeCode.NULL ) {
			return null;
		}
		else {
			return deserializeUsingClass(Map.class, in);
		}
	}
}
