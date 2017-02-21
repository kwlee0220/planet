package etri.planet.serializer;

import java.io.IOException;

import etri.planet.PlanetReader;
import etri.planet.PlanetWriter;


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public class ShortSerializer implements Serializer {
	@Override
	public byte getTypeCode() {
		return TypeCode.SHORT;
	}
	
	public void serialize(Object obj, PlanetWriter writer) throws IOException {
		writer.writeShort((Short)obj);
	}

	public void serializeWithCode(Object obj, PlanetWriter writer) throws IOException {
		writer.writeByte(TypeCode.SHORT);
		serialize(obj, writer);
	}
	
	public Object deserializeUsingClass(Class<?> expected, PlanetReader reader) throws IOException {
		return reader.readShort();
	}

	public Object deserializeUsingCode(byte code, PlanetReader reader) throws IOException {
		return reader.readShort();
	}
}