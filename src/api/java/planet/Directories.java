package planet;

import java.lang.reflect.Method;


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public class Directories {
	private Directories() {
		throw new AssertionError("Should not be called: class=" + Directories.class);
	}

	public static Method GET_SERVANTS;
	static {
		try {
			GET_SERVANTS = Directory.class.getMethod("getServant", String.class);
		}
		catch ( Exception e ) {
			System.err.println(e);
			throw new RuntimeException(e);
		}
	}
}
