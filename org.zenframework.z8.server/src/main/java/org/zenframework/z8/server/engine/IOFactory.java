package org.zenframework.z8.server.engine;

public interface IOFactory<T> {
	Class<T> getSupportedClass();
}
