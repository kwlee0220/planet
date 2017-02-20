package planet;

import planet.idl.PlanetLocal;



/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
@PlanetLocal
public interface Event {
	/**
	 * 이벤트가 지원하는 모든 이벤트 타입 식별자를 반환한다.
	 * 
	 * @return	타입 식별자 배열.
	 */
	public String[] getEventTypeIds();
    
	/**
	 * 이벤트에 포함된 모든 속성 이름들을 반환한다.
	 * 
	 * @return	속성 이름 배열.
	 */
	public String[] getPropertyNameAll();
    
	/**
	 * 주어진 속성 이름의 속성 값을 반환한다.
	 * 
	 * @param name	속성 이름
	 * @return	속성 값.
	 */
    public Object getProperty(String name);
    
    public default String getPropertyAsString(String name) {
    	return (String)getProperty(name);
    }
    
    public default int getPropertyAsInt(String name) {
    	return (Integer)getProperty(name);
    }
}
