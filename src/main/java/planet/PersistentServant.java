package planet;

import planet.idl.PlanetLocal;


/**
 * <code>PersistentServant</code>는 복수개의 원격 PlanetServer들로 부터 호출될 객체의 인터페이스를 정의한다.
 * <p>
 * 다수의 PlanetServer로부터 호출된 servant를 정의하기 위해서는 기존 {@link Servant}에서 정의된
 * 인터페이스 외에 servant에게 부여할 servant 경로가 정의되어야 한다.
 * Servant 경로는 문자열의 배열로 정의된다.
 * 
 * @author Kang-Woo Lee (ETRI)
 */
@PlanetLocal
public interface PersistentServant extends Servant {
	/**
	 * 객체의 servant 경로를 반환한다.
	 * 
	 * @return	원격 경로.
	 */
	public String getServantPath();
}
