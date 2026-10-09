package org.zenframework.z8.server.engine;

public interface IOFactory<T> {
	Class<T> getSupportedClass();

	/**
	 * Defines the factory execution priority. Higher priority factories override
	 * lower ones. Default z8 implementations use priority 0.
	 */
	default int getPriority() {
		return 0;
	}
}
