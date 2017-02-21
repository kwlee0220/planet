package etri.planet.serializer;

import java.io.IOException;

import planet.Servant;

import etri.planet.PlanetInternalUtils;
import etri.planet.PlanetReader;
import etri.planet.PlanetWriter;


/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
public class ServantSerializer implements Serializer {
	@Override
	public byte getTypeCode() {
		return TypeCode.REMOTE;
	}

	public void serialize(Object obj, PlanetWriter writer) throws IOException {
		if ( !(obj instanceof Servant) ) {
			obj = PlanetInternalUtils.wrapToServant(obj);
		}

		writer.writeServant((Servant)obj);
	}

	public void serializeWithCode(Object obj, PlanetWriter writer) throws IOException {
		if ( obj == null ) {
			writer.writeByte(TypeCode.NULL);
		}
		else {
			if ( !(obj instanceof Servant) ) {
				obj = PlanetInternalUtils.wrapToServant(obj);
			}

			writer.writeByte(TypeCode.REMOTE);
			writer.writeServant((Servant)obj);
		}
	}

	public Object deserializeUsingClass(Class<?> expected, PlanetReader reader) throws IOException {
		return reader.readRemote(expected);
	}

	public Object deserializeUsingCode(byte code, PlanetReader reader) throws IOException {
		if ( code == TypeCode.NULL ) {
			return null;
		}
		else {
			return reader.readRemote();
		}
	}
}