package etri.planet.serializer;

import java.io.IOException;
import java.io.InputStream;

import etri.planet.PlanetReader;
import etri.planet.PlanetWriter;


/**
 * 
 * @author Kang-Woo Lee
 */
public class StreamSerializer implements Serializer {
	@Override
	public byte getTypeCode() {
		return TypeCode.STREAM;
	}
	
	public void serialize(Object obj, PlanetWriter out) throws IOException {
		if ( obj instanceof InputStream ) {
			out.writeInputStream((InputStream)obj);
		}
		else {
			throw new IllegalArgumentException(
						String.format("Invalid type-code: expected(%s)<->actual(%s)",
						InputStream.class.getName(), obj.getClass().getName()));
		}
	}

	public void serializeWithCode(Object obj, PlanetWriter buf) throws IOException {
		if ( obj == null ) {
			buf.writeByte(TypeCode.NULL);
		}
		else {
			buf.writeByte(TypeCode.STREAM);
			serialize(obj, buf);
		}
	}
	
	public Object deserializeUsingClass(Class<?> expected, PlanetReader reader) throws IOException {
		return reader.readInputStream();
	}

	public Object deserializeUsingCode(byte code, PlanetReader reader) throws IOException {
		if ( code == TypeCode.NULL ) {
			return null;
		}
		
		return reader.readInputStream();
	}
}