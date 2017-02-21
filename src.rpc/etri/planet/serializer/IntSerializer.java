package etri.planet.serializer;

import java.io.IOException;

import etri.planet.PlanetReader;
import etri.planet.PlanetWriter;




public class IntSerializer implements Serializer {
	@Override
	public byte getTypeCode() {
		return TypeCode.INT;
	}
	
	public void serialize(Object obj, PlanetWriter writer) throws IOException {
		writer.writeInt((Integer)obj);
	}

	public void serializeWithCode(Object obj, PlanetWriter writer) throws IOException {
		writer.writeByte(TypeCode.INT);
		serialize(obj, writer);
	}
	
	public Object deserializeUsingClass(Class<?> expected, PlanetReader reader) throws IOException {
		return reader.readInt();
	}

	public Object deserializeUsingCode(byte code, PlanetReader reader) throws IOException {
		return reader.readInt();
	}
}
