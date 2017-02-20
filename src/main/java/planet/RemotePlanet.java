package planet;

import java.util.Collection;

import planet.idl.Oneway;



/**
 * 
 * @author Kang-Woo Lee
 */
public interface RemotePlanet {
	/**
	 * 원격 PlanetServer가 사용하는 쓰레드 풀의 크기를 반환한다.
	 * 
	 * @return 쓰레드 풀에 포함된 쓰레드 갯수.
	 */
	public int getThreadCount();
	
	/**
	 * 원격 PlanetServer에서 쓰레드 풀의 쓰레드 중에서 현재 수행 중인 쓰레드의 갯수.
	 * 
	 * @return 수행 중인 쓰레드 갯수.
	 */
	public int getActiveThreadCount();

	public void setConnectionDescription(String description);
	
	public @Oneway void removeServant(String path);
	
    /**
     * 원격 PlanetServer에 현재 등록된 서번트들의 경로값을 모두 검색한다.
     * 
     * @return 검색된 연결 키값  리스트.
     * @throws InvalidArgumentException	<code>id</code>가 <code>null</code>인 경우.
     */		
	public Collection<String> getServantsIds();
	
    /**
     * 원격 PlanetServer에 현재 접속된  모든 연결에 대한 키값을 모두 검색한다
     * 
     * @return 검색된 연결 키값  리스트.
     * @throws InvalidArgumentException	<code>id</code>가 <code>null</code>인 경우.
     */	
	public Collection<String> getSessionIds();
	
	 /**
     * 원격 PlanetServer에 접속된 연결중에서 주어진 키값의 연결을 강제로 종료한다.
     * 
     * @param id 강제 종료할 연결에 대한 키값.
     * @throws InvalidArgumentException	<code>id</code>가 <code>null</code>인 경우.
     */	
	public void closeSession(String id);
		
    /**
     * 원격 PlanetServer에 접속된 연결중에서 주어진 키값의 연결에 등록된 서번트 경로명을 모두 검색한다.
     * 
     * @param id 검색에 사용될 연결에 대한 키값.
     * @return 검색된 서번트 경로  리스트.
     * @throws InvalidArgumentException	<code>id</code>가 <code>null</code>인 경우.
     */
	public Collection<String> getServantIdsOfSession(String id);
}
