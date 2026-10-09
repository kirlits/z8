package org.zenframework.z8.server.engine;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

public final class IOFactoryManager<F extends IOFactory<?>> {
	@SuppressWarnings("rawtypes")
	private static final Map<Class<?>, IOFactoryManager> managers = new HashMap<>();

	@SuppressWarnings("unchecked")
	public static <I extends IOFactory<?>> IOFactoryManager<I> getInstance(Class<I> factoryInterface) {
		IOFactoryManager<I> result = (IOFactoryManager<I>) managers.get(factoryInterface);

		// First check (No synchronization overhead for 99.9% of requests)
		if (result == null) {
			// Synchronize strictly on the class registry map during initialization
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
	private final Map<Class<?>, F> factoryCache = Collections.synchronizedMap(new HashMap<>());

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

	public F getFactory(Class<?> currentClass) {
		if (factoryCache.containsKey(currentClass)) {
			return factoryCache.get(currentClass);
		}

		F factory = null;
		for (Class<?> clazz = currentClass; clazz != null && clazz != Object.class && factory == null; clazz = clazz.getSuperclass()) {
			if (factoryCache.containsKey(clazz)) {
				factory = factoryCache.get(clazz);
				break;
			}
			factory = spiFactories.get(clazz);
		}

		factoryCache.put(currentClass, factory);
		return factory;
	}
}
