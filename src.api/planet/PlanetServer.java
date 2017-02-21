package planet;

import java.io.IOException;
import java.util.concurrent.ExecutorService;

import planet.idl.PlanetLocal;
import planet.transport.Connection;


/**
 * {@literal PlanetServer}는 Planet 통신 관련 지역 서버 인터페이스를 정의한다.
 *
 * <p>
 * {@literal PlanetServer}가 시작되면 외부로부터의 접속을 위해 소켓 리스너가 열린다.
 * 이때 사용되는 IP 주소 값({@link #getPlanetServerHost()})과
 * 포트 번호({@link #getPlanetServerPort()})가 해당 {@literal PlanetServer}의
 * 식별자(또는 키)로 정의된다.
 * {@literal PlanetServer}가 사용하는 리스너의 호스트 IP와 포트 번호는
 * 각각 {@link #setPlanetServerHost(String)}와
 * {@link #setPlanetServerPort(int)} 메소드를 통해 설정해 줄 수 있으며, 별도로 설정하지 않은
 * 경우는 각각 {@link java.net.InetAddress#getLocalHost()}를 통한 IP 주소와
 * 시스템에서 할당한 포트 번호가 사용된다.
 *
 * <p>
 * {@literal PlanetServer}를 통해 원격에서 호출될 객체는 해당 servant 객체가
 * 미리 {@literal PlanetServer}에 등록되어야 한다.
 * {@literal PlanetServer}는 등록된 servant 객체의 인터페이스를 통해 지역 객체가
 * 원격에서 호출되기 위해 필요한 다음과 같은 정보를 얻는다.
 * <ul>
 * 	<li> servant path: 원격에서 지역 객체를 지칭하기 위한 경로명
 * 	<li> remote interfaces: 원격으로 공개할 인터페이스들. 복수 인터페이스 공개도 가능하다.
 * </ul>
 *
 * <h3>Servant 자동 생성 및 등록 </h3>
 *
 * 지역 객체가 {@literal PlanetServer}에 등록되는 방법은 사용자가 명시적으로 등록시키는 방법과
 * 지역 객체의 참조가 외부로 전달되는 경우 {@literal PlanetServer}에 의해 자동적으로
 * 등록되는 방법이 있다.
 * <br>
 * Servant를 명시적으로 등록하는 방법은 {@link #addServant(PersistentServant)} 호출을 통해
 * 이루어지며, 이 경우 등록될 servant는 {@link PersistentServant} 인터페이스를 구현하여야 한다.
 * 이렇게 등록된 servant는 servant 관리자에 의해 관리되며 다수의 원격
 * {@literal PlanetServer}에 의해 호출될 수 있다.
 * <br>
 * <code>PersistentServant</code> 인터페이스를 지원하지 않는 객체의 참조가 외부에 전달되는 경우는
 * 해당 객체는 자동적으로 {@literal PlanetServer}에 등록된다.
 * 그러나 자동 등록은 객체 다음과 같이 외부로 공개할 인터페이스를 선정할 수 있는 경우, PlanetServer는
 * 동적으로 servant 객체를 생성할 수 있는 경우로 제한된다.
 * <ul>
 * 	<li> 객체가 'Servant' 인터페이스를 구현한 경우, 인터페이스의 {@link Servant#getRemoteInterfaces()}
 * 		메소드 호출 결과 얻은 인터페이스들을 외부에 공개할 인터페이스로 지정.
 * 	<li> (PlanetServer 설정에 따라) 객체가 하나 이상의 '@PlanetLocal'로 지정되지 않은
 * 		인터페이스를 구현한 경우. 이때는 '@PlanetLocal'로 설정되지 않은 인터페이스를 외부에
 * 		공개할 인터페이스로 지정.
 * </ul>
 * 만일 위 조건을 만족하지 않은 객체의 참조가 외부로 전달되는 경우는 {@link PlanetRuntimeException} 예외가
 * 발생된다.
 *
 *
 * <p>{@literal PlanetServer}에 등록된 모든 Servant 객체들은
 * PlanetServer 내의
 * 유일한 식별자를 가지며, 이를 Servant 경로(Servant Path)라 부른다.
 * Servant 경로는 <code>null</code>이 아닌 스트링의 배열로 정의된다. 경로가 길이 0의 배열의
 * 경우는 해당 PlanetServer의 시스템 정의 Servant인 Root({@link #ROOT_PATH}) 이며 이는 등록 삭제되지 않는다.
 * 일부 Servant의 경우는 다수 개의 하위 Servant을 가질 수 있으며 이를 {@link Directory}라
 * 부른다.
 *
 * <p>{@literal PlanetServer}에서 원격 객체에 대한 참조는 {@link Remote} 인터페이스를 통해
 * 정의되며 다음과 같은 구성요소를 갖는다.
 * <ul>
 * 	<li> remoteHost: 원격 객체가 등록된 PlanetServer의 호스트 IP.
 * 	<li> remotePort: 원격 객체가 등록된 PlanetServer의 포트 번호.
 * 	<li> servantPath: 원격 객체가 등록된 PlanetServer 내의 Servant 경로.
 * </ul>
 * {@literal PlanetServer}는 원격 참조로부터 해당 원격 객체의 proxy 객체를 생성할 수 있는 메소드
 * {@link #createProxy(Remote)}, {@link #createProxy(String, String, Class...)},
 * {@link #createProxy(String, String, Class)}를 제공한다.
 *
 * <p>{@literal PlanetServer}를 통해 원격 PlanetServer와 연결이 맺어질 때와
 * 끊어지는 경우 이 사실을 클라이언트에게 알릴 수 있다.
 * 이를 위해 클라이언트는 {@link PlanetServerListener} 클래스의 리스너 객체를
 * {@link #addPlanetServerListener(PlanetServerListener)} 메소드를 통해 등록하면, PlanetServer는
 * 외부와의 연결이 생성되거나 끊어질 때 리스너 객체의 {@link PlanetServerListener#onSessionOpened(PlanetSession)}
 * 메소드와 {@link PlanetServerListener#onSessionClosed(PlanetSession)} 메소드를 호출된다.
 * 이때 전달되는 인자는 연결된 (혹은 끊어진) 원격 PlanetServer의 식별자이다.
 * Planet에서는 동일 외부 PlanetServer와 다수의 연결을 가질 수 있다. 이 경우 리스너 객체의 호출은
 * 매 연결이 생성 될 때와 끊어 질때 마다 호출되는 것이 아니라, 대상 PlanetServer와의 첫 연결이
 * 생성될 때와 마지막 연결이 끊어질 때 호출된다.
 *
 * @author Kang-Woo Lee (Kang-Woo Lee)
 */
@PlanetLocal
public interface PlanetServer {
	public static final ThreadLocal<PlanetContext> s_context = new ThreadLocal<PlanetContext>() {
        protected synchronized PlanetContext initialValue() {
            return new PlanetContext();
        }
	};
	public static final String ROOT_PATH = "/";

	/**
	 * 현 PlanetServer를 시작시킨다.
	 * <p>
	 * PlanetServer가 시작되면 외부 요청을 받은 서버 소켓이 개방되며 등록된 servant 객체에 대한
	 * 외부로부터의 원격 호출이 가능하게 된다.
	 *
	 * @throws	IOException	외부 요청을 받은 서버 소켓 개방 및 초기화 중 오류가 발생된 경우.
	 */
	public void start() throws IOException;

	/**
	 * 현 PlanetServer를 종료시킨다.
	 * <p>
	 * PlanetServer가 종료되면 등록된 servant들에 대한 더 이상의 외부로부터의 원격 호출이 불가능하게 된다.
	 * 종료 과정 중에 발생되는 모든 checked 예외와 일부 unchecked 예외는 모두 무시된다.
	 */
	public void stop();

	/**
	 * 현 PlanetServer의 수행 상태를 반환한다.
	 *
	 * @return	수행 중인 경우는 <code>true</code>, 그렇지 않은 경우는 <code>false</code>.
	 */
	public boolean isRunning();

//	public TransportManager getTransportManager();

	public String getPlanetServerHost();
	public void setPlanetServerHost(String host);

	public int getPlanetServerPort();
	public void setPlanetServerPort(int port);
	
	public void setDelayedServantQueue(int qLength);

	/**
	 * PlanetServer에 부여된 PLANET HOST NAME(PHN)을 반환한다.
	 *
	 * @return	PlanetServer의 PHN 값.
	 */
	public String getId();

	/**
	 * 원격 객체의 proxy를 생성한다.
	 *
	 * @param rmt	proxy를 만들 대상 원격 객체의 참조 객체.
	 */
	public Object createProxy(String planetUri);

	/**
	 * 원격 객체의 proxy를 생성한다.
	 *
	 * @param rmt	proxy를 만들 대상 원격 객체의 참조 객체.
	 */
	public Object createProxy(Remote rmt);

	/**
	 * 원격 객체 proxy를 생성한다.
	 * <p>
	 * 생성된 proxy 객체는 인자로 전달되는 types 배열에 포함된
	 * 모든 인터페이스들과 {@link Remote}와 {@link PlanetProxy} 인터페이스를 모두 구현한다.
	 *
	 * @param	remotePlanetId		대상 원격 PlanetServer의 식별자.
	 * @param	path	대상 원격 객체의 servant path.
	 * @param	types	생성될 proxy 객체의 타입들.
	 * @return	원격 planet 객체의 proxy 객체.
	 * @throws	InvalidArgumentException	<code>remotePlanetId</code> 또는
	 * 									<code>types</code>가 null이거나, 크기가 0인 경우.
	 */
	public Object createProxy(String remotePlanetId, String path, Class<?>... types);

	/**
	 * 원격 객체 proxy를 생성한다.
	 * <p>
	 * 생성된 proxy 객체는 인자로 전달되는 type 인터페이스 뿐만 아니라  {@link Remote}와
	 * {@link PlanetProxy} 인터페이스를 모두 구현한다.
	 *
	 * @param	remotePlanetId		대상 원격 PlanetServer의 식별자.
	 * @param	path	대상 원격 객체의 servant path.
	 * @param	type	생성될 proxy 객체의 타입.
	 * @return	원격 planet 객체의 proxy 객체.
	 * @throws	InvalidArgumentException	<code>remotePlanetId</code> 또는
	 * 								<code>type</code>가 null인 경우.
	 */
	public <T> T createProxy(String remotePlanetId, String path, Class<T> type);

	/**
	 * 지역 객체를 wrapping한 proxy 객체를 생성한다.
	 * <p>
	 * 생성된 proxy 객체는 인자로 전달되는 types 배열에 포함된
	 * 모든 인터페이스들과 {@link Remote}와 {@link PlanetProxy} 인터페이스를 모두 구현한다.
	 *
	 * @param obj	wrapping할 대상 지역 객체.
	 * @param path	대상 원격 객체의 servant path.
	 * @param types	생성될 proxy 객체의 타입들.
	 * @return		원격 planet 객체의 proxy 객체.
	 * @throws InvalidArgumentException	<code>types</code>에 포함된 인터페이스가 없거나 유효하지 않은
	 * 								경우, <code>obj</code>, <code>path</code> 또는
	 * 								<code>types</code>가 null인 경우.
	 */
	public Object createLocalProxy(Object obj, String path, Class<?>... types);

	/**
	 * 주어진 호스트 IP 주소(<code>host</code>)와 포트 번호(<code>port</code>)에
	 * 해당하는 원격 PlanetServer와의 {@link PlanetSession}을 반환한다.
	 * <p>
	 * 해당하는 객체가 없는 경우는 <code>create</code> 값이 <code>true</code>인 경우는
	 * 주어진 host와 port로 연결을 시도하여 새로운 {@literal PlanetSession} 객체를 생성하거나,
	 * <code>false</code>인 경우는 <code>null</code>을 반환한다.
	 *
	 * @param remoteKey		대상 원격 PlanetServer의 식별자.
	 * @param create		대상 {@literal PlanetSession} 객체가 없는 경우,
	 * 						{@literal PlanetSession} 객체의 생성 시도 여부.
	 * @return		검색된 {@literal PlanetSession} 객체.
	 * 				<code>create</code>가 <code>false</code>이고,
	 * 				{@literal PlanetSession} 객체 검색에 실패한 경우는 <code>null</code>.
	 * @throws IOException		네트워크 문제로 {@literal PlanetSession} 객체 생성에 실패한 경우.
	 * @throws InvalidArgumentException	<code>remoteHost</code>가 <code>null</code>인 경우.
	 */
	public PlanetSession getPlanetSession(String remoteKey, boolean create)
		throws IOException, InterruptedException;

	/**
	 * 주어진 <code>remote</code> 객체가 위치한 원격 PlanetServer와의 {@link PlanetSession}을 반환한다.
	 * <p>
	 * 해당하는 객체가 없는 경우는 <code>create</code> 값이 <code>true</code>인 경우는
	 * 주어진 <code>remote</code>가 위치한 host와 port로 연결을 시도하여
	 * 새로운 {@literal PlanetSession} 객체를 생성하거나,
	 * <code>false</code>인 경우는 <code>null</code>을 반환한다.
	 *
	 * @param remote		대상 원격 참조.
	 * @param create		대상 {@literal PlanetSession} 객체가 없는 경우,
	 * 						{@literal PlanetSession} 객체의 생성 시도 여부.
	 * @return		검색된 {@literal PlanetSession} 객체.
	 * 				<code>create</code>가 <code>false</code>이고,
	 * 				{@literal PlanetSession} 객체 검색에 실패한 경우는 <code>null</code>.
	 * @throws IOException		네트워크 문제로 {@literal PlanetSession} 객체 생성에 실패한 경우.
	 * @throws InvalidArgumentException	<code>remoteHost</code>가 <code>null</code>인 경우.
	 */
	public PlanetSession getPlanetSession(Remote remote, boolean create)
		throws IOException, InterruptedException;

	/**
	 * 주어진 호스트 IP 주소(<code>host</code>)와 포트 번호(<code>port</code>)에
	 * 해당하는 원격 PlanetServer와의 {@link PlanetSession} 존재 여부를 반환한다.
	 * <p>
	 * 본 메소드는 현재 대상 원격 PlanetServer와 연결 여부만을 검사하며, 해당 PlanetServer와의
	 * 연결은 시도하지 않는다.
	 *
	 * @param remoteKey		대상 원격 PlanetServer의 식별자.
	 * @return	연결 여부. 연결된 경우는 true 그렇지 않은 경우는 false.
	 * @throws 	InvalidArgumentException	<code>remoteKey</code> 객체가 null인 경우.
	 */
	public boolean existsPlanetSession(String remoteKey);
	
	/**
	 * 최상위 Directory를 반환한다.
	 * <p>
	 * 최상위 Directory는 PlanetServer에 의해 자동적으로 생성된다.
	 *
	 * @return	최상위 Directory 객체.
	 */
	public Directory getRootDirectory();

	/**
	 * PlanetServer에 주어진 경로에 해당하는 등록된 {@link Servant}를 반환한다.
	 * <p>
	 * <code>path</code>가 <code>null</code>이거나 길이 0 배열인 경우는 루트 Directory를 반환한다.
	 *
	 * @param path		대상 Servant 객체의 path
	 * @return 	검색된 servant 객체.
	 * @throws ServantNotFoundException	path에 해당하는 servant 객체가 없는 경우.
	 * @throws IllegalArgumentException	유효하지 않은 path인 경우.
	 */
	public Servant getServant(String path) throws ServantNotFoundException,
													IllegalArgumentException;

	/**
	 * PlanetServer에 주어진 경로의 {@link Servant}가 등록되어 있는지 여부를 반환한다.
	 *
	 * @param	path	검색할 servant 객체 경로.
	 * @return			등록 여부. 주어진 경로의 servant 객체가 존재하면
	 * 					<code>true</code> 반환되고, 그렇지 않으면 <code>false</code>가 반환된다.
	 * 					인자 <code>path</code>가 <code>null</code>인 경우, 또는 배열의 길이가
	 * 					0인 경우는 루트 Directory를 의미하여 <code>true</code>가 반환된다.
	 * @throws IllegalArgumentException	유효하지 않은 path인 경우.
	 */
	public boolean existsServant(String path) throws IllegalArgumentException;

	/**
	 * 주어진 servant를 현 PlanetServer에 등록한다. 이때, 등록될 path는
	 * {@link PersistentServant#getServantPath()}를 호출하여 얻는다.
	 * <p>
	 * 최상위 Servant 등록은 허용되지 않고, <code>ServantExistsException</code> 예외가 발생된다.
	 *
	 * @param	servant 					추가할 Servant 객체.
	 * @throws	ServantNotFoundException	servant가 등록될 parent servant가 존재하지 않는 경우.
	 * @throws	ServantExistsException		동일 path를 갖는 Servant가 이미 존재하는 경우 또는
	 * 										Servant 객체의 path가 root 위치인 경우.
	 * @throws	InvalidArgumentException	servant 객체가 null인 경우.
	 */
	public void addServant(PersistentServant servant)
		throws ServantNotFoundException, ServantExistsException;

	/**
	 * 지정된 path의 {@link Servant}를 PlanetServer에서 제거시킨다. 만일 지정된 path에 해당하는
	 * <code>Servant</code>가 존재하지 않는 경우 호출은 무시된다.
	 * <p>
	 * 최상위 path (즉, "/")에 해당하는 Servant의 제거는 허용되지 않고 호출은 무시된다.
	 *
	 * @param	path	제거 대상 Servant 객체의 path.
	 * @throws 	InvalidArgumentException	유효하지 않은 path인 경우 또는 path가 null인 경우.
	 */
	public void removeServant(String path);

	/**
	 * 본 {@literal PlanetServer}에서 사용하는 heartbeat 전송 interval을
	 * mill-second 단위로 반환한다. 반환 값이 음수인 경우는 heartbeat을 사용하지 않음을 의미한다.
	 * <p>
	 * {@literal PlanetServer}는 heartbeat 전송 interval 동안 메세지 송수신이
	 * 없는 모든 연결에 대해 heartbeat 메시지를 강제적으로 발송시켜 해당 연결과의 단절 여부를 판정한다.
	 *
	 * @return	heartbeat 전송 interval (단위: milli-second).
	 */
	public long getHeartbeatInterval();

	/**
	 * 본 {@literal PlanetServer}에서 사용할 heartbeat 전송 interval을
	 * mill-second 단위로 설정한다. heartbeat을 사용하지 않으려면 음수 값의
	 * interval을 설정하면 된다.
	 * <p>
	 * {@literal PlanetServer}는 heartbeat 전송 interval 동안 메세지 송수신이
	 * 없는 모든 연결에 대해 heartbeat 메시지를 강제적으로 발송시켜 해당 연결과의 단절 여부를 판정한다.
	 *
	 * @param interval	heartbeat 전송 interval (단위: milli-second).
	 */
    public void setHeartbeatInterval(long interval);

    /**
     * 현 PlanetServer에서 사용하는 연결 제한 시간을 초 단위로 반환한다.
     * <p>
     * 연결 제한 시간은 소켓을 개방하는 경우 상대방의 응답이 올 때까지 최대 대기 시간으로
     * 제한 시간이 초과하는 경우 <code>IOException</code>이 발생된다.
     *
     * @return	연결 제한 시간 (단위: second)
     */
    public int getConnectTimeout();

    /**
     * 현 PlanetServer에 지정된 연결 제한 시간을 설정한다. 설정 시간이 0인 경우는
     * 무제한을 의미한다. {@literal PlanetServer}을 처음 시작하는 경우는
     * 시스템 지정 연결 제한 시간을 사용한다.
     * <p>
     * 연결 제한 시간은 소켓을 개방하는 경우 상대방의 응답이 올 때까지 최대 대기 시간으로
     * 제한 시간이 초과하는 경우 <code>IOException</code>이 발생된다.
     *
     * @param timeout	설정할 연결 제한 시간
     * @throws IllegalArgumentException	설정 시간 <code>timeout</code>이 음수인 경우.
     */
    public void setConnectTimeout(int timeout) throws IllegalArgumentException;

    /**
     * PlanetServer에서 사용 중인 원격 메소드 호출 제한시간을 반환한다.
     * 반환되는 값의 단위 millisecond 이고, 음수인 경우는 제한시간이 설정되지 않음을 의미한다.
     * <p>
     * 원격 메소드 호출 제한시간은 원격 객체의 메소드를 호출하고 그 반환 값을 기다리는
     * 제한 시간으로, 제한 시간을 초과한 경우는 해당 메소드 대기는 즉시 취소되고
     * {@link CallTimeoutException} 예외가 발생된다.
     *
     * @return	원격 메소드 호출 timeout (millisecond), timeout이 설정되지 않은 경우는 음수.
     */
    public long getDefaultCallTimeout();

    /**
     * PlanetServer에서 원격 메소드 호출 제한시간을 설정한다.
     * 설정할 값의 단위 millisecond 이고, 제한 시간을 사용하지 않으려면 음수를 설정한다.
     * <p>
     * 원격 메소드 호출 제한시간은 원격 객체의 메소드를 호출하고 그 반환 값을 기다리는
     * 제한 시간으로, 제한 시간을 초과한 경우는 해당 메소드 대기는 즉시 취소되고
     * {@link CallTimeoutException} 예외가 발생된다.
     *
     * @param timeout	원격 메소드 호출 timeout (millisecond), timeout이 설정되지 않은 경우는 음수.
     */
    public void setDefaultCallTimeout(long timeout);
    
    public ConnectionInfo[] getConnectionInfos();
    
	public Connection getConnection(String planetId, boolean create) throws IOException, InterruptedException;

	/**
	 * 현 PlanetServer 에서 사용하는 쓰레드 풀 객체를 반환한다.
	 *
	 * @return	쓰레드 풀 객체.
	 */
	public ExecutorService getPlanetExecutor();

	/**
	 * PlanetServer 에서 사용할 쓰레드 풀 객체를 설정한다.
	 *
	 * @param executor	설정할 쓰레드 풀 객체.
	 * @throws InvalidArgumentException	<code>executor</code> 객체가 <code>null</code>인 경우.
	 */
	public void setPlanetExecutor(ExecutorService executor);

	public void addClassLoader(String id, ClassLoader loader);
	public void removeClassLoader(String id);

	public Class<?> loadClass(String className) throws UndeclaredTypeException;

	public boolean addPlanetServerListener(PlanetServerListener listener);
	public boolean removePlanetServerListener(PlanetServerListener listener);
}
