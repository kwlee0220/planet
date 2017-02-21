package etri.planet;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import planet.IllegalOperationException;
import planet.Remote;
import planet.Servant;
import planet.ServantNotFoundException;
import planet.UndeclaredTypeException;
import planet.idl.PlanetEvent;
import planet.idl.PlanetValue;

import event.Event;


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public abstract class InvocationMessage extends AbstractPlanetMessage {
	public final PlanetServerImpl m_planet;
	public String m_path;
	protected Servant m_servant;
	public Method m_method;
	public Object[] m_arguments;
	
	protected InvocationMessage(PlanetServerImpl planet, byte code, String path, Method method,
								Object[] args) {
		super(new PlanetHeader(code, -1));
		
		m_planet = planet;
		m_path = path;
		m_method = method;
		m_arguments = args;
	}
	
	public InvocationMessage(PlanetServerImpl planet, PlanetHeader header) {
		super(header);
		
		m_planet = planet;
	}
	
	// 1. servant path string
	// 2. 호출 대상 인터페이스의 fully qualified name
	// 3. 호출 대상 메소드 이름 (encoded with paramter information)
	// 4. 호출시 사용할 인자 값들 if any.

	private static final Object[] EMPTY_PARAMS = new Object[0];
	@Override
	public void readPayload(PlanetReader reader) throws IOException {
		try {
			// Input stream에서 servant 경로명을 읽어 해당 servant를 찾는다.
			m_path = reader.readString();	
			m_servant = m_planet.getServant(m_path);
		}
		catch ( ServantNotFoundException e ) {	// 대상 servant가 등록되어 있지 않은 경우.
			throw new ServantNotFoundException(reader.getPlanetSession().getPlanetServer().getTransportManager().getId() + ":" + m_path);
		}
		
		// Input stream에서 호출 대상 인터페이스 이름과 대상 메소드 이름을 읽어
		// 해당 메소드를 로드한다.
		String intfcName = reader.readString();	// 대상 인터페이스 클래스 이름
		String opId = reader.readString();		// Encoding된 메소드 이름
		try {
			// 인터페이스 클래스 이름에 해당하는 얻어 해당 클래스를 찾고,
			// 대상 인터페이스에서 주어진 메소드를 검색한다.
			Class<?> intfc = Class.forName(intfcName, true, m_servant.getClass().getClassLoader());
			m_method = findMethod(intfc, opId);
		}
		catch ( NoSuchMethodException e ) {		// 해당 메소드가 없는 경우.
			throw new IllegalOperationException("method not found: intfc=" + intfcName + ", op=" + opId);
		}
		catch ( SecurityException e ) {
			throw new IllegalOperationException(opId);
		}
		catch ( ClassNotFoundException e ) {	// 인터페이스 이름에 해당하는 클래스가 없는 경우.
			throw new UndeclaredTypeException("intfc=" + intfcName);
		}
		
		// 검색된 메소드 정보를 이용하여 호출시 전달할 인자 값을 input stream에서 읽는다.
		int paramCount = reader.readInt();
		if ( paramCount > 0 ) {
			Class<?>[] paramTypes = m_method.getParameterTypes();
			
			m_arguments = new Object[paramCount];
			for ( int i =0; i < paramCount; ++i ) {
				m_arguments[i] = reader.readObject(paramTypes[i]);
			}
		}
		else {
			m_arguments = EMPTY_PARAMS;
		}
	}

	@Override
	public void writePayload(PlanetWriter output) throws IOException {
		output.writeString(m_path);
		writeMethod(m_method, output);
		
		if ( m_arguments == null || m_arguments.length == 0 ) {
			output.writeInt(0);
		}
		else {
			output.writeInt(m_arguments.length);
			
			for ( int i =0; i < m_arguments.length; ++i ) {
				output.writeObjectWithCode(m_arguments[i]);
			}
		}
	}
	
	private void writeMethod(Method method, PlanetWriter out) throws IOException {
		out.writeString(method.getDeclaringClass().getName());
		out.writeString(encodeMethod(method));
	}
	
	public String toInvocationString() {
		return RpcUtils.toInvocationString(m_method, m_arguments);
	}

	private Method findMethod(Class<?> intfc, String methodName) throws ClassNotFoundException,
														SecurityException, NoSuchMethodException {
		int idx = methodName.indexOf('(');
		String name = methodName.substring(0, idx);
		String signature = methodName.substring(idx+1);
		Class<?>[] paramTypes = decodeTypes(signature, intfc.getClassLoader());

		return intfc.getMethod(name, paramTypes);
	}

	private Class<?>[] decodeTypes(String encoded, ClassLoader loader)
		throws ClassNotFoundException {
		List<Class<?>> typeList = new ArrayList<Class<?>>();

		SignatureParser parser = new SignatureParser(encoded, loader);
		while ( true ) {
			Class<?> clazz = parser.parseNextClass();
			if ( clazz == null ) {
				break;
			}

			typeList.add(clazz);
		}

		if ( typeList.size() == 0 ) {
			return null;
		}
		else {
			Class<?>[] types = new Class<?>[typeList.size()];
			return typeList.toArray(types);
		}
	}

	private static String encodeMethod(Method method) {
		final Class<?>[] paramTypes = method.getParameterTypes();

		StringBuilder builder = new StringBuilder(method.getName());
		builder.append('(');

		for ( int i =0; i < paramTypes.length; ++i ) {
			encodeType(paramTypes[i], builder);
		}
		builder.append(')');

		return builder.toString();
	}

	private static void encodeType(Class<?> clazz, StringBuilder builder) {
		if ( clazz == String.class ) {
			builder.append('T');
		}
		else if ( clazz == int.class ) {
			builder.append('4');
		}
		else if ( clazz == byte[].class ) {
			builder.append('N');
		}
		else if ( clazz == InputStream.class ) {
			builder.append('R');
		}
		else if ( clazz == boolean.class ) {
			builder.append('Z');
		}
		else if ( clazz == byte.class ) {
			builder.append('1');
		}
		else if ( clazz == char.class ) {
			builder.append('C');
		}
		else if ( clazz == short.class ) {
			builder.append('2');
		}
		else if ( clazz == long.class ) {
			builder.append('8');
		}
		else if ( clazz == float.class ) {
			builder.append('F');
		}
		else if ( clazz == double.class ) {
			builder.append('D');
		}
		else if ( clazz == Event.class ) {
			builder.append("E;");
		}
		else if ( clazz.isAnnotationPresent(PlanetValue.class) ) {
			builder.append('V').append(clazz.getName()).append(';');
		}
		else if ( clazz.isArray() ) {
			builder.append("[A");
			encodeType(clazz.getComponentType(), builder);
		}
		else if ( Vector.class.isAssignableFrom(clazz) ) {
			builder.append("[V");
		}
		else if ( List.class.isAssignableFrom(clazz) ) {
			builder.append("[L");
		}
		else if ( Set.class.isAssignableFrom(clazz) ) {
			builder.append("[S");
		}
		else if ( Map.class.isAssignableFrom(clazz) ) {
			builder.append("[M");
		}
		else if ( Enum.class.isAssignableFrom(clazz) ) {
			builder.append('M').append(clazz.getName()).append(';');
		}
		else if ( Throwable.class.isAssignableFrom(clazz) ) {
			builder.append('X').append(clazz.getName()).append(';');
		}
		else if ( Event.class.isAssignableFrom(clazz) || clazz.isAnnotationPresent(PlanetEvent.class) ) {
			builder.append('E').append(clazz.getName()).append(';');
		}
		else if ( Remote.class.equals(clazz) ) {
			builder.append("L;");
		}
		else if ( clazz == Object.class ) {
			builder.append("A");
		}
		else {
			builder.append('L').append(clazz.getName()).append(';');
		}
	}
}