package planet.transport;

import java.io.IOException;
import java.nio.channels.ServerSocketChannel;
import java.util.concurrent.Executor;


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public interface TransportManager {
	/** 최대 메시지 불럭 크기. 주어진 크기보다 큰 메시지의 경우는 다수의 메시지 블럭으로 분할된다.*/
	public static final int MAX_BLOCK_SIZE = 16 << 10;
	
	public static final int BUFFER_COUNT = 3;
	
	public String getId();
	
	/**
	 * TransportManager를 시작시킨다.
	 * <p>
	 * TransportManager가 시작되면 주어진 포트로의 TCP 연결을 청취하여 외부로부터의
	 * 연결 요청 처리가 시작된다.
	 * 또한 메시지의 송수신에 대한 정보는 주어진 {@link TransportListener}를 통해 통보된다.
	 * 
	 * @param port	TCP 연결 청취 포트. 0인 경우는 시스템에서 유휴 포트를 자동으로
	 * 						선정하여 사용한다.
	 * @param executor	TransportManager가 사용할 Executor 객체.
	 * @param listener		TransportManager 청취자 객체.
	 * @return	개방된 TCP 연결 청취 포트 번호. 특히 <code>listenerPort</code>를 0으로 호출한 경우,
	 * 			실제 사용된 포트 번호를 반환한다.
	 * @throws IOException	TransportManager 시작 중 TCP 연결 청취 포트 개방시 오류가 발생된 경우.
	 * @throws NullPointerException	<code>executor</code> 또는 <code>listener</code>가
	 * 								<code>null</code>인 경우.
	 */
    public int start(int port, Executor executor, TransportListener listener)
    	throws IOException;
    
    /**
     * TransportManager를 종료시킨다.
     * <p>
     * TransportManager가 종료되면 더 이상의 TCP 연결은 허용되지 않고, 연결된 모든 connection들은
     * 끊어진다.
     */
    public void stop();
    
    /**
     * TransportManager 내부에서 사용하는 서버 소켓 채널을 반환한다.
     * 
     * @return	서버 소켓 채널
     */
    public ServerSocketChannel getServerSocketChannel();
	
    /**
     * 현재 적용중인 최대 연결 대기 시간(단위: milli-second)을 반환한다.
     * <p>
     * 연결 시간 제한은 타 시스템과의 TCP 연결을 요청한 경우 최대 대기 시간으로 이 시간이 초과된 경우는
     * 연결 요청은 철회되고, {@link java.io.IOException} 예외가 발생된다.
     * 만일 반환된 시간이 0인 경우는 무제한 대기를 의미한다. 
     * 
     * @return	적용중인 최대 연결 대기 시간 (단위: milli-second).
     */
	public int getConnectTimeout();
	
	/**
	 * 주어진 시간을 최대 연결 대기 시간(단위: milli-second)으로 설정한다.
	 * <p>
     * 연결 시간 제한은 타 시스템과의 TCP 연결을 요청한 경우 최대 대기 시간으로 이 시간이 초과된 경우는
     * 연결 요청은 철회되고, {@link java.io.IOException} 예외가 발생된다.
     * 만일 대기 시간이 0인 경우는 무한 대기를 의미한다.
     * 
     * @param timeout	설정할 연결 대기 시간 (단위: milli-second)
     * @throws IllegalArgumentException	<code>timeout</code>이 음수인 경우.
	 */
	public void setConnectTimeout(int timeout) throws IllegalArgumentException;
	
	/**
	 * 현재 적용 중인 heartbeat 전송 주기(단위: milli-second)를 반환한다.
	 * <p>
	 * heartbeat 전송 주기는 주어진 연결에서 heartbeat 송수신 없이 허락되는
	 * 최대 메시지 수신 대기 시간으로, 이 기간동안 메시지 수신이 없는 경우 TransportManager는
	 * 강제로 heartbeat 메시지를 연결 상대에게 전송하여 강제로 heartbeat 응답 메시지가 수신되도록 한다.
	 * 만일 다음 heartbeat 전송 주기 동안 heartbeat 응답 메시지를 포함하여 메시지 수신이
	 * 없는 경우는 연결 상대가 끊어진 것으로 간주하고 강제로 연결이 끊어지게 된다.
	 * <br>대기 시간이 0인 경우는 무한 대기를 의미한다.
     * 
     * @return	적용중인 heartbeat 전송 주기 (단위: milli-second).
	 */
	public long getHeartbeatInterval();
	
	/**
	 * heartbeat 전송 주기를 설정한다.
	 * <p>
	 * heartbeat 전송 주기는 주어진 연결에서 heartbeat 송수신 없이 허락되는
	 * 최대 메시지 수신 대기 시간으로, 이 기간동안 메시지 수신이 없는 경우 TransportManager는
	 * 강제로 heartbeat 메시지를 연결 상대에게 전송하여 강제로 heartbeat 응답 메시지가 수신되도록 한다.
	 * 만일 다음 heartbeat 전송 주기 동안 heartbeat 응답 메시지를 포함하여 메시지 수신이
	 * 없는 경우는 연결 상대가 끊어진 것으로 간주하고 강제로 연결이 끊어지게 된다.
	 * <br>대기 시간이 0인 경우는 무한 대기를 의미한다.
	 * 
	 * @param interval	heartbeat 전송 주기 (단위: milli-second)
	 * @throws IllegalArgumentException	<code>interval</code>이 음수인 경우.
	 */
    public void setHeartbeatInterval(long interval) throws IllegalArgumentException;

    /**
     * 주어진 Planet 호스트 이름(Planet Host Name: phn)에 해당하는 호스트와의 연결 여부를 반환한다.
     * 
     * @param phn	대상 Planet 호스트 이름
     * @return	대상 호스트와의 연결이 존재하는 경우는 <code>true</code>
     * 			그렇지 않은 경우는 <code>false</code>.
     * @throws NullPointerException	<code>phn</code>이 <code>null</code>인 경우.
     */
	public boolean existsConnection(String phn) throws NullPointerException;
	
	/**
	 * 주어진 Planet 호스트 이름(Planet Host Name: phn)에 해당하는 호스트와의 연결 객체를 반환한다.
	 * <p>
	 * 만일 해당 호스트와의 연결이 없는 경우는 <code>create</code> 값에 따라 <code>true</code>인 경우는
	 * <code>null</code>을, 그리고 <code>false</code>인 경우는 해당 호스트와 연결을 시도하여
	 * 생성된 연결 객체를 반환하게 된다.
	 * 
	 * @param phn	대상 Planet 호스트 이름.
	 * @param create	해당 호스트와 연결이 없는 경우 연결 생성 시도 여부. <code>true</code>인 경우
	 * 					해당 호스트와의 연결을 시도하고, 그렇지 않은 경우는 <code>null</code>를 반환.
	 * @throws IOException	연결 시도 중 오류가 발생된 경우.
	 * @throws InterruptedException	연결 시도 후 대기 중 쓰레드가 interrupt된 경우.
     * @throws NullPointerException	<code>phn</code>이 <code>null</code>인 경우.
	 */
	public Connection getConnection(String phn, boolean create)
		throws IOException, InterruptedException, NullPointerException;
}
