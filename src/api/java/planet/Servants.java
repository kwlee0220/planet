package planet;

import java.lang.reflect.Method;


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public class Servants {
	private Servants() {
		throw new AssertionError("Should not be called: class=" + Servants.class);
	}

	public static Method GET_REMOTE_INTERFACES;
	static {
		try {
			GET_REMOTE_INTERFACES = Servant.class.getMethod("getRemoteInterfaces", (Class[])null);
		}
		catch ( Exception e ) {
			System.err.println(e);
			throw new RuntimeException(e);
		}
	}
}
