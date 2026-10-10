package org.zenframework.z8.server.json;

import java.io.IOException;

import org.zenframework.z8.server.engine.IOFactory;
import org.zenframework.z8.server.json.parser.JsonObject;

public interface JsonFactory<T> extends IOFactory<T> {
	public JsonObject toJson(T instance) throws IOException;
	public T fromJson(JsonObject json, Class<? extends T> instanceClass) throws IOException, ClassNotFoundException;
}
