package planet;

import planet.idl.PlanetLocal;


/**
 * <code>Directory</code>는 다수의 {@link PersistentServant}를 포함하는
 * <code>PersistentServant</code>의 인터페이스를 정의한다.
 * 
 * @author Kang-Woo Lee (ETRI)
 */
@PlanetLocal
public interface Directory extends PersistentServant {
	/**
	 * 현 디렉토리에 등록된 <code>PersistentServant</code>들 중에서 지정된 식별자에 해당하는
	 * <code>PersistentServant</code> 객체를 반환한다.
	 * 
	 * @param id	검색할 <code>PersistentServant</code>의 식별자.
	 * @return	검색된 <code>PersistentServant</code> 객체. 식별자에 해당하는
	 * 			<code>PersistentServant</code>가 없는 경우는 <code>null</code>을 반환한다.
	 */
	public PersistentServant getServant(String id);
}
