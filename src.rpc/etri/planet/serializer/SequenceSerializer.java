package etri.planet.serializer;

import java.io.IOException;

import etri.planet.PlanetReader;
import etri.planet.PlanetWriter;


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public class SequenceSerializer implements Serializer {
	@Override
	public byte getTypeCode() {
		return TypeCode.SEQUENCE;
	}
	
	public void serialize(Object obj, PlanetWriter writer) throws IOException {
		writer.writeSequence(obj);
	}

	public void serializeWithCode(Object obj, PlanetWriter writer) throws IOException {
		if ( obj == null ) {
			writer.writeByte(TypeCode.NULL);
		}
		else {
			writer.writeByte(TypeCode.SEQUENCE);
			serialize(obj, writer);
		}
	}
	
	public Object deserializeUsingClass(Class<?> expected, PlanetReader reader) throws IOException {
		return reader.readSequence(expected);
	}

	public Object deserializeUsingCode(byte code, PlanetReader reader) throws IOException {
		if ( code == TypeCode.NULL ) {
			return null;
		}
		else {
//			Class<?> elmClass = reader.readClass();
//			Class<?> arrClass = Array.newInstance(elmClass, 0).getClass();

			return deserializeUsingClass(null, reader);
		}
	}
}
