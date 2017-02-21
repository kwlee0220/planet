package etri.planet.serializer;

import java.io.IOException;

import etri.planet.PlanetReader;
import etri.planet.PlanetWriter;


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public class BinarySerializer implements Serializer {
	@Override
	public byte getTypeCode() {
		return TypeCode.BINARY;
	}
	
	public void serialize(Object obj, PlanetWriter writer) throws IOException {
		writer.writeBinary((byte[])obj);
	}

	public void serializeWithCode(Object obj, PlanetWriter writer) throws IOException {
		if ( obj == null ) {
			writer.writeByte(TypeCode.NULL);
		}
		else {
			writer.writeByte(TypeCode.BINARY);
			serialize(obj, writer);
		}
	}
	
	public Object deserializeUsingClass(Class<?> expected, PlanetReader reader) throws IOException {
		return reader.readBinary();
	}

	public Object deserializeUsingCode(byte code, PlanetReader reader) throws IOException {
		if ( code == TypeCode.NULL ) {
			return null;
		}
		else {
			return reader.readBinary();
		}
	}
}
