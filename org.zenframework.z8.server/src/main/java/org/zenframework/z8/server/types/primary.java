package org.zenframework.z8.server.types;

import java.io.Serializable;
import java.lang.reflect.Constructor;

import org.zenframework.z8.server.db.DatabaseVendor;
import org.zenframework.z8.server.db.FieldType;
import org.zenframework.z8.server.json.parser.JsonObject;

public abstract class primary implements Comparable<primary>, Serializable {

	private static final long serialVersionUID = -6139111122281366413L;

	private final static String JsonClass = "class";
	private final static String JsonValue= "value";
	
	public FieldType type() {
		throw new UnsupportedOperationException();
	}

	public String toDbConstant(DatabaseVendor vendor) {
		throw new UnsupportedOperationException();
	}

	public integer z8_hashCode() {
		return new integer(hashCode());
	}

	public string z8_toString() {
		return new string(toString());
	}

	public string string() {
		return z8_toString();
	}
	
	public Object getValue() {
		if (this instanceof binary)
			return ((binary) this).get();
		if (this instanceof bool)
			return ((bool) this).get();
		if (this instanceof date)
			return ((date) this).get();
		if (this instanceof datespan)
			return ((datespan) this).get();
		if (this instanceof decimal)
			return ((decimal) this).get();
		if (this instanceof file)
			return ((file) this).get();
		if (this instanceof geometry)
			return ((geometry) this).get();
		if (this instanceof guid)
			return ((guid) this).get();
		if (this instanceof integer)
			return ((integer) this).get();
		if (this instanceof string)
			return ((string) this).get();
		throw new IllegalStateException();
	}

	public JsonObject toJson() {
		JsonObject json = new JsonObject();
		json.put(JsonClass, this.getClass().getName());
		json.put(JsonValue, this.toString());
		return json;
	}

	public static primary parseJson(JsonObject json) {
		if (json == null)
			return null;

		String className = json.has(JsonClass) ? json.getString(JsonClass) : string.class.getName();
		String rawValue = json.has(JsonValue) ? json.getString(JsonValue) : "";

		try {
			Class<?> clazz = Class.forName(className);

			Constructor<?> constructor = clazz.getDeclaredConstructor(String.class);
			constructor.setAccessible(true);

			Object instance = constructor.newInstance(rawValue);
			return primary.class.cast(instance);
		} catch (Exception e) {
			throw new RuntimeException("Не удалось восстановить свойство primary для класса: " + className, e);
		}
	}

}
