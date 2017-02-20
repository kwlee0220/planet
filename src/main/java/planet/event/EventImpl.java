package planet.event;

import java.util.Arrays;

import planet.Event;



/**
 * 본 클래스는 ThreadSafe 하지 않다.
 * 
 * @author Kang-Woo Lee
 */
public class EventImpl implements Event {
	private final String[] m_typeIds;
    private final String[] m_propNames;
    private final Object[] m_propValues;
    
    public EventImpl(String[] typeIds, String[] propNames, Object[] propValues) {
    	m_typeIds = typeIds;
    	m_propNames = propNames;
    	m_propValues = propValues;
    }

	public String[] getEventTypeIds() {
		return m_typeIds;
	}
    
    public Object getProperty(String name) {
    	if ( name == null ) {
    		throw new NullPointerException("Property name was null");
    	}
    	
    	for ( int i =0; i < m_propNames.length; ++i ) {
    		if ( m_propNames[i].equals(name) ) {
    			return m_propValues[i];
    		}
    	}
    	
		throw new IllegalArgumentException("Property not found: name=" + name);
    }

	public String[] getPropertyNameAll() {
		return m_propNames;
	}
	
	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		else if ( obj == null || obj.getClass() != getClass() ) {
			return false;
		}
		
		EventImpl other = (EventImpl)obj;
		
		return Arrays.equals(m_typeIds, other.m_typeIds)
				&& Arrays.equals(m_propNames, other.m_propNames)
				&& Arrays.equals(m_propValues, other.m_propValues);
	}
	
	@Override
	public int hashCode() {
    	int hash = 17;

    	hash = (31 * hash) + Arrays.hashCode(m_typeIds);
    	hash = (31 * hash) + Arrays.hashCode(m_propNames);
    	hash = (31 * hash) + Arrays.hashCode(m_propValues);

    	return hash;
	}
    
    public String toString() {
    	StringBuilder builder = new StringBuilder();
    	
    	for ( String id: m_typeIds ) {
    		int start = id.lastIndexOf('.');
    		if ( start >= 0 ) {
    			id = id.substring(start+1);
    		}
    		
			builder.append(id).append(',');
    	}
    	builder.setLength(builder.length()-1);
    	builder.append(":[");
    	
    	int remains = m_propNames.length;
        for ( int i =0; i < m_propNames.length; ++i ) {
        	Object value = m_propValues[i];
        	
        	builder.append(m_propNames[i]).append('=');
        	if ( value instanceof byte[] ) {
        		builder.append("byte[").append(((byte[])value).length).append("]");
        	}
        	else {
        		builder.append(value).append("");
        	}
        	
        	if ( --remains > 0 ) {
        		builder.append(",");
        	}
        }
        builder.append(']');
        
        return builder.toString();
    }
}