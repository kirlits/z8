package org.zenframework.z8.server.json;

import java.io.IOException;

import org.zenframework.z8.server.json.parser.JsonObject;

public interface JsonSerializable {
	JsonObject toJson() throws IOException;
	void fromJson(JsonObject json) throws IOException, ClassNotFoundException;
}
