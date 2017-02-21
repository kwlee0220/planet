package etri.planet.serializer;

import java.io.IOException;

import etri.planet.PlanetReader;
import etri.planet.PlanetWriter;

/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public class ExceptionSerializer implements Serializer {
	@Override
	public byte getTypeCode() {
		return TypeCode.EXCEPTION;
	}
	
	public void serialize(Object obj, PlanetWriter out) throws IOException {
		out.writeException((Throwable)obj);
	}

	public void serializeWithCode(Object obj, PlanetWriter out) throws IOException {
		if ( obj == null ) {
			out.writeByte(TypeCode.NULL);
		}
		else {
			out.writeByte(TypeCode.EXCEPTION);
			serialize(obj, out);
		}
	}
	
	public Object deserializeUsingClass(Class<?> expected, PlanetReader in) throws IOException {
		return in.readException();
	}

	public Object deserializeUsingCode(byte code, PlanetReader in) throws IOException {
		if ( code == TypeCode.NULL ) {
			return null;
		}
		else {
			return deserializeUsingClass(Throwable.class, in);
		}
	}
}
