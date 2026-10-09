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

	/**
	 * Explicitly declares whether this factory has the authority to bypass and override 
	 * the native platform serialization interfaces (JsonSerializable / RmiSerializable).
	 */
	default boolean overridesSerializable() {
		return false;
	}
}
