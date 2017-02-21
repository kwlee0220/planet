package etri.planet;

import java.io.InputStream;
import java.lang.reflect.Array;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import planet.Remote;
import planet.SystemException;

import event.Event;


class SignatureParser {
	char[] m_carr;
	int m_pos;
	ClassLoader m_loader;

	SignatureParser(String signature, ClassLoader loader) {
		m_carr = signature.toCharArray();
		m_pos = 0;
		m_loader = loader;
	}

	Class<?> parseNextClass() throws ClassNotFoundException {
		if ( m_carr[m_pos] == ')' ) {
			return null;
		}

		switch ( m_carr[m_pos++] ) {
			case 'T':
				return String.class;
			case '4':
				return int.class;
			case 'N':
				return byte[].class;
			case 'R':
				return InputStream.class;
			case 'Z':
				return boolean.class;
			case '1':
				return byte.class;
			case 'C':
				return char.class;
			case '2':
				return short.class;
			case '8':
				return long.class;
			case 'F':
				return float.class;
			case 'D':
				return double.class;
			case '[':
				switch ( m_carr[m_pos++] ) {
					case 'A':
						Class<?> compType = parseNextClass();
						return Array.newInstance(compType, 0).getClass();
					case 'L':
						return List.class;
					case 'S':
						return Set.class;
					case 'M':
						return Map.class;
					case 'V':
						return Vector.class;
				}

			case 'L':	// REMOTE
				return parseObjectClass(Remote.class);
			case 'E':	// EVENT
				return parseObjectClass(Event.class);
			case 'X':	// EXCEPTION
				return parseObjectClass(Exception.class);
			case 'V':	// VALUE
				return parseObjectClass(null);
			case 'M':	// ENUM
				return parseObjectClass(null);
			case 'A':	// ANY
				return Object.class;
			default:
				throw new SystemException("Illegal type signature code="
													+ m_carr[m_pos - 1]);
		}
	}

	Class<?> parseObjectClass(Class<?> defaultClass) throws ClassNotFoundException {
		int start = m_pos;
		while ( m_carr[m_pos++] != ';' );

		if ( m_pos == start + 1) {
			return defaultClass;
		}
		else {
			String className = new String(m_carr, start, m_pos - start - 1);

			return Class.forName(className, true, m_loader);
		}
	}
}