package etri.planet;

import java.lang.reflect.Method;


/**
 * 
 * @author Kang-Woo Lee
 */
public class RpcUtils {
	private RpcUtils() { }
	
	public static String toInvocationString(Method method, Object[] args) {
		StringBuilder builder = new StringBuilder();
		
		builder.append(method.getDeclaringClass().getSimpleName());
		builder.append('.').append(method.getName()).append('(');
		
		if ( args != null && args.length > 0 ) {
			int i = -1;
			while ( ++i < args.length-1 ) {
				builder.append(getShortString(args[i])).append(',');
			}
			builder.append(getShortString(args[i]));
		}
		builder.append(')');
		
		return builder.toString();
	}
	
	private static final int MAX_STR_LENGTH = 20;
	private static String getShortString(Object value) {
		try {
			if ( value instanceof String ) {
				String str = (String)value;
				if ( str.length() > MAX_STR_LENGTH ) {
					return str.substring(0, MAX_STR_LENGTH) + "...";
				}
				else {
					return str;
				}
			}
			else {
				return String.valueOf(value);
			}
		}
		catch ( Exception e ) {
			return "failed";
		}
	}

//	private static final String SUFFIX_ADAPTOR = "Adaptor";
//	public static Class<?> toAdaptorFromUser(Class<?> intfc) throws ClassNotFoundException {
//		return Class.forName(intfc.getName() + SUFFIX_ADAPTOR, true, intfc.getClassLoader());
//	}
}
