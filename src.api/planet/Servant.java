package planet;

import planet.idl.PlanetLocal;


/**
 * <code>Servant</code>는 Planet을 통해 원격 호출되는 객체들을 Planet 서버에게 등록시키기 위해
 * 정의된 인터페이스이다. 즉, PlanetServer는 Servant 인터페이스만을 이용하여 외부에서 호출될 객체로부터
 * 필요한 정보를 얻게 된다.
 * <p>
 * <code>Servant</code>은 {@link PersistentServant} 객체와는 달리 동일 Servant 객체에 대해 각
 * 연결 ({@link PlanetSession}마다 별도로 등록시키게 된다.
 * 또한 해당 servant 객체가 원격 호출되기 위해 객체의 참조가 연결을 통해 상대쪽으로 전달되는 시점
 * (즉, {@literal PlanetOutput}을 통해 serialize되는 시점)에 자동적으로
 * 해당 {@literal Connection}에 등록되기 때문에 {@literal PersistentServant}와 달리 별도로
 * 미리 등록할 필요가 없다.
 * 또한, 등록된 {@literal Servant} 객체들은 소속된 {@literal Connection} 객체가 close될 때 자동적으로
 * 모두 등록 말소 된다.
 * <p>
 * <code>Servant</code>를 구현한 객체의 servant path는 PlanetServer에 의해 자동적으로 생성되고
 * 그 포맷은 다음과 같이 길이 3의 문자열 배열로 구성된다.
 * <center>"conn", &lt;connection key>, &lt;unique></center>
 * 여기서 첫번째 문자열 "conn"은 연결마다 생성된 servant path임을 알리는 구분자 역할을 하고,
 * &lt;connection key>은 소속 연결의 고유키 문자열이며, 마지막으로 &lt;unique>은
 * 연결내의 유일한 식별자를 생성하기 위한 생성된 유일 키이다.
 * 
 * @author Kang-Woo Lee (ETRI)
 */
@PlanetLocal
public interface Servant {
	/**
	 * Servant가 원격에 제공하는 인터페이스들을 반환한다.
	 * 
	 * <p>Planet을 통한 원격 객체 호출은 해당 servant가 제공하는 원격 인터페이스들에서
	 * 정의된 메소드만을 호출할 수 있다.
	 * 
	 * @return	인터페이스 클래스 배열.
	 */
	public Class<?>[] getRemoteInterfaces();
}
