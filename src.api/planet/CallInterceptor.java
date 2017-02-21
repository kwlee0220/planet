package planet;

import java.lang.reflect.InvocationHandler;


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public interface CallInterceptor extends InvocationHandler {
	public Object getSourceObject();
}
