package planet;

import java.lang.reflect.Method;


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public interface PlanetFaultListener {
	public void onFaultFound(Remote remote, Method method, Throwable error, String tag);
}
