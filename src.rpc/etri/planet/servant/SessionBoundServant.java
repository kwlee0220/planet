package etri.planet.servant;

import planet.PlanetSession;
import planet.Servant;


/**
 * <code>SessionBoundServant</code>는 지정된 하나의 연결({@link PlanetSession})에만 속한
 * servant 인터페이스를 정의한다.
 * <p>
 * <code>SessionBoundServant</code>를 구현한 servant의 경우는
 * 해당 servant가 특정 연결에 연결되거나 단절되는 시점을 통보 받을 수 있다.
 * 연결된 경우는 해당 연결 객체를 접근할 수 있다.
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public interface SessionBoundServant extends Servant {
	/**
	 * 지정된 연결에 바인딩됨을 알린다.
	 * 
	 * @param	session	바인딩될 연결 객체.
	 */
	public void bindToSession(PlanetSession session);
	
	/**
	 * 지정된 연결에서 말소됨을 알린다.
	 */
	public void unbindFromSession();
	
	/**
	 * 현재 바인딩된 연결 객체를 반환한다.
	 * 
	 * @return	연결 객체. 현재 연결에 바인딩되지 않은 경우는 <code>null</code>.
	 */
	public PlanetSession getBoundSession();
}
