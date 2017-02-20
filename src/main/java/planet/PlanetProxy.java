package planet;

import planet.idl.PlanetLocal;



/**
 * <code>PlanetProxy</code>는 PlanetServer를 통해 생성된 모든 proxy 객체가
 * 공통적으로 지원하는 타입을 정의한다.
 * <br>모든 proxy 객체는 반드시 <code>PlanetProxy</code>를 implement하고,
 * 사용자는 <code>PlanetProxy</code>를 통해 관련된 {@link planet.PlanetServer},
 * {@link planet.PlanetSession} 등과 같은 정보를 얻을 수 있다.
 * 
 * @author Kang-Woo Lee (ETRI)
 */
@PlanetLocal
public interface PlanetProxy {
	/**
	 * Proxy 객체가 사용하는 PlanetServer 객체를 반환한다.
	 * 
	 * @return	PlanetServer 객체
	 */
	public PlanetServer getPlanetServer();
	
	/**
	 * 본 프록시 객체가 속한 {@link PlanetSession} 객체를 반환한다.
	 * <p>
	 * 프록시가 특정 세션에 바인딩되어 있지 않은 경우는 인자 <code>create</code>에 따라
	 * 달라진다. <code>true</code>이면 새로운 세션을 생성하여 바인딩시키고 이를
	 * 반환하게 되고, <code>false</code>인 경우는 <code>null</code>을 반환한다.
	 * 
	 * @param create	PlanetSession이 없는 경우 생성 여부.
	 * @return	PlanetSession 객체
	 */
	public PlanetSession getPlanetSession(boolean create);
	
	/**
	 * 본 프록시 객체가 가리키는 원격 객체의 참조를 반환한다.
	 * 
	 * @return	대상 객체의 원격 참조.
	 */
	public Remote getRemote();
	
	public void removeServant();
	
	/**
	 * 지정된 타입을 지원하는 Proxy 객체를 생성한다.
	 * <p>생성된 proxy 객체는 자동적으로 {@link PlanetProxy} 타입을
	 * implement하게 된다.
	 * 
	 * @param newType	지원할 타입 클래스.
	 * @return	proxy 객체.
	 */
	public <T> T recast(Class<T> newType);
}
