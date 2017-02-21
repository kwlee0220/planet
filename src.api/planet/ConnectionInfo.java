package planet;

import planet.idl.PlanetField;
import planet.idl.PlanetValue;


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
@PlanetValue
public class ConnectionInfo {
	@PlanetField(ordinal=0) public String m_id;
	@PlanetField(ordinal=1) public boolean m_active;
}
