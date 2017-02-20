package planet.transport;


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public interface TransportListener {
	/**
	 * 주어진 연결이 새로 생성되었음을 알린다.
	 * 
	 * @param conn	생성된 연결 객체.
	 */
	public void onConnected(Connection conn);
	
	/**
	 * 주어진 연결이 단절되었음을 알린다.
	 * 
	 * @param conn	단절된 연결 객체
	 */
	public void onDisconnected(Connection conn);
	
	/**
	 * 연결 상대에서 새로운 메시지 전송으로 새 스트림이 생성되었음을 알린다.
	 * <p>
	 * 스트림 생성되면 전송될 메시지를 내용을 읽을 수 있는 입력 스트림 객체가 생성된다.
	 * 메시지의 길이 {@link TransportManager#MAX_BLOCK_SIZE} 보다 긴 경우
	 * 메시지가 복수개의 블럭으로 분할되어 도착하기 때문에 메시지 수신시 blocking이
	 * 발생될 수 있다.
	 * 
	 * @param inChannel	스트림 생성 요청으로 생성된 입력 스트림 객체.
	 * @param nonblocking	생성된 입력 스트림으로부터 데이타를 읽는 경우 blocking여부.
	 * 						<code>true</code>인 경우는 blocking이 발생되지 않음을 의미하고
	 * 						<code>false</code>인 경우는 메시지 분할으로 메시지 수신 중 blocking이
	 * 						발생될 여지가 있음을 의미.
	 */
	public void onInputChannelCreated(InputChannel inChannel, boolean nonblocking);
}
