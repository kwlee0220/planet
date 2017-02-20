package planet;

import planet.idl.PlanetLocal;
import planet.transport.Connection;


/**
 * <code>DisconnectionHandler</code>는 원격 PlanetServer 사이의 연결이 단절된 경우
 * 이를 통보받기 위한 리스너 인터페이스를 정의한다.
 * <p>
 * 연결 단절을 통보 받기 위해서는 대상 연결에 해당 <code>DisconnectionHandler</code>를
 * {@link Connection#addDisconnectionHandler(DisconnectionHandler)}를 통해 리스너를
 * 등록하여야 하고, 해당 연결이 끊어진 경우 등록된 모든 리스너의 {@link #onDisconnected(Connection)}
 * 메소드를 호출하는 방법으로 단절을 통보하게 된다.
 * <br>
 * 단절이 통보는 최대 1번만 전달되고 자동적으로 등록 말소되기 때문에, 대상 원격 PlanetServer와
 * 재 연결 후에는 다시 명시적으로 등록하지 않으면 단절이 통보되지 않는다. 
 * <p>
 * <code>DisconnectionHandler</code> 등록시 이미 해당 연결이 단절된 경우는 바로 해당 리스너의
 * {@link #onDisconnected(Connection)} 메소드가 호출된다.
 * 
 * @author Kang-Woo Lee (ETRI)
 */
@PlanetLocal
public interface DisconnectionHandler {
	/**
	 * 원격 PlanetServer와 연결이 단절됨을 알린다.
	 * 
	 * @param conn	단절된 Connection 객체.
	 */
	public void onDisconnected(Connection conn);
}
