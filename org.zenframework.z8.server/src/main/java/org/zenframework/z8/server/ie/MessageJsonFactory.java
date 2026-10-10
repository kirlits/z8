package org.zenframework.z8.server.ie;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.zenframework.z8.server.json.JsonFactory;
import org.zenframework.z8.server.json.JsonIO;
import org.zenframework.z8.server.json.parser.JsonArray;
import org.zenframework.z8.server.json.parser.JsonException;
import org.zenframework.z8.server.json.parser.JsonObject;
import org.zenframework.z8.server.runtime.IObject;
import org.zenframework.z8.server.types.file;
import org.zenframework.z8.server.types.guid;
import org.zenframework.z8.server.types.primary;

public class MessageJsonFactory implements JsonFactory<Message> {
	protected static final String JsonId = "id";
	protected static final String JsonSender = "sender";
	protected static final String JsonAddress = "address";
	protected static final String JsonFile = "file";
	protected static final String JsonType = "type";
	protected static final String JsonDescription = "description";
	protected static final String JsonSource = "body";
	protected static final String JsonExportAll = "exportAll";
	protected static final String JsonSkipFiles = "skipFiles";
	protected static final String JsonProperties = "properties";
	protected static final String JsonInserts = "inserts";
	protected static final String JsonUpdates = "updates";
	protected static final String JsonSources = "data";
	protected static final String JsonRules= "rules";
	protected static final String JsonTableName = "tableName";
	protected static final String JsonFields = "fields";
	protected static final String JsonRecords= "records";
	protected static final String JsonDefaultPolicy = "defaultPolicy";
	protected static final String JsonTableRules = "tables";
	protected static final String JsonRecordFields = "recordFields";
	protected static final String JsonTable = "table";

	private static final Field tableRulesRecordFieldsField;
	private static final Field tableRulesRecordsField;
	private static final Field tableRulesFieldsField;
	private static final Field tableRulesDefaultPolicyField;

	private static final Field exportRulesDefaultPolicyField;
	private static final Field exportRulesTablesField;

	private static final Field exportSourceTableNameField;
	private static final Field exportSourceFieldNamesField;
	private static final Field exportSourceRecordsField;

	static {
		try {
			tableRulesRecordFieldsField = TableRules.class.getDeclaredField("recordFields");
			tableRulesRecordFieldsField.setAccessible(true);

			tableRulesRecordsField = TableRules.class.getDeclaredField("records");
			tableRulesRecordsField.setAccessible(true);

			tableRulesFieldsField = TableRules.class.getDeclaredField("fields");
			tableRulesFieldsField.setAccessible(true);

			tableRulesDefaultPolicyField = TableRules.class.getDeclaredField("defaultPolicy");
			tableRulesDefaultPolicyField.setAccessible(true);
		} catch (NoSuchFieldException e) {
			throw new RuntimeException("Failed to map TableRules private fields via reflection", e);
		}
		try {
			exportRulesDefaultPolicyField = ExportRules.class.getDeclaredField("defaultPolicy");
			exportRulesDefaultPolicyField.setAccessible(true);

			exportRulesTablesField = ExportRules.class.getDeclaredField("tables");
			exportRulesTablesField.setAccessible(true);
		} catch (NoSuchFieldException e) {
			throw new RuntimeException("Failed to map ExportRules private fields via reflection", e);
		}
		try {
			exportSourceTableNameField = ExportSource.class.getDeclaredField("tableName");
			exportSourceTableNameField.setAccessible(true);

			exportSourceFieldNamesField = ExportSource.class.getDeclaredField("fieldNames");
			exportSourceFieldNamesField.setAccessible(true);

			exportSourceRecordsField = ExportSource.class.getDeclaredField("records");
			exportSourceRecordsField.setAccessible(true);
		} catch (NoSuchFieldException e) {
			throw new RuntimeException("Failed to map ExportSource private fields via reflection", e);
		}
	}


	@Override
	public Class<Message> getSupportedClass() {
		return Message.class;
	}

	@Override
	public JsonObject toJson(Message instance) throws IOException {
		JsonObject json = basicToJson(instance);

		if(instance instanceof FileMessage)
			return toJson((FileMessage)instance, json);
		if(instance instanceof DataMessage)
			return toJson((DataMessage)instance, json);

		throw new IOException("Unsupported entity subclass for base " + Message.class.getName() + ": " + instance.getClass().getName());
	}
	
	protected JsonObject basicToJson(Message instance) {
		JsonObject json = new JsonObject();
		json.put(JsonId, instance.getId());
		json.put(JsonSender, instance.getSender());
		json.put(JsonAddress, instance.getAddress());

		return json;
	}

	protected JsonObject toJson(FileMessage instance, JsonObject json) throws IOException {
		json.put(JsonFile, instance.getFile().toJsonObject());
		return json;
	}

	protected JsonObject toJson(DataMessage instance, JsonObject json) throws IOException {
		json.put(JsonType, instance.getType());
		json.put(JsonDescription, instance.getDescription());
		json.put(JsonSource, toJson(instance.getSource()));

		return json;
	}

	private JsonObject toJson(MessageSource instance) throws IOException {
		JsonObject result = new JsonObject();
		result.put(JsonExportAll, instance.isExportAll());
		result.put(JsonSkipFiles, instance.isSkipFiles());

		Map<String, primary> properties = instance.getProperties();
		JsonObject jsonProperties = new JsonObject();
		for (Map.Entry<String, primary> entry : properties.entrySet())
			jsonProperties.put(entry.getKey(), JsonIO.toJson(entry.getValue()));
		result.put(JsonProperties, jsonProperties);

		Collection<ExportSource> sources = instance.getSources();
		JsonArray records = new JsonArray();
		for(ExportSource source : sources)
			records.put(toJson(source));
		result.put(JsonSources, records);

		result.put(JsonRules, toJson(instance.exportRules()));
		result.put(JsonInserts, toJson(instance.getInserts()));
		result.put(JsonUpdates, toJson(instance.getUpdates()));

		return result;
	}

	@SuppressWarnings("unchecked")
	private JsonObject toJson(ExportSource instance) throws IOException {
		try {
			JsonObject result = new JsonObject();
			result.put(JsonTableName, instance.name());
			result.put(JsonRecords, instance.records());
		
			Collection<String> fieldNames = (Collection<String>) exportSourceFieldNamesField.get(instance);
			result.put(JsonFields, fieldNames);
			return result;
		} catch (IllegalAccessException e) {
			throw new IOException("Unauthorized reflection access during ExportSource json serialization", e);
		}
	}

	@SuppressWarnings("unchecked")
	private JsonObject toJson(ExportRules instance) throws IOException {
		try {
			JsonObject result = new JsonObject();
			ImportPolicy defaultPolicy = (ImportPolicy) exportRulesDefaultPolicyField.get(instance);
			result.put(JsonDefaultPolicy, defaultPolicy.name());

			Map<String, TableRules> tables = (Map<String, TableRules>) exportRulesTablesField.get(instance);
			JsonObject tableRules = new JsonObject();
			for (Map.Entry<String, TableRules> entry : tables.entrySet()) {
				tableRules.put(entry.getKey(), toJson(entry.getValue()));
			}
			result.put(JsonTableRules, tableRules);

			return result;
		} catch (IllegalAccessException e) {
			throw new IOException("Unauthorized reflection access during ExportRules json serialization", e);
		}
	}

	@SuppressWarnings("unchecked")
	private JsonObject toJson(TableRules instance) throws IOException {
		try {
			JsonObject result = new JsonObject();
			ImportPolicy defaultPolicy = (ImportPolicy) tableRulesDefaultPolicyField.get(instance);
			result.put(JsonDefaultPolicy, defaultPolicy.name());
	
			Map<guid, ImportPolicy> records = (Map<guid, ImportPolicy>) tableRulesRecordsField.get(instance);
			JsonObject recordsObject = new JsonObject();
			records.forEach((key, value) -> recordsObject.put(key.toString(), value.name()));
			result.put(JsonRecords, recordsObject);
	
			Map<String, ImportPolicy> fields = (Map<String, ImportPolicy>) tableRulesFieldsField.get(instance);
			JsonObject fieldsObject = new JsonObject();
			fields.forEach((key, value) -> fieldsObject.put(key, value.name()));
			result.put(JsonFields, fieldsObject);
	
			Map<guid, Map<String, ImportPolicy>> recordFields = (Map<guid, Map<String, ImportPolicy>>) tableRulesRecordFieldsField.get(instance);
			JsonObject recordFieldsObject = new JsonObject();
			recordFields.forEach((key, value) -> {
				JsonObject innerJson = new JsonObject();
				value.forEach((innerKey, innerValue) -> innerJson.put(innerKey, innerValue.name()));
				recordFieldsObject.put(key.toString(), innerJson);
			});
			result.put(JsonRecordFields, recordFieldsObject);
	
			return result;
		} catch (IllegalAccessException e) {
			throw new IOException("Unauthorized reflection access during TableRules json serialization", e);
		}
	}

	private JsonArray toJson(Collection<RecordInfo> infos) throws JsonException, IOException {
		JsonArray result = new JsonArray();
		for(RecordInfo info : infos)
			result.put(toJson(info));
		return result;
	}

	private JsonObject toJson(RecordInfo instance) throws IOException {
		JsonObject result = new JsonObject();
		result.put(JsonId, instance.id());
		result.put(JsonTable, instance.table());

		Collection<FieldInfo> fields = instance.fields();
		JsonObject infos = new JsonObject();
		for(FieldInfo field : fields)
			infos.put(field.name(), JsonIO.toJson(field.value()));
		result.put(JsonFields, infos);

		return result;
	}

	@Override
	public Message fromJson(JsonObject json, Class<? extends Message> instanceClass) throws IOException, ClassNotFoundException {
		if (json == null) {
			return null;
		}

		Message instance = init(json, instanceClass);

		if (instance instanceof FileMessage)
			return fromJsonToFileMessage((FileMessage)instance, json);
		if (instance instanceof DataMessage)
			return fromJsonToDataMessage((DataMessage)instance, json);

		throw new IOException("Unsupported target message instance class during deserialization: " + instanceClass.getName());
	}

	protected Message init(JsonObject json, Class<? extends Message> instanceClass) {
		Message instance = createInstance(instanceClass, null);

		instance.setId(json.getGuid(JsonId));
		instance.setSender(json.getString(JsonSender));
		instance.setAddress(json.getString(JsonAddress));

		return instance;
	}

	private Message createInstance(Class<? extends Message> instanceClass, IObject container) {
		try {
			Constructor<?> constructor = instanceClass.getDeclaredConstructor(IObject.class);
			Object instance = constructor.newInstance(container);
			return Message.class.cast(instance);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	protected FileMessage fromJsonToFileMessage(FileMessage instance, JsonObject json) {
		instance.setFile(new file(json.getJsonObject(JsonFile)));

		return instance;
	}

	protected DataMessage fromJsonToDataMessage(DataMessage instance, JsonObject json) throws ClassNotFoundException, IOException {
		instance.setType(json.getString(JsonType));
		instance.setDescription(json.getString(JsonDescription));
		instance.setSource(fromJsonToMessageSource(json.getJsonObject(JsonSource)));

		return instance;
	}

	private MessageSource fromJsonToMessageSource(JsonObject json) throws IOException, ClassNotFoundException {
		MessageSource source = new MessageSource();
		source.setExportAll(json.getBoolean(JsonExportAll));
		source.setSkipFiles(json.getBoolean(JsonSkipFiles));

		JsonObject jsonProperties = json.getJsonObject(JsonProperties);
		for(String key : jsonProperties.keySet())
			source.properties.put(key, (primary)JsonIO.fromJson(jsonProperties.getJsonObject(key)));

		JsonArray records = json.getJsonArray(JsonSources);
		Collection<ExportSource> sources = new ArrayList<ExportSource>();
		for (int i = 0; i < records.size(); i++)
			sources.add(fromJsonToExportSource(records.getJsonObject(i)));
		source.sources = sources;

		source.exportRules = fromJsonToExportRules(json.getJsonObject(JsonRules));
		source.setInserts(fromJsonToCollectionRecordInfo(json.getJsonArray(JsonInserts)));
		source.setUpdates(fromJsonToCollectionRecordInfo(json.getJsonArray(JsonUpdates)));

		return source;
	}

	private ExportSource fromJsonToExportSource(JsonObject json) throws IOException {
		try {
			ExportSource result = new ExportSource();
			exportSourceTableNameField.set(result, json.getString(JsonTableName));

			JsonArray jsonRecords = json.getJsonArray(JsonRecords);
			Collection<guid> records = new ArrayList<guid>();
			for (int i = 0; i < jsonRecords.size(); i++)
				records.add(jsonRecords.getGuid(i));
			exportSourceRecordsField.set(result, records);

			JsonArray jsonFieldNames = json.getJsonArray(JsonFields);
			Collection<String> fieldNames = new ArrayList<String>();
			for (int i = 0; i < jsonFieldNames.size(); i++)
				fieldNames.add(jsonFieldNames.getString(i));
			exportSourceFieldNamesField.set(result, fieldNames);

			return result;
		} catch (IllegalAccessException e) {
			throw new IOException("Unauthorized reflection access during ExportSource json deserialization", e);
		}
	}

	private ExportRules fromJsonToExportRules(JsonObject json) throws IOException {
		try {
			ExportRules result = new ExportRules();
			result.add(ImportPolicy.valueOf(json.getString(JsonDefaultPolicy)));

			JsonObject tableRules = json.getJsonObject(JsonTableRules);
			Map<String, TableRules> tables = new HashMap<String, TableRules>();
			for(String key : tableRules.keySet())
				tables.put(key, fromJsonToTableRules(tableRules.getJsonObject(key)));
			exportRulesTablesField.set(result, tables);

			return result;
		} catch (IllegalAccessException e) {
			throw new IOException("Unauthorized reflection access during TableRules json deserialization", e);
		}
	}

	private TableRules fromJsonToTableRules(JsonObject json) throws IOException {
		try {
			TableRules result = new TableRules(ImportPolicy.valueOf(json.getString(JsonDefaultPolicy)));

			JsonObject recordsObject = json.getJsonObject(JsonRecords);
			Map<guid, ImportPolicy> records = new HashMap<>();
			recordsObject.keySet().forEach(key -> records.put(new guid(key), ImportPolicy.valueOf(recordsObject.getString(key))));
			tableRulesRecordsField.set(result, records);

			JsonObject fieldsObject = json.getJsonObject(JsonFields);
			Map<String, ImportPolicy> fields = new HashMap<String, ImportPolicy>();
			fieldsObject.keySet().forEach(key -> fields.put(key, ImportPolicy.valueOf(fieldsObject.getString(key))));
			tableRulesFieldsField.set(result, fields);

			JsonObject recordFieldsObject = json.getJsonObject(JsonRecordFields);
			Map<guid, Map<String, ImportPolicy>> recordFields = new HashMap<guid, Map<String, ImportPolicy>>();
			recordFieldsObject.keySet().forEach(key -> {
				JsonObject innerJson = recordFieldsObject.getJsonObject(key);
				Map<String, ImportPolicy> innerMap = new HashMap<String, ImportPolicy>();
				innerJson.keySet().forEach(innerKey -> innerMap.put(innerKey, ImportPolicy.valueOf(innerJson.getString(innerKey))));
				recordFields.put(new guid(key), innerMap);
			});
			tableRulesRecordFieldsField.set(result, recordFields);

			return result;
		} catch (IllegalAccessException e) {
			throw new IOException("Unauthorized reflection access during TableRules json deserialization", e);
		}
	}
	
	private Collection<RecordInfo> fromJsonToCollectionRecordInfo(JsonArray infos) throws ClassNotFoundException, IOException {
		Collection<RecordInfo> result = new ArrayList<RecordInfo>();
		for(int i = 0; i < infos.size(); i++)
			result.add(fromJsonToRecordInfo(infos.getJsonObject(i)));
		return result;
	}

	private RecordInfo fromJsonToRecordInfo(JsonObject json) throws ClassNotFoundException, IOException {
		RecordInfo result = new RecordInfo(json.getGuid("id"), json.getString("table"));
		JsonObject infos = json.getJsonObject("fields");
		for(String key : infos.keySet())
			result.add(new FieldInfo(key, (primary)JsonIO.fromJson(infos.getJsonObject(key))));

		return result;
	}

}
