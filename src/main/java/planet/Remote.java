package planet;

import planet.idl.PlanetLocal;


/**
 * <code>Remote</code>는 Planet 인터페이스를 구현한 원격 (또는 지역도 가능) servant 객체의
 * 참조를 정의한 인터페이스이다.
 * 
 * <p><code>Remote</code>는 다음과 같은 요소들로 구성되어 있다.
 * <ul>
 * 	<li> 대상 원격 객체의 URL
 * 	<li> 대상 원격 객체를 관리하는 planet 호스트 이름 (PHN)
 * 	<li> 대상 원격 객체의 planet path.
 * 	<li> 대상 원격 객체의 원격 타입.
 * </ul> 
 * 
 * @author Kang-Woo Lee (ETRI)
 */
@PlanetLocal
public interface Remote {
	/**
	 * 원격 객체의 URL을 반환한다.
	 * <p>
	 * 원격 PLANET 객체의 URL은 "planet://<host>:<port>/<path>" 구조를 갖는 URL 형태를 따른다.
	 */
	public String getUri();
	
	/**
	 * 원격 객체를 갖고 있는 planet server의 의 접속 키를 반환한다.
	 * 
	 * @return	원격 객체의 planet server 접속 키.
	 */
	public String getPlanetId();
	
	/**
	 * 원격 서버 내의 servant 객체의 path를 반환한다.
	 * 
	 * @return	servant 객체의 path.
	 */
	public String getServantPath();

	/**
	 * 원격 객체의 합병 타입 식별자를 반환한다.
	 * 
	 * @return	합병된 타입 식별자
	 */
	public String getTypeNames();
	
	/**
	 * 원격 객체의 타입을 반환한다.
	 * 
	 * @return	원격 객체의 타입.
	 */
	public Class<?>[] getTypes();
}
