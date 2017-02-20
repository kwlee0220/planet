package planet.event;

import planet.Event;
import planet.idl.Oneway;


/**
 * <code>EventSubscriber</code>는 CAMUS 이벤트 구독자 인터페이스를 정의한다.
 * 
 * <p>이벤트 구독자를 구현한 객체는 임의의 이벤트 채널 ({@link EventChannel})에 구독자로
 * {@link EventChannel#subscribe(EventSubscriber)}를 통해 등록될 수 있고,
 * 구독자는 채널로 발송되는 모든 이벤트를 받을 수 있다.
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public interface EventSubscriber {
	/**
	 * Subscribe된 {@link EventChannel}로 부터 {@link Event}를 전송 받는다.
	 * <p>인자 state는 해당 {@literal UrcEventChannel}에 등록시 사용된 state 값이 사용된다. 
	 * 
	 * @param event		전송된 이벤트 객체.
	 */
	@Oneway
	public void receiveEvent(Event event);
}
