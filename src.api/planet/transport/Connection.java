package planet.transport;

import java.io.IOException;

import planet.DisconnectionHandler;
import planet.idl.PlanetLocal;



/**
 * 
 * @author Kang-Woo Lee
 */
@PlanetLocal
public interface Connection {
	/**
	 * 현 연결을 단절시킨다.
	 */
	public void close();
	
	/**
	 * 본 연결에 바인딩된 상대 Planet의 식별자를 반환한다.
	 * 
	 * @return	연결 상대 Planet 식별자
	 */
	public String getId();
	
	/**
	 * 본 연결의 단절 여부를 반환한다.
	 * 
	 * @return	단절된 경우는 <code>true</code>, 그렇지 않은 경우는 <code>false</code>.
	 */
	public boolean isClosed();
	
	/**
	 * 본 연결에 대해 연결을 요청하였는지 여부를 반환한다.
	 * 
	 * @return	요청된 연결 여부.
	 */
	public boolean isActive();
	
	/**
	 *  본 연결의 최대 유휴 대기 시간을 반환한다.
	 */
	public int getMaxIdleSeconds();
	public void setMaxIdleSeconds(int seconds);
	
	public Object getAttachment();
	
	public void setAttachment(Object att);

	public String getDescription();

	public void setDescription(String description);

	/**
	 * 출력 채널을 할당한다.
	 * <p>
	 * 할당된 출력 채널을 폐쇄하는 경우는  채널 객체의 {@link OutputChannel#close(boolean)} 메소드를
	 * 호출한다.
	 * 
	 * @return	생성된 출력 채널 객체.
	 * @throws IOException 출력 채널 생성 중 오류가 발생된 경우.
	 */
	public OutputChannel allocateOutputChannel() throws IOException;
	
	/**
	 * 단절 리스너({@link DisconnectionHandler})를 등록시킨다.
	 * <p>
	 * <code>DisconnectionHandler</code>는 등록된 연결이 폐쇄되는 경우 호출되는 핸들러 객체이다.
	 * Connection은 연결이 닫힐 때 등록된 모든 <code>DisconnectionHandler</code> 객체의
	 * {@link DisconnectionHandler#onDisconnected(Connection)} 메소드를 호출한다.
	 * 메소드 호출 후 해당 리스너의 등록은 삭제되어 메소드 호출을 등록 후 최대 한 번만 호출되게 된다.
	 * <p>
	 * 단절 리스너 등록시 해당 연결이 이미 단절된 상태인 경우는 바로 단절 리스너의
	 * {@link DisconnectionHandler#onDisconnected(Connection)}가 호출된다.
	 * 
	 * @param handler	등록할 DisconnectionHandler 객체.
	 * @throws NullPointerException	리스너 객체가 <code>null</code>인 경우.
	 */
	public void addDisconnectionHandler(DisconnectionHandler handler);
	
	public void removeDisconnectionHandler(DisconnectionHandler handler);
}
