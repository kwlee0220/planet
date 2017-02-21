package etri.planet.serializer;

import java.io.IOException;

import etri.planet.PlanetReader;
import etri.planet.PlanetWriter;


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public class ByteSerializer implements Serializer {
	@Override
	public byte getTypeCode() {
		return TypeCode.BYTE;
	}
	
	public void serialize(Object obj, PlanetWriter writer) throws IOException {
		writer.writeByte((Byte)obj);
	}

	public void serializeWithCode(Object obj, PlanetWriter writer) throws IOException {
		writer.writeByte(TypeCode.BYTE);
		serialize(obj, writer);
	}
	
	public Object deserializeUsingClass(Class<?> expected, PlanetReader reader) throws IOException {
		return reader.readByte();
	}

	public Object deserializeUsingCode(byte code, PlanetReader reader) throws IOException {
		return reader.readByte();
	}
}
