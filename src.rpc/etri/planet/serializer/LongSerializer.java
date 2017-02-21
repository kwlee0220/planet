package etri.planet.serializer;

import java.io.IOException;

import etri.planet.PlanetReader;
import etri.planet.PlanetWriter;

/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public class LongSerializer implements Serializer {
	@Override
	public byte getTypeCode() {
		return TypeCode.LONG;
	}
	
	public void serialize(Object obj, PlanetWriter writer) throws IOException {
		writer.writeLong((Long)obj);
	}

	public void serializeWithCode(Object obj, PlanetWriter input) throws IOException {
		input.writeByte(TypeCode.LONG);
		serialize(obj, input);
	}
	
	public Object deserializeUsingClass(Class<?> expected, PlanetReader reader) throws IOException {
		return reader.readLong();
	}

	public Object deserializeUsingCode(byte code, PlanetReader reader) throws IOException {
		return reader.readLong();
	}
}
