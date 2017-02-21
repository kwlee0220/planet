package etri.planet;

import planet.SystemException;



/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public class IllegalTypeCodeException extends SystemException {
	private static final long serialVersionUID = -5675507120490865551L;

	public IllegalTypeCodeException(String details) {
		super(details);
	}
}
