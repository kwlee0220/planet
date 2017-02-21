package etri.planet.serializer.ext;

import java.io.IOException;
import java.nio.ByteBuffer;

import etri.planet.PlanetReader;
import etri.planet.PlanetWriter;
import etri.planet.serializer.Serializer;
import etri.planet.serializer.TypeCode;



public class ByteBufferSerializer implements Serializer {
	@Override
	public byte getTypeCode() {
		return TypeCode.BYTE_BUFFER;
	}
	
	public void serialize(Object obj, PlanetWriter writer) throws IOException {
		if ( obj == null || obj instanceof ByteBuffer ) {
			writer.writeBinary((ByteBuffer)obj);
		}
		else {
			throw new IllegalArgumentException("obj's class was not byte[], class="
												+ obj.getClass());
		}
	}

	public void serializeWithCode(Object obj, PlanetWriter writer) throws IOException {
		if ( obj == null ) {
			writer.writeByte(TypeCode.NULL);
		}
		else {
			writer.writeByte(TypeCode.BYTE_BUFFER);
			serialize(obj, writer);
		}
	}
	
	public Object deserializeUsingClass(Class<?> expected, PlanetReader reader) throws IOException {
		return reader.readByteBuffer();
	}

	public Object deserializeUsingCode(byte code, PlanetReader reader) throws IOException {
		if ( code == TypeCode.NULL ) {
			return null;
		}
		
		return reader.readByteBuffer();
	}
}
