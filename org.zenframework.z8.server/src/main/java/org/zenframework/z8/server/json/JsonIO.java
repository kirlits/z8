package org.zenframework.z8.server.json;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.zenframework.z8.server.engine.IOFactoryManager;
import org.zenframework.z8.server.json.parser.JsonArray;
import org.zenframework.z8.server.json.parser.JsonObject;
import org.zenframework.z8.server.types.primary;

public class JsonIO {
	private static final String JsonClass = "class";
	private static final String JsonKey = "key";
	private static final String JsonValue = "value";
	//private static final String ReferenceClass = "reference";
	//private static final String This = "this";
	private static final JsonObject NullObject;

	static {
		NullObject = new JsonObject();
		NullObject.put(JsonClass, JsonObject.NULL);
		NullObject.put(JsonValue, JsonObject.NULL);
	}

	private static final Map<String, Class<?>> classes = Collections.synchronizedMap(new HashMap<String, Class<?>>());
	private static final Map<String, Constructor<?>> constructors = Collections.synchronizedMap(new HashMap<String, Constructor<?>>());
	//private static final ThreadLocal<Map<OBJECT, String>> visitedObjects = ThreadLocal.withInitial(IdentityHashMap::new);
	//private static final ThreadLocal<Map<String, Object>> deserializedObjects = ThreadLocal.withInitial(java.util.HashMap::new);
	@SuppressWarnings("unchecked")
	private static final IOFactoryManager<JsonFactory<Object>> jsonFactoryManager = IOFactoryManager.getInstance((Class<JsonFactory<Object>>) (Class<?>) JsonFactory.class);

	private JsonIO() { }

	// --- REFLECTION INFRASTRUCTURE ---
	private static Class<?> getClass(String name) {
		try {
			Class<?> cls = classes.get(name);
			if (cls != null)
				return cls;
			cls = Class.forName(name);
			classes.put(name, cls);
			return cls;
		} catch (Throwable e) {
			throw new RuntimeException("JsonIO: Failed to load class " + name, e);
		}
	}

	private static Constructor<?> getConstructor(String className, Class<?>[] parameters) {
		try {
			String paramTypeStr = (parameters != null && parameters.length > 0) ? parameters[0].getName() : "void";
			String cacheKey = className + ":" + paramTypeStr;
			Constructor<?> constructor = constructors.get(cacheKey);
			if (constructor != null)
				return constructor;

			constructor = getClass(className).getDeclaredConstructor(parameters);
			constructor.setAccessible(true);
			constructors.put(cacheKey, constructor);
			return constructor;
		} catch (Throwable e) {
			throw new RuntimeException("JsonIO: Constructor not found for class " + className, e);
		}
	}

	private static Object newObject(String name, Class<?>[] arguments, Object[] parameters) {
		try {
			return getConstructor(name, arguments).newInstance(parameters);
		} catch (Throwable e) {
			throw new RuntimeException("JsonIO: Failed to instantiate class " + name, e);
		}
	}
	// --- END REFLECTION INFRASTRUCTURE ---

	public static JsonObject toJson(Object object) throws IOException {
	//Second "toJson" is for future use to prevent cycle links in OBJECTs
		/*return toJson(object, This);
	}

	private static JsonObject toJson(Object object, String currentPath) throws IOException {*/
		if (object == null) {
			return NullObject;
		}

		JsonObject json = new JsonObject();
		Class<?> objectClass = object.getClass();
		JsonFactory<Object> objectFactory = jsonFactoryManager.getFactory(objectClass);
		json.put(JsonClass, objectClass.getName());

		// --- Standard Java Primitives & Basic Types ---
		if (object instanceof Boolean) {
			json.put(JsonValue, (Boolean) object);
		} else if (object instanceof Byte) {
			json.put(JsonValue, ((Byte) object).intValue());
		} else if (object instanceof Character) {
			json.put(JsonValue, object.toString());
		} else if (object instanceof Short) {
			json.put(JsonValue, ((Short) object).intValue());
		} else if (object instanceof Integer) {
			json.put(JsonValue, (Integer) object);
		} else if (object instanceof Long) {
			json.put(JsonValue, object.toString());
		} else if (object instanceof Float) {
			json.put(JsonValue, ((Float) object).doubleValue());
		} else if (object instanceof Double) {
			json.put(JsonValue, (Double) object);
		} else if (object instanceof String) {
			json.put(JsonValue, (String) object);
		} else if (object instanceof byte[]) {
			json.put(JsonValue, Base64.getEncoder().encodeToString((byte[]) object));

			// --- Primary Types (guid, bool, integer, decimal etc.) ---
		} else if (object instanceof primary) {
			json.put(JsonValue, object.toString());

			// --- Custom Serializable Entities ---
		} else if (objectFactory != null && objectFactory.overridesSerializable()) {
			json.put(JsonValue, objectFactory.toJson(object));
		} else if (object instanceof JsonSerializable) {
			json.put(JsonValue, ((JsonSerializable) object).toJson());
		} else if (objectFactory != null) {
			json.put(JsonValue, objectFactory.toJson(object));

			// --- Arrays, Collections, Maps ---
		} else if (object instanceof Object[]) {
			json.put(JsonValue, serializeArray((Object[]) object));
		} else if (object instanceof Collection) {
			json.put(JsonValue, serializeCollection((Collection<?>) object));
		} else if (object instanceof Map) {
			json.put(JsonValue, serializeMap((Map<?, ?>) object));

			// --- Platform Business Entities (OBJECT) with cycle protection ---
		/*} else if (object instanceof OBJECT) {
			OBJECT obj = (OBJECT) object;
			json.put(JsonValue, serializeOBJECT(obj, currentPath));*/
		} else if (object instanceof Enum<?>) {
			json.put(JsonValue, ((Enum<?>) object).name());
		} else {
			json.put(JsonValue, object.toString());
		}

		return json;
	}

	public static Object fromJson(JsonObject json) throws IOException, ClassNotFoundException {
		//Second "fromJson" is for future use to parse cycle links in OBJECTs
		/*return fromJson(json, This);
	}

	private static Object fromJson(JsonObject json, String path) throws IOException, ClassNotFoundException {*/
		if (json == null) return null;

		String className = json.getString(JsonClass);
		if (className == null) return null;

		//Parse references for future use
		/*if (ReferenceClass.equals(className)) {
			String refPath = json.getString(JsonValue);
			Object referencedInstance = deserializedObjects.get().get(refPath);
			if (referencedInstance == null) {
				throw new IOException("JsonIO: Broken circular reference path: " + refPath);
			}
			return referencedInstance;
		}*/

		Class<?> clazz = getClass(className);
		JsonFactory<Object> objectFactory = jsonFactoryManager.getFactory(clazz);

		try {
			// --- Standard Java Primitives & Basic Types ---
			if (clazz == Boolean.class) {
				return json.getBoolean(JsonValue);
			} else if (clazz == Byte.class) {
				return (byte) json.getInt(JsonValue);
			} else if (clazz == Character.class) {
				String str = json.getString(JsonValue);
				return str.isEmpty() ? (char) 0 : str.charAt(0);
			} else if (clazz == Short.class) {
				return (short) json.getInt(JsonValue);
			} else if (clazz == Integer.class) {
				return json.getInt(JsonValue);
			} else if (clazz == Long.class) {
				return Long.valueOf(json.getString(JsonValue));
			} else if (clazz == Float.class) {
				return (float) json.getDouble(JsonValue);
			} else if (clazz == Double.class) {
				return json.getDouble(JsonValue);
			} else if (clazz == String.class) {
				return json.getString(JsonValue);
			} else if (clazz == byte[].class) {
				String base64 = json.getString(JsonValue);
				return Base64.getDecoder().decode(base64);

			// --- Primary Types (guid, bool, integer, decimal etc.) ---
			} else if (primary.class.isAssignableFrom(clazz)) {
				String rawValue = json.getString(JsonValue);
				return clazz.cast(newObject(className, new Class<?>[] { String.class }, new Object[] { rawValue }));

				// --- Custom Serializable Entities ---
			} else if (objectFactory != null && objectFactory.overridesSerializable()) {
				JsonObject value = json.getJsonObject(JsonValue);
				return objectFactory.fromJson(value, clazz);
			} else if (JsonSerializable.class.isAssignableFrom(clazz)) {
				Object instance = newObject(className, null, null);
				JsonObject value = json.getJsonObject(JsonValue);
				((JsonSerializable) instance).fromJson(value);
				return instance;
			} else if (objectFactory != null) {
				JsonObject value = json.getJsonObject(JsonValue);
				return objectFactory.fromJson(value, clazz);

				// --- Arrays, Collections, Maps ---
			} else if (clazz.isArray()) {
				JsonArray jsonArray = json.getJsonArray(JsonValue);
				return deserializeArray(jsonArray, clazz);
			} else if (Collection.class.isAssignableFrom(clazz)) {
				JsonArray jsonArray = json.getJsonArray(JsonValue);
				return deserializeCollection(jsonArray, className);
			} else if (Map.class.isAssignableFrom(clazz)) {
				JsonArray jsonArray = json.getJsonArray(JsonValue);
				return deserializeMap(jsonArray, className);

			// --- Platform Business Entities (OBJECT) with cycle protection ---
			/*} else if (OBJECT.class.isAssignableFrom(clazz)) {
				JsonObject value = json.getJsonObject(JsonValue);
				return deserializeOBJECT(value, className, path);*/
			} else if (clazz.isEnum()) {
				String enumName = json.getString(JsonValue);
				@SuppressWarnings({ "unchecked", "rawtypes" })
				Object enumConstant = Enum.valueOf((Class<Enum>) clazz, enumName);
				return enumConstant;
			} else {
				// Fallback matching the exact toJson logic structure
				String rawValue = json.getString(JsonValue);
				return clazz.cast(newObject(className, new Class<?>[]{String.class}, new Object[]{rawValue}));
			}
			
		} catch (Exception e) {	
			throw new IOException("JsonIO: Failed to deserialize class " + className, e);
		}
	}

	private static JsonArray serializeArray(Object[] array) throws IOException {
		JsonArray jsonArray = new JsonArray();
		for (Object item : array) {
			jsonArray.add(toJson(item));
		}
		return jsonArray;
	}

	private static Object deserializeArray(JsonArray jsonArray, Class<?> clazz) throws Exception {
		Class<?> componentType = clazz.getComponentType();
		int length = jsonArray != null ? jsonArray.length() : 0;
		Object array = Array.newInstance(componentType, length);

		for (int i = 0; i < length; i++) {
			Array.set(array, i, fromJson(jsonArray.getJsonObject(i)));
		}
		return array;
	}

	private static JsonArray serializeCollection(Collection<?> collection) throws IOException {
		JsonArray jsonArray = new JsonArray();
		for (Object item : collection) {
			jsonArray.add(toJson(item));
		}
		return jsonArray;
	}

	private static Collection<Object> deserializeCollection(JsonArray jsonArray, String className) throws Exception {
		@SuppressWarnings("unchecked")
		Collection<Object> collection = (Collection<Object>) newObject(className, new Class<?>[]{}, new Object[]{});

		if (jsonArray != null) {
			int length = jsonArray.length();
			for (int i = 0; i < length; i++) {
				collection.add(fromJson(jsonArray.getJsonObject(i)));
			}
		}
		return collection;
	}

	private static JsonArray serializeMap(Map<?, ?> map) throws IOException {
		JsonArray jsonArray = new JsonArray();

		for (Map.Entry<?, ?> entry : map.entrySet()) {
			JsonObject pair = new JsonObject();
			pair.put(JsonKey, toJson(entry.getKey()));
			pair.put(JsonValue, toJson(entry.getValue()));
			jsonArray.add(pair);
		}

		return jsonArray;
	}

	private static Map<Object, Object> deserializeMap(JsonArray jsonArray, String className) throws Exception {
		@SuppressWarnings("unchecked")
		Map<Object, Object> map = (Map<Object, Object>) newObject(className, new Class<?>[]{}, new Object[]{});
		if (jsonArray != null) {
			int length = jsonArray.length();
			for (int i = 0; i < length; i++) {
				JsonObject pair = jsonArray.getJsonObject(i);

				Object restoredKey = fromJson(pair.getJsonObject(JsonKey));
				Object restoredValue = fromJson(pair.getJsonObject(JsonValue));
				
				map.put(restoredKey, restoredValue);
			}
		}
		return map;
	}

	// TODO / FOR FUTURE USE: Business Entities (OBJECT) Serialization Mechanics
	/*private static JsonObject serializeOBJECT(OBJECT obj, String path) throws IOException {
		JsonObject jsonObject = new JsonObject();
		Map<OBJECT, String> visited = visitedObjects.get();
		boolean isRootOBJECT = visited.isEmpty();

		try {
			if (visited.containsKey(obj)) {
				jsonObject.put(JsonClass, ReferenceClass);
				jsonObject.put(JsonValue, visited.get(obj));
			} else {
				visited.put(obj, path);
				for (IClass<? extends IObject> item : obj.objects) {
					IObject itemObject = item.get();
					if (itemObject != null) {
						String itemName = itemObject.index();
						jsonObject.put(itemName, toJson(itemObject, path + "." + itemName));
					}
				}
			}

			return jsonObject;
		} finally {
			if (isRootOBJECT) {
				visited.clear();
			}
		}
	}
	
	private static OBJECT deserializeOBJECT(JsonObject json, String className, String path) throws Exception {
		OBJECT instance = (OBJECT) newObject(className, new Class<?>[] { IObject.class }, new Object[] { null });

		Map<String, Object> visited = deserializedObjects.get();
		boolean isRoot = visited.isEmpty();
		visited.put(path, instance);

		try {
			JsonObject fields = json.getJsonObject(JsonValue);
			if (fields != null) {
				for (String itemName : fields.keySet()) {
					JsonObject itemJson = fields.getJsonObject(itemName);

					String nextPath = path + "." + itemName;
					Object childObject = fromJson(itemJson, nextPath);

					// Safe partial injection: filling only what was selected in DB query
					// instance.injectReadonlyField(itemName, childObject);
				}
			}
			return instance;
		} finally {
			if (isRoot) {
				visited.clear();
			}
		}
	}*/
}
