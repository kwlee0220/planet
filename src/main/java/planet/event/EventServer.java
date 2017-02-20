package planet.event;

import planet.Event;
import planet.InvalidArgumentException;
import planet.idl.Oneway;



/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public interface EventServer {
	/**
	 * 주어진 이벤트를 발송한다.
	 * 
	 * @param event	발송할 이벤트 객체.
	 * @throws InvalidArgumentException	{@literal event}가 <code>null</code>인 경우.
	 */
	@Oneway public void publishEvent(String chId, Event event);
	
	/**
	 * 주어진 이벤트 구독자 객체 ({@link EventSubscriber})를 등록시킨다.
	 * <p>
	 * 발송될 이벤트는 등록된 모든 이벤트 구독자들에게 전달된다.
	 * 등록 당시 이미 동일 이벤트 구독자가 존재하는 경우는 등록이 거부되고 <code>false</code>를 반환한다.
	 * 동일 이벤트 구독자 여부는 {@link EventSubscriber#equals(Object)} 메소드를 사용한다.
	 * <br>
	 * 인자로 전달되는 state는 구독자 등록시 첨부되는 정보로 해당 구독자에게 이벤트 발송시
	 * 이 정보를 동반하여 전송한다.
	 * @param subscriber	등록할 이벤트 구독자 객체.
	 * 
	 * @return	등록 여부. 성공적으로 등록된 경우는 <code>true</code> 그렇지 않은 경우는 <code>false</code>
	 * @throws InvalidArgumentException	<code>subscriber</code> 객체가 <code>null</code>인 경우.	
	 */
	public boolean subscribe(String chId, EventSubscriber subscriber);
	
	/**
	 * 주어진 이벤트 구독자 객체 ({@link EventSubscriber})를 등록시킨다.
	 * <p>
	 * 발송될 이벤트는 등록된 모든 이벤트 구독자들에게 전달된다.
	 * 등록 당시 이미 동일 이벤트 구독자가 존재하는 경우는 등록이 거부되고 <code>false</code>를 반환한다.
	 * 동일 이벤트 구독자 여부는 {@link EventSubscriber#equals(Object)} 메소드를 사용한다.
	 * <br>
	 * 인자로 전달되는 state는 구독자 등록시 첨부되는 정보로 해당 구독자에게 이벤트 발송시
	 * 이 정보를 동반하여 전송한다.
	 * @param subscriber	등록할 이벤트 구독자 객체.
	 * 
	 * @return	등록 여부. 성공적으로 등록된 경우는 <code>true</code> 그렇지 않은 경우는 <code>false</code>
	 * @throws InvalidArgumentException	<code>subscriber</code> 객체가 <code>null</code>인 경우.	
	 */
	public boolean subscribe(String chId, String filter, EventSubscriber subscriber);
	
	/**
	 * 등록된 이벤트 구독자 객체를 해제시킨다.
	 * 
	 * 만일 해당 이벤트 구독자 객체가 없는 경우는 요청은 무시되고, <code>false</code>를 반환한다.
	 * 이벤트 구독자 존재 여부는 {@link EventSubscriber#equals(Object)} 메소드를 사용한다.
	 * 
	 * @param	subscriber	등록 해제할 구독자 객체.
	 * @return	등록 해제 여부. 성공적으로 해제된 경우는 <code>true</code>,
	 * 			그렇지 않은 경우는 <code>false</code>
	 * @throws InvalidArgumentException	<code>subscriber</code> 객체가 <code>null</code>인 경우.	
	 */
	public boolean unsubscribe(String chId, EventSubscriber subscriber);
}
