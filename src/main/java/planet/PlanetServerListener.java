package planet;

import planet.idl.PlanetLocal;


/**
 * <code>ConnectionListener</code>는 PlanetServer 사이의 연결 상태를 전달 받기 위한
 * 리스너 인터페이스를 정의한다.
 * 
 * <p>PlanetServer에 등록된 리스너 객체는 외부 PlanetServer와의 첫 연결이 생성될 때
 * {@link #onSessionOpened(PlanetSession)}가 호출되고, 마지막 연결이 끊어질 때
 * {@link #onSessionClosed(PlanetSession)}가 호출된다.
 * 
 * @see PlanetServer#addPlanetServerListener(PlanetServerListener)
 * @see PlanetServer#removePlanetServerListener(PlanetServerListener)
 * 
 * @author Kang-Woo Lee
 */
@PlanetLocal
public interface PlanetServerListener {
	/**
	 * 외부 PlanetServer와의 첫 연결이 생성됨을 알린다.
	 * 
	 * @param	session	연결된 PlanetSession 객체.
	 */
	public void onSessionOpened(PlanetSession session);
	
	/**
	 * 외부 PlanetServer와의 마지막 연결이 끊어짐을 알린다.
	 * 
	 * @param	session	단절된 PlanetSession 객체.
	 */
	public void onSessionClosed(PlanetSession session);
}
