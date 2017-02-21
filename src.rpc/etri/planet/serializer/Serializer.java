package etri.planet.serializer;

import java.io.IOException;

import etri.planet.PlanetReader;
import etri.planet.PlanetWriter;


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public interface Serializer {
	public byte getTypeCode();
	
	public void serialize(Object obj, PlanetWriter writer) throws IOException;
	public void serializeWithCode(Object obj, PlanetWriter writer) throws IOException;
	
	public Object deserializeUsingClass(Class<?> expected, PlanetReader reader) throws IOException;
	public Object deserializeUsingCode(byte code, PlanetReader reader) throws IOException;
}