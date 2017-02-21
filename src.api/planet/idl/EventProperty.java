package planet.idl;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * {@literal EventProperty}는 Event에서 정의된 이벤트 속성 값 접근 메소드에
 * 해당하는 이벤트 속성 이름과의 관계를 기술하는 annotation이다.
 * <p>
 * EventProperty는 이벤트 인터페이스에서 정의된 메소드에 첨부되는 annotation으로
 * 해당 메소드가 사용하는 이벤트 속성 이름을 기술한다. 
 * 
 * @author Kang-Woo Lee (ETRI)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface EventProperty {
	/**
	 * 대상 이벤트 속성 이름.
	 */
	String name();
}
