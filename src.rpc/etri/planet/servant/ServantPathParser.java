package etri.planet.servant;



/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public class ServantPathParser {
	private static final char DELIM = '/';
	private static final char ESC = '\'';
	
	private final char[] m_path;
	private int m_cursor;
	
	ServantPathParser(String path) {
		m_path = path.toCharArray();
		m_cursor = (path.charAt(0) == DELIM) ?  1 : 0;
	}
	
	String parseNext() throws IllegalArgumentException {
		if ( m_cursor >= m_path.length ) {
			return null;
		}
		else if ( m_path[m_cursor] == ESC ) {
			if ( ++m_cursor >= m_path.length ) {
				throw new IllegalArgumentException("Invalid servantpath=" + new String(m_path));
			}
			
			int end = findChar(ESC, m_cursor);
			if ( end < 0 ) {
				throw new IllegalArgumentException("Invalid servantpath=" + new String(m_path));
			}
			
			String word = new String(m_path, m_cursor, end-1);
			if ( (m_cursor = end + 1) < m_path.length ) {
				if ( m_path[m_cursor] != DELIM ) {
					throw new IllegalArgumentException("Invalid servantpath=" + new String(m_path));
				}
			}
			
			return word;
		}
		else {
			int end = findChar(DELIM, m_cursor);
			if ( end < 0 ) {
				throw new IllegalArgumentException("Invalid servantpath=" + new String(m_path));
			}
			
			String word = new String(m_path, m_cursor, end - m_cursor);
			m_cursor = end + 1;
			
			return word;
		}
	}
	
	private int findChar(char c, int start) {
		while ( start < m_path.length && m_path[start] != c ) {
			++start;
		}
		
		return start;
	}
}
