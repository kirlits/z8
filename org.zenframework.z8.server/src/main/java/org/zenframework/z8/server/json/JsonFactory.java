package org.zenframework.z8.server.json;

import java.io.IOException;

import org.zenframework.z8.server.json.parser.JsonObject;

public interface JsonFactory<T> {
	public Class<T> getSupportedClass();

	public JsonObject toJson(T instance) throws IOException;
	public T fromJson(JsonObject json) throws IOException, ClassNotFoundException;
}
