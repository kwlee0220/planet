package etri.planet.serializer;

import java.io.IOException;

import etri.planet.PlanetReader;
import etri.planet.PlanetWriter;

/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public class FloatSerializer implements Serializer {
	@Override
	public byte getTypeCode() {
		return TypeCode.FLOAT;
	}
	
	public void serialize(Object obj, PlanetWriter writer) throws IOException {
		writer.writeFloat((Float)obj);
	}

	public void serializeWithCode(Object obj, PlanetWriter writer) throws IOException {
		writer.writeByte(TypeCode.FLOAT);
		serialize(obj, writer);
	}
	
	public Object deserializeUsingClass(Class<?> expected, PlanetReader reader) throws IOException {
		return reader.readFloat();
	}

	public Object deserializeUsingCode(byte code, PlanetReader reader) throws IOException {
		return reader.readFloat();
	}
}
