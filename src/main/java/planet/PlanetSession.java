package planet;


import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.concurrent.ExecutionException;

import planet.idl.PlanetLocal;
import planet.transport.Connection;


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
@PlanetLocal
public interface PlanetSession {
	/**
	 * 현 세션의 키 값을 반환한다.
	 * <p>
	 * 세션 키는 원격 PlanetServer의 호스트 IP 주소와 포트 번호의 조합으로 정의되고
	 * 한 PlanetServer내에는 모든 세션은 각각 유일한 세션 키를 갖는다.
	 * 
	 * @return 세션 키.
	 */
	public String getKey();
	
	/**
	 * 세션을 끊는다.
	 * <p>
	 * 세션에 속한 모든 채널들은 모두 닫히게 된다.
	 */
	public void close();
	
	/**
	 * 세션의 폐쇄 여부를 반환한다.
	 * 
	 * @return	세션 폐쇄 여부
	 */
	public boolean isClosed();
	
	public PlanetServer getPlanetServer();
	
	/**
	 * 현 세션이 사용하는 연결 객체를 반환한다.
	 */
	public Connection getConnection();
	
	public boolean isActive();
	
	public Collection<String> getServantIds();
	
	public boolean removeServant(Object servant);

	/**
	 * 현 세션과 연결된 원격의 객체의 메소드를 호출한다.
	 * 
	 * @param path		원격 객체의 경로.
	 * @param method	호출할 메소드
	 * @param args		호출시 사용할 인자들의 배열
	 * @return	메소드 호출 반환 값.
	 * @throws ExecutionException	호출된 메소드에서 선언된 예외가 발생된 경우. 실제 발생된
	 * 					예외는 {@link ExecutionException#getCause()}를 통해 얻는다.
	 * @throws RemoteSystemException 호출된 원격 호스트에서 시스템 오류가 발생된 경우.
	 * 					실제 발생된 시스템 오류는
	 * 					{@link RemoteSystemException#getCauseException(PlanetServer)}를
	 * 					통해 얻을 수 있다.
	 * @throws IOException		메소드 호출 과정 중 통신 문제가 발생된 경우.
	 * @throws InterruptedException	메소드 호출 과정 중 호출 쓰레드가 중단된 경우.
	 */
	public Object invoke(String path, Method method, Object[] args)
		throws ExecutionException, RemoteSystemException, IOException, InterruptedException;
}