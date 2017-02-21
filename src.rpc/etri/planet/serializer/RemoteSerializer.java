package etri.planet.serializer;

import java.io.IOException;

import planet.Remote;

import etri.planet.PlanetReader;
import etri.planet.PlanetWriter;


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public class RemoteSerializer implements Serializer {
	@Override
	public byte getTypeCode() {
		return TypeCode.REMOTE;
	}
	
	public void serialize(Object obj, PlanetWriter writer) throws IOException {
		writer.writeRemote((Remote)obj);
	}

	public void serializeWithCode(Object obj, PlanetWriter writer) throws IOException {
		if ( obj == null ) {
			writer.writeByte(TypeCode.NULL);
		}
		else {
			writer.writeByte(TypeCode.REMOTE);
			serialize((Remote)obj, writer);
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
			return reader.readRemote_();
		}
	}
}