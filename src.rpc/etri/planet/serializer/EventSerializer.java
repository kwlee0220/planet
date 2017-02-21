package etri.planet.serializer;

import java.io.IOException;

import etri.planet.PlanetReader;
import etri.planet.PlanetWriter;
import event.Event;


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public class EventSerializer implements Serializer {
	@Override
	public byte getTypeCode() {
		return TypeCode.EVENT;
	}
	
	public void serialize(Object obj, PlanetWriter writer) throws IOException {
		writer.writeEvent((Event)obj);
	}

	public void serializeWithCode(Object obj, PlanetWriter writer) throws IOException {
		if ( obj == null ) {
			writer.writeByte(TypeCode.NULL);
		}
		else {
			writer.writeByte(TypeCode.EVENT);
			serialize(obj, writer);
		}
	}
	
	public Object deserializeUsingClass(Class<?> expected, PlanetReader reader) throws IOException {
		return reader.readEvent();
	}

	public Object deserializeUsingCode(byte code, PlanetReader reader) throws IOException {
		if ( code == TypeCode.NULL ) {
			return null;
		}
		
		return deserializeUsingClass(null, reader);
	}
}