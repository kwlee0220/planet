package etri.planet.serializer;

import java.io.IOException;

import etri.planet.PlanetReader;
import etri.planet.PlanetWriter;


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public class BooleanSerializer implements Serializer {
	@Override
	public byte getTypeCode() {
		return TypeCode.BOOLEAN;
	}
	
	public void serialize(Object obj, PlanetWriter writer) throws IOException {
		writer.writeBoolean((Boolean)obj);
	}

	public void serializeWithCode(Object obj, PlanetWriter writer) throws IOException {
		writer.writeByte(TypeCode.BOOLEAN);
		serialize(obj, writer);
	}
	
	public Object deserializeUsingClass(Class<?> expected, PlanetReader reader) throws IOException {
		return reader.readBoolean();
	}

	public Object deserializeUsingCode(byte code, PlanetReader reader) throws IOException {
		return reader.readBoolean();
	}
}
