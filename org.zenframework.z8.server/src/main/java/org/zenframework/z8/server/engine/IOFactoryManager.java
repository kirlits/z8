package org.zenframework.z8.server.engine;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;

public final class IOFactoryManager<F extends IOFactory<?>> {
	private static final Object NullMarker = new Object();
	
	@SuppressWarnings("rawtypes")
	private static final Map<Class<?>, IOFactoryManager> managers = new HashMap<>();

	@SuppressWarnings("unchecked")
	public static <I extends IOFactory<?>> IOFactoryManager<I> getInstance(Class<I> factoryInterface) {
		IOFactoryManager<I> result = (IOFactoryManager<I>) managers.get(factoryInterface);

		if (result == null) {
			synchronized (managers) {
				result = (IOFactoryManager<I>) managers.get(factoryInterface);

				if (result == null) {
					result = new IOFactoryManager<>(factoryInterface);
					managers.put(factoryInterface, result);
				}
			}
		}
		return result;
	}

	// --- INTERNAL DISPATCHER INFRASTRUCTURE ---

	private final Map<Class<?>, F> spiFactories;
	private final Map<Class<?>, Object> factoryCache = new ConcurrentHashMap<>();

	private IOFactoryManager(Class<F> factoryInterface) {
		Map<Class<?>, F> map = new HashMap<>();
		ServiceLoader<F> loader = ServiceLoader.load(factoryInterface);

		for (F factory : loader) {
			Class<?> supportedClass = factory.getSupportedClass();
			if (supportedClass == null) {
				continue;
			}

			if (map.containsKey(supportedClass)) {
				F existingFactory = map.get(supportedClass);
				if (factory.getPriority() > existingFactory.getPriority()) {
					map.put(supportedClass, factory);
				} else if (factory.getPriority() == existingFactory.getPriority()) {
					throw new IllegalStateException("IOFactory collision detected for class " 
							+ supportedClass.getName() + " with identical priority (" + factory.getPriority() + "). "
							+ "Conflict between: " + existingFactory.getClass().getName() 
							+ " and " + factory.getClass().getName());
				}
			} else {
				map.put(supportedClass, factory);
			}
		}
		this.spiFactories = Collections.unmodifiableMap(map);
	}

	private boolean isNull(Object f) {
		return f == null || f == NullMarker;
	}

	@SuppressWarnings("unchecked")
	private F getFactory(Object obj) {
		return isNull(obj) ? null : (F)obj;
	}

	public F getFactory(Class<?> currentClass) {
		if (factoryCache.containsKey(currentClass)) {
			return getFactory(factoryCache.get(currentClass));
		}

		F factory = null;
		for (Class<?> clazz = currentClass; clazz != null && clazz != Object.class && isNull(factory); clazz = clazz.getSuperclass()) {
			if (factoryCache.containsKey(clazz)) {
				factory = getFactory(factoryCache.get(clazz));
				break;
			}
			factory = spiFactories.get(clazz);
		}

		factoryCache.put(currentClass, isNull(factory) ? NullMarker : factory);
		return factory;
	}
}
