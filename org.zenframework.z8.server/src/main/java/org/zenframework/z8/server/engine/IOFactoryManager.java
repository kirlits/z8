package org.zenframework.z8.server.engine;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;

import org.zenframework.z8.server.config.ServerConfig;
import org.zenframework.z8.server.ie.Message;
import org.zenframework.z8.server.ie.MessageJsonFactory;
import org.zenframework.z8.server.json.JsonFactory;

public final class IOFactoryManager<F extends IOFactory<?>> {
	private static final Object NullMarker = new Object();
	
	@SuppressWarnings("rawtypes")
	private static final Map<Class<?>, IOFactoryManager> managers = new ConcurrentHashMap<>();

	@SuppressWarnings("unchecked")
	public static <I extends IOFactory<?>> IOFactoryManager<I> getInstance(Class<I> factoryInterface) {
		return (IOFactoryManager<I>) managers.computeIfAbsent(factoryInterface, cls -> new IOFactoryManager<>((Class<IOFactory<?>>) cls));
	}

	// --- INTERNAL DISPATCHER INFRASTRUCTURE ---

	private final Map<Class<?>, F> spiFactories;
	private final Map<Class<?>, Object> factoryCache = new ConcurrentHashMap<>();

	private IOFactoryManager(Class<F> factoryInterface) {
		if (ServerConfig.shouldDynamicallyLoadIOFactories())
			this.spiFactories = Collections.unmodifiableMap(dynamicalLoad(factoryInterface));
		else
			this.spiFactories = Collections.unmodifiableMap(staticLoad(factoryInterface));
	}

	//@SuppressWarnings("unchecked")
	private Map<Class<?>, F> staticLoad(Class<F> factoryInterface) {
		Map<Class<?>, F> map = new HashMap<>();

		/*if (factoryInterface.equals(JsonFactory.class)) {
			map.put(Message.class, (F) new MessageJsonFactory());
		}*/

		return map;
	}

	private Map<Class<?>, F> dynamicalLoad(Class<F> factoryInterface) {
		ServiceLoader<F> loader = ServiceLoader.load(factoryInterface);
		Map<Class<?>, F> winners = new HashMap<>();
		Map<Class<?>, F> runners = new HashMap<>();

		for (F factory : loader) {
			Class<?> supportedClass = factory.getSupportedClass();
			if (supportedClass == null) {
				continue;
			}

			F winner = winners.get(supportedClass);
			F runner = runners.get(supportedClass);

			if (winner == null) {
				winners.put(supportedClass, factory);
			} else if (factory.getPriority() > winner.getPriority()) {
				runners.put(supportedClass, winner);
				winners.put(supportedClass, factory);
			} else if (runner == null || factory.getPriority() > runner.getPriority()) {
				runners.put(supportedClass, factory);
			}
		}
	
		Map<Class<?>, F> resolvedMap = new HashMap<>();
		for (Map.Entry<Class<?>, F> entry : winners.entrySet()) {
			Class<?> supportedClass = entry.getKey();
			F winner = entry.getValue();
			F runnerUp = runners.get(supportedClass);

			if (runnerUp != null && winner.getPriority() == runnerUp.getPriority()) {
				throw new IllegalStateException("IOFactory collision detected for class " 
						+ supportedClass.getName() + " with identical priority (" + winner.getPriority() + "). "
						+ "Conflict between: " + winner.getClass().getName() 
						+ " and " + runnerUp.getClass().getName());
			}

			resolvedMap.put(supportedClass, winner);
		}

		return resolvedMap;
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
