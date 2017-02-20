package planet.event;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import planet.Event;
import planet.PlanetServer;
import planet.UndeclaredTypeException;
import planet.idl.EventProperty;

import org.apache.log4j.Logger;



/**
 *
 * @author Kang-Woo Lee
 */
public class EventBuilder {
	private static final Logger s_logger = Logger.getLogger("EVENT.BUILDER");

	private final List<Class<?>> m_types;
	private final List<String> m_typeIdList = new ArrayList<String>();
	private final Map<String,Object> m_props;

	public static <T> T buildEvent(Class<T> type) {
		return new EventBuilder(type).build(type);
	}

	public EventBuilder(Class<?>... types) {
		m_types = new ArrayList<Class<?>>();
		for ( int i =0; i < types.length; ++i ) {
			addEventType(types[i]);
		}
		addEventType(Event.class);

		m_props = new HashMap<String,Object>();
	}

	public EventBuilder(PlanetServer planet, Event event) {
		String[] typeIds = event.getEventTypeIds();

		m_types = new ArrayList<Class<?>>();
		for ( int i =0; i < typeIds.length; ++i ) {
			try {
				m_typeIdList.add(typeIds[i]);
				m_types.add(planet.loadClass(typeIds[i]));
			}
			catch ( UndeclaredTypeException e ) {
				if ( s_logger.isDebugEnabled() ) {
					s_logger.debug("fails to load class=" + typeIds[i] + ", cause=" + e);
				}
			}
		}
		addEventType(Event.class);

		m_props = new HashMap<String,Object>();
		for ( String name: event.getPropertyNameAll() ) {
			m_props.put(name, event.getProperty(name));
		}
	}

	public EventBuilder(PlanetServer planet, String[] typeIds) {
		m_types = new ArrayList<Class<?>>();
		for ( int i =0; i < typeIds.length; ++i ) {
			try {
				m_typeIdList.add(typeIds[i]);
				m_types.add(planet.loadClass(typeIds[i]));
			}
			catch ( UndeclaredTypeException e ) {
				if ( s_logger.isDebugEnabled() ) {
					s_logger.debug("fails to load class=" + typeIds[i] + ", cause=" + e);
				}
			}
		}
		addEventType(Event.class);

		m_props = new HashMap<String,Object>();
	}

	public Event build(ClassLoader loader) {
		EventImpl impl = createEventImpl();
		Class<?>[] types = new Class<?>[m_types.size()];
		m_types.toArray(types);

		return (Event)Proxy.newProxyInstance(loader, types, new Handler(impl));
	}

	public Event build() {
		EventImpl impl = createEventImpl();
		Class<?>[] types = new Class<?>[m_types.size()];
		m_types.toArray(types);

		return (Event)Proxy.newProxyInstance(getClass().getClassLoader(), types, new Handler(impl));
	}

	@SuppressWarnings("unchecked")
	public <T> T build(Class<T> type) {
		return (T)build(type.getClassLoader());
	}

	public EventBuilder addEventType(Class<?> type) {
		for ( Class<?> cls: m_types ) {
			if ( type.isAssignableFrom(cls) ) {
				return this;
			}
		}

		m_types.add(type);
		m_typeIdList.add(type.getName());

		return this;
	}

	public void setProperty(String name, Object value) {
		m_props.put(name, value);
	}
	
	public boolean isPropertyDefined(String name) {
		return m_props.containsKey(name);
	}

	private EventImpl createEventImpl() {
		String[] typeIds = m_typeIdList.toArray(new String[m_typeIdList.size()]);

		String[] propNames = new String[m_props.size()];
		Object[] propValues = new Object[m_props.size()];
		int i =0;
		for ( Map.Entry<String, Object> entry: m_props.entrySet() ) {
			propNames[i] = entry.getKey();
			propValues[i] = entry.getValue();

			++i;
		}

		return new EventImpl(typeIds, propNames, propValues);
	}

	static class Handler implements InvocationHandler {
		private final EventImpl m_impl;

		Handler(EventImpl impl) {
			m_impl = impl;
		}

		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			EventProperty anno = method.getAnnotation(EventProperty.class);
			if ( anno != null ) {
				return m_impl.getProperty(anno.name());
			}
			else {
				return method.invoke(m_impl, args);
			}
		}
	}
}
