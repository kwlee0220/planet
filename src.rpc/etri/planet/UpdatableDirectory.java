package etri.planet;

import planet.Directory;
import planet.PersistentServant;
import planet.idl.PlanetLocal;


/**
 * <code>UpdatableDirectory</code>는 다수의 {@link PersistentServant}를 포함하는
 * <code>PersistentServant</code>의 인터페이스를 정의한다.
 * 
 * @author Kang-Woo Lee (ETRI)
 */
@PlanetLocal
public interface UpdatableDirectory extends Directory {
	/**
	 * 현 디렉토리에 지정된 새 <code>PersistentServant</code> 객체를 추가한다.
	 * <p>
	 * 디렉토리에 이미 동일 식별자의 <code>PersistentServant</code>가 존재하는 경우는
	 * 기존 <code>PersistentServant</code>가 제거되고, 기존 <code>PersistentServant</code>를 반환한다.
	 * 그렇지 않은 경우는 <code>null</code>이 반환된다.
	 * 
	 * @param id		추가할 <code>PersistentServant</code>의 식별자.
	 * @param servant	추가할 <code>PersistentServant</code> 객체.
	 * @return	동일 식별자의 <code>PersistentServant</code>가 존재하는 경우는 해당
	 * 			<code>PersistentServant</code>, 그렇지 않은 경우는 <code>null</code>이 반환된다.
	 * @throws	NullPointerException	<code>id</code> 또는 <code>servant</code>가
	 * 						<code>null</code>인 경우.
	 */
	public PersistentServant addServant(String id, PersistentServant servant)
		throws NullPointerException;

	/**
	 * 디렉토리에서 지정된 식별자의 <code>PersistentServant</code>를 제거하고 해당 객체를 반환한다.
	 * <p>만일 식별자에 해당하는 <code>PersistentServant</code>가 없는 경우는
	 * <code>null</code>을 반환한다.
	 * 
	 * @param id	제거할 <code>PersistentServant</code>의 식별자.
	 * @return	식별자에 해당하는 <code>PersistentServant</code>가 존재하는 경우는
	 * 			해당 <code>PersistentServant</code> 객체, 그렇지 않은 경우는 <code>null</code>이 반환된다.
	 * @throws	NullPointerException	<code>id</code>가 <code>null</code>인 경우.
	 */
	public PersistentServant removeServant(String id) throws NullPointerException;
}
