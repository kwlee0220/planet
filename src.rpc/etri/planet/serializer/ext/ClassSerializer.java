package etri.planet.serializer.ext;

import java.io.IOException;

import etri.planet.PlanetReader;
import etri.planet.PlanetWriter;
import etri.planet.serializer.Serializer;
import etri.planet.serializer.TypeCode;


public class ClassSerializer implements Serializer {
	@Override
	public byte getTypeCode() {
		return TypeCode.TYPE;
	}
	
	public void serialize(Object obj, PlanetWriter writer) throws IOException {
		Class<?> cls = obj.getClass();
		
		if ( obj == null || obj instanceof Class ) {
			writer.writeClass((Class<?>)obj);
		}
		else {
			throw new IllegalArgumentException("Invalid object: expected=Class.class, but=" + cls);
		}
	}

	public void serializeWithCode(Object obj, PlanetWriter writer) throws IOException {
		writer.writeByte(TypeCode.TYPE);
		serialize(obj, writer);
	}
	
	public Object deserializeUsingClass(Class<?> expected, PlanetReader reader) throws IOException {
		return reader.readClass();
	}

	public Object deserializeUsingCode(byte code, PlanetReader reader) throws IOException {
		return deserializeUsingClass(null, reader);
	}
}