package etri.planet.serializer;

import java.io.IOException;

import etri.planet.PlanetReader;
import etri.planet.PlanetWriter;


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public class EnumSerializer implements Serializer {
	@Override
	public byte getTypeCode() {
		return TypeCode.ENUM;
	}
	
	public void serialize(Object obj, PlanetWriter out) throws IOException {
		out.writeEnum((Enum<?>)obj);
	}

	public void serializeWithCode(Object obj, PlanetWriter out) throws IOException {
		if ( obj == null ) {
			out.writeByte(TypeCode.NULL);
		}
		else {
			out.writeByte(TypeCode.ENUM);
			serialize(obj, out);
		}
	}

	public Object deserializeUsingClass(Class<?> expected, PlanetReader in) throws IOException {
		return in.readEnum(expected);
	}

	public Object deserializeUsingCode(byte code, PlanetReader in) throws IOException {
		if ( code == TypeCode.NULL ) {
			return null;
		}
		else {
			Class<?> enumClass = in.readClassFromString();
			return deserializeUsingClass(enumClass, in);
		}
	}
}
