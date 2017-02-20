package planet.event;

import planet.Event;
import planet.InvalidArgumentException;
import planet.idl.Oneway;



/**
 * <code>EventChannel</code>는 이벤트 채널 인터페이스를 정의한다.
 * 
 * <p><code>EventChannel</code>은 Publisher/Subscriber 기반 이벤트 전달 방식의
 * 인터페이스를 정의한다.
 * <code>UrcEventChannel</code>은 다수의 이벤트 구독자 ({@link EventSubscriber})를
 * 등록 받을 수 있으며 채널로 발송되는 모든 이벤트는 등록된 모든 구독자들에게 전달된다.
 * <br>이벤트 전달은 구독자 객체의 {@link EventSubscriber#receiveEvent(Event)}를
 * 호출하는 방식을 수행된다.
 * <br>구독자는 {@link #subscribe(EventSubscriber)}와 {@link #unsubscribe(String)}를
 * 통해 등록을 설정할 수 있다.
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public interface EventChannel extends EventPublisher {
	/**
	 * 주어진 이벤트를 본 채널을 통해 발송한다.
	 * <p>
	 * 발송된 이벤트는 채널을 구독하는 모든 이벤트 구독자들에게 전달된다.
	 * 
	 * @param event	발송할 이벤트 객체.
	 * @throws InvalidArgumentException	{@literal event}가 <code>null</code>인 경우.
	 */
	@Oneway public void publishEvent(Event event);
}
