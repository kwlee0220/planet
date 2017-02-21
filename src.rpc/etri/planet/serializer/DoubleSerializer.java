package etri.planet.serializer;

import java.io.IOException;

import etri.planet.PlanetReader;
import etri.planet.PlanetWriter;

/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public class DoubleSerializer implements Serializer {
	@Override
	public byte getTypeCode() {
		return TypeCode.DOUBLE;
	}
	
	public void serialize(Object obj, PlanetWriter writer) throws IOException {
		writer.writeDouble((Double)obj);
	}

	public void serializeWithCode(Object obj, PlanetWriter writer) throws IOException {
		writer.writeByte(TypeCode.DOUBLE);
		serialize(obj, writer);
	}
	
	public Object deserializeUsingClass(Class<?> expected, PlanetReader reader) throws IOException {
		return reader.readDouble();
	}

	public Object deserializeUsingCode(byte code, PlanetReader reader) throws IOException {
		return reader.readDouble();
	}
}
