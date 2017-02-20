package planet.event;

import java.util.List;

import planet.Event;
import planet.idl.PlanetLocal;



/**
 * 
 * @author Kang-Woo Lee
 */
@PlanetLocal
public interface EventInterceptor {
	/**
	 * 주어진 Event 객체를 intercept한다.
	 * <p>Intercept 결과로 변형/발생된 이벤트들은 <code>survival</code>에 순서대로
	 * 삽입된다.
	 * 
	 * @param event		intercept할 대상 Event 객체.
	 * @param survivals	intercept 결과로 생성될 Event 객체를 삽입할 리스트.
	 */
	public void intercept(Event event, List<Event> survivals);
}
