package planet;

import java.lang.reflect.Method;


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public class PersistentServants {
	private PersistentServants() {
		throw new AssertionError("Should not be called: class=" + PersistentServants.class);
	}

	public static Method GET_SERVANT_PATH;
	static {
		try {
			GET_SERVANT_PATH = PersistentServant.class.getMethod("getServantPath", (Class[])null);
		}
		catch ( Exception e ) {
			System.err.println(e);
			throw new RuntimeException(e);
		}
	}
}
