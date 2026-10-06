package org.zenframework.z8.server.ie;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.zenframework.z8.server.base.table.value.Field;
import org.zenframework.z8.server.engine.RmiIO;
import org.zenframework.z8.server.engine.RmiSerializable;
import org.zenframework.z8.server.json.parser.JsonObject;
import org.zenframework.z8.server.types.guid;

public class TableRules implements RmiSerializable, Serializable {
	private static final long serialVersionUID = 7712259310049227053L;

	private static final String JsonDefaultPolicy = "defaultPolicy";
	private static final String JsonRecords = "records";
	private static final String JsonFields = "fields";
	private static final String JsonRecordFields = "recordFields";

	private Map<guid, Map<String, ImportPolicy>> recordFields = new HashMap<guid, Map<String, ImportPolicy>>();
	private Map<guid, ImportPolicy> records = new HashMap<guid, ImportPolicy>();
	private Map<String, ImportPolicy> fields = new HashMap<String, ImportPolicy>();
	private ImportPolicy defaultPolicy = ImportPolicy.Default;

	public TableRules() {
	}

	public TableRules(ImportPolicy policy) {
		setPolicy(policy);
	}

	public TableRules(guid recordId, ImportPolicy policy) {
		setPolicy(recordId, policy);
	}

	public TableRules(String field, ImportPolicy policy) {
		setPolicy(field, policy);
	}

	public TableRules(guid recordId, String field, ImportPolicy policy) {
		setPolicy(recordId, field, policy);
	}

	public void setPolicy(ImportPolicy policy) {
		defaultPolicy = policy;
	}

	public void setPolicy(String field, ImportPolicy policy) {
		fields.put(field, policy);
	}

	public void setPolicy(guid recordId, ImportPolicy policy) {
		records.put(recordId, policy);
	}

	public void setPolicy(guid recordId, String field, ImportPolicy policy) {
		Map<String, ImportPolicy> fields = recordFields.get(recordId);

		if (fields == null) {
			fields = new HashMap<String, ImportPolicy>();
			recordFields.put(recordId, fields);
		}

		fields.put(field, policy);
	}

	public ImportPolicy getPolicy(guid recordId) {
		ImportPolicy policy = records.get(recordId);
		return policy != null ? policy : defaultPolicy;
	}

	public ImportPolicy getPolicy(guid recordId, Field field) {
		return getPolicy(recordId, field.id());
	}

	public ImportPolicy getPolicy(guid recordId, String field) {
		Map<String, ImportPolicy> fieldsMap = recordFields.get(recordId);

		ImportPolicy policy = null;

		if (fieldsMap != null)
			policy = fieldsMap.get(field);

		if (policy != null)
			return policy;

		policy = records.get(recordId);

		if (policy != null)
			return policy;

		policy = fields.get(field);

		return policy != null ? policy : defaultPolicy;
	}

	private void writeObject(ObjectOutputStream out) throws IOException {
		serialize(out);
	}

	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		deserialize(in);
	}

	@Override
	public void serialize(ObjectOutputStream out) throws IOException {
		RmiIO.writeLong(out, serialVersionUID);

		out.writeObject(recordFields);
		out.writeObject(records);
		out.writeObject(fields);
		out.writeObject(defaultPolicy);
	}

	@Override
	@SuppressWarnings("unchecked")
	public void deserialize(ObjectInputStream in) throws IOException, ClassNotFoundException {
		@SuppressWarnings("unused")
		long version = RmiIO.readLong(in);

		recordFields = (Map<guid, Map<String, ImportPolicy>>) in.readObject();
		records = (Map<guid, ImportPolicy>) in.readObject();
		fields = (Map<String, ImportPolicy>) in.readObject();
		defaultPolicy = (ImportPolicy) in.readObject();
	}

	public JsonObject toJson() {
		JsonObject result = new JsonObject();
		result.put(JsonDefaultPolicy, defaultPolicy.name());
		JsonObject recordsObject = new JsonObject();
		records.forEach((key, value) -> recordsObject.put(key.toString(), value.name()));
		result.put(JsonRecords, recordsObject);
		JsonObject fieldsObject = new JsonObject();
		fields.forEach((key, value) -> fieldsObject.put(key, value.name()));
		result.put(JsonFields, fieldsObject);
		JsonObject recordFieldsObject = new JsonObject();
		recordFields.forEach((key, value) -> {
			JsonObject innerJson = new JsonObject();
			value.forEach((innerKey, innerValue) -> innerJson.put(innerKey, innerValue.name()));
			recordFieldsObject.put(key.toString(), innerJson);
		});
		result.put(JsonRecordFields, recordFieldsObject);

		return result;
	}

	public static TableRules parseJson(JsonObject json) {
		TableRules result = new TableRules(ImportPolicy.valueOf(json.getString(JsonDefaultPolicy)));

		JsonObject recordsObject = json.getJsonObject(JsonRecords);
		Map<guid, ImportPolicy> records = new HashMap<guid, ImportPolicy>();
		recordsObject.keySet().forEach(key -> records.put(new guid(key), ImportPolicy.valueOf(recordsObject.getString(key))));
		result.records = records;

		JsonObject fieldsObject = json.getJsonObject(JsonFields);
		Map<String, ImportPolicy> fields = new HashMap<String, ImportPolicy>();
		fieldsObject.keySet().forEach(key -> fields.put(key, ImportPolicy.valueOf(recordsObject.getString(key))));
		result.fields = fields;

		JsonObject recordFieldsObject = json.getJsonObject(JsonRecordFields);
		Map<guid, Map<String, ImportPolicy>> recordFields = new HashMap<guid, Map<String, ImportPolicy>>();
		recordFieldsObject.keySet().forEach(key -> {
			JsonObject innerJson = recordFieldsObject.getJsonObject(key);
			Map<String, ImportPolicy> innerMap = new HashMap<String, ImportPolicy>();
			innerJson.keySet().forEach(innerKey -> innerMap.put(innerKey, ImportPolicy.valueOf(innerJson.getString(innerKey))));
			recordFields.put(new guid(key), innerMap);
		});
		result.recordFields = recordFields;

		return result;
	}
}
