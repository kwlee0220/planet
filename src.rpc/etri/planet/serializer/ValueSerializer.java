package etri.planet.serializer;


import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import planet.PlanetRuntimeException;
import planet.PlanetUtils;
import planet.SystemException;
import planet.idl.PlanetField;

import etri.planet.PlanetReader;
import etri.planet.PlanetWriter;
import etri.planet.RpcLoggers;


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public class ValueSerializer implements Serializer {
	private static final Class<?>[] EMPTY_PARAMS = new Class[0];
	private static final Object[] EMPTY_ARGS = new Object[0];

	private final Class<?> m_valueClass;
	private final ValueDesc m_rootDesc;
	private final Method m_readResolve;
	private final Constructor<?> m_ctor;
	
	public ValueSerializer(Class<?> valueCls) {
		m_valueClass = valueCls;
		m_rootDesc = loadValueDesc(valueCls);
		
		Method readResolve;
		try {
			readResolve = valueCls.getMethod("readResolve", EMPTY_PARAMS);
		}
		catch ( Exception ignored ) {
			readResolve = null;
		}
		m_readResolve = readResolve;
		
		try {
			m_ctor = valueCls.getConstructor((Class[])null);
			m_ctor.setAccessible(true);
		}
		catch ( Exception e ) {
			throw new SystemException("PlanetValue does not have default constructor: class="
											+ valueCls.getName());
		}
	}
	
	@Override
	public byte getTypeCode() {
		return TypeCode.VALUE;
	}
	
	public void serialize(Object obj, PlanetWriter out) throws IOException {
		if ( obj == null ) {
			throw new NullPointerException("obj should not be null");
		}
		else if ( obj.getClass() != m_valueClass ) {
			throw new IllegalArgumentException("Invalid object class: expected("
					+ m_valueClass.getName() + ")<->actual(" + obj.getClass().getName() + ")");
		}
		
		int idx = out.getIndexOfReference(obj);
		if ( idx >= 0 ) {
			out.writeByte(TypeCode.REFERENCE);
			out.writeShort((short)idx);
		}
		else {
			out.addReference(obj);
			serializeNonNullTypedObject(obj, out);
		}
	}

	public void serializeWithCode(Object obj, PlanetWriter out) throws IOException {
		if ( obj == null ) {
			out.writeByte(TypeCode.NULL);
			return;
		}
		else if ( obj.getClass() != m_valueClass ) {
			throw new IllegalArgumentException("Invalid object class: expected("
					+ m_valueClass.getName() + ")<->actual(" + obj.getClass().getName() + ")");
		}
		
		int idx = out.getIndexOfReference(obj);
		if ( idx >= 0 ) {
			out.writeByte(TypeCode.REFERENCE);
			out.writeShort((short)idx);
		}
		else {
			out.addReference(obj);
			out.writeByte(TypeCode.VALUE);
			
			serializeNonNullTypedObject(obj, out);
		}
	}
	
	public Object deserializeUsingClass(Class<?> expected, PlanetReader in) throws IOException {
		return deserializeUnreferred(expected, in);
	}

	public Object deserializeUsingCode(byte code, PlanetReader in) throws IOException {
		Class<?> vClass = in.readClassFromString();
		return deserializeUsingClass(vClass, in);
	}

	private void serializeNonNullTypedObject(Object obj, PlanetWriter out) throws IOException {
		out.writeString(m_valueClass.getName());
		out.writeInt(m_rootDesc.m_fields.length);
		
		for ( ValueDesc desc = m_rootDesc; desc != null; desc = desc.m_childValue ) {
			for ( int i =0; i < desc.m_fields.length; ++i ) {
				Field field = desc.m_fields[i];
				if ( field != null ) {
					try {
						Object value = field.get(obj);
						
						Serializer serializer;
						if ( field.getType().equals(Object.class) ) {
							serializer = SerializerGlobals.getSerializerFromClass(value.getClass());
						}
						else {
							serializer = SerializerGlobals.getSerializerFromClass(field.getType());
						}
// TODO: 위 부분을 급하게 수정한 것이라서 좀 더 살펴볼 필요가 있음
//						Serializer serializer = SerializerGlobals.getSerializerFromClass(field.getType());
						
						serializer.serializeWithCode(value, out);
		
						if ( RpcLoggers.CODEC.isDebugEnabled() ) {
							RpcLoggers.CODEC.debug("encoded: Field[" + field.getName() + "," + field.get(obj) + "]");
						}
					}
					catch ( Throwable ignored ) {
						RpcLoggers.CODEC.error("fails to encode fields=" + field,
												PlanetUtils.unwrapThrowable(ignored));
					}
				}
			}
		}
	}
	
	protected Object deserializeUnreferred(Class<?> expected, PlanetReader reader)
		throws IOException {
		try {
			reader.readInt();		// # of fields
			
			Object valued = m_ctor.newInstance((Object[])null);
			reader.addReference(valued);
			
			for ( ValueDesc desc = m_rootDesc; desc != null; desc = desc.m_childValue ) {
				for ( int i =0; i < desc.m_fields.length; ++i ) {
					Field field = desc.m_fields[i];
					
					if ( field != null ) {
						Object value = reader.readObject(field.getType());
						
						try {
							field.set(valued, value);
						}
						catch ( Throwable e ) {
							throw new SystemException("" + e);
						}
					}
				}
			}
			
			if ( m_readResolve != null ) {
				try {
					valued = m_readResolve.invoke(valued, EMPTY_ARGS);
				}
				catch ( InvocationTargetException e ) {
					throw new SystemException("" + e.getTargetException());
				}
				catch ( Exception e ) {
					throw new SystemException("" + e);
				}
			}
			
			return valued;
		}
		catch ( SystemException e ) {
			throw e;
		}
		catch ( Throwable e ) {
			throw new SystemException("fails to initiate object of class=" + m_valueClass,
																PlanetUtils.unwrapThrowable(e));
		}
	}
	
	static class ValueDesc {
		private ValueDesc m_childValue;
		private Field[] m_fields;
	}
	
	private ValueDesc loadValueDesc(Class<?> cls) {
		ValueDesc childDesc = null;
		for (; cls != Object.class; cls = cls.getSuperclass() ) {
			ValueDesc desc = new ValueDesc();
			
			Field[] fields = cls.getDeclaredFields();
			desc.m_fields = new Field[fields.length];
			
			for ( int i =0; i < fields.length; ++i ) {
				Field field = fields[i];
				
				PlanetField aField = field.getAnnotation(PlanetField.class);
				if ( aField != null ) {
					if ( aField.ordinal() >= desc.m_fields.length ) {
						throw new SystemException("Invalid value field ordinal="
												+ aField.ordinal() + ", name=" + field.getName());
					}

					int modifiers = field.getModifiers();
					if ( Modifier.isStatic(modifiers) ) {
						throw new SystemException("PlanetField cannot annotated on static filed=" + field);
					}

					field.setAccessible(true);
					desc.m_fields[aField.ordinal()] = field;
				}
			}
			
			int count = 0;
			for ( int i =0; i < desc.m_fields.length; ++i ) {
				if ( desc.m_fields[i] != null ) {
					++count;
				}
			}
			if ( count < desc.m_fields.length ) {
				Field[] resized = new Field[count];
				for ( int i =0; i < count; ++i ) {
					resized[i] = desc.m_fields[i];
					if ( resized[i] == null ) {
						throw new PlanetRuntimeException("Bad PlanetValue object");
					}
				}
				
				desc.m_fields = resized;
			}
			
			if ( childDesc != null ) {
				desc.m_childValue = childDesc;
			}
			
			childDesc = desc;
		}
		
		return childDesc;
	}
}
