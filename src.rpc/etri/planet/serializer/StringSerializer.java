package etri.planet.serializer;

import java.io.IOException;

import etri.planet.PlanetReader;
import etri.planet.PlanetWriter;

/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public class StringSerializer implements Serializer {
	@Override
	public byte getTypeCode() {
		return TypeCode.STRING;
	}
	
	public void serialize(Object obj, PlanetWriter writer) throws IOException {
		writer.writeString((String)obj);
	}

	public void serializeWithCode(Object obj, PlanetWriter writer) throws IOException {
		if ( obj == null ) {
			writer.writeByte(TypeCode.NULL);
		}
		else {
			writer.writeByte(TypeCode.STRING);
			serialize(obj, writer);
		}
	}

	public Object deserializeUsingClass(Class<?> expected, PlanetReader reader) throws IOException {
		return reader.readString();
	}

	public Object deserializeUsingCode(byte code, PlanetReader reader) throws IOException {
		if ( code == TypeCode.NULL ) {
			return null;
		}
		else {
			return reader.readString();
		}
	}
}
