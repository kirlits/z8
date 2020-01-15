package org.zenframework.z8.server.base.model.actions;

import java.util.ArrayList;
import java.util.List;

import org.zenframework.z8.server.base.file.AttachmentProcessor;
import org.zenframework.z8.server.base.query.Query;
import org.zenframework.z8.server.base.table.value.AttachmentField;
import org.zenframework.z8.server.base.table.value.Field;
import org.zenframework.z8.server.json.Json;
import org.zenframework.z8.server.json.JsonWriter;
import org.zenframework.z8.server.json.parser.JsonArray;
import org.zenframework.z8.server.types.file;
import org.zenframework.z8.server.types.guid;

public class DetachAction extends Action {

	public DetachAction(ActionParameters parameters) {
		super(parameters);
	}

	@Override
	public void writeResponse(JsonWriter writer) throws Throwable {

		List<file> files = new ArrayList<file>();
		JsonArray jsonArray = new JsonArray(getDataParameter());

		for(int i = 0; i < jsonArray.length(); i++)
			files.add(new file(jsonArray.getGuid(i)));

		Query query = getRootQuery();
		guid target = getRecordIdParameter();
		String fieldId = getFieldParameter();

		Field field = fieldId != null ? query.findFieldById(fieldId) : null;

		AttachmentProcessor processor = new AttachmentProcessor((AttachmentField)field);

		writer.startArray(Json.data);

		for(file file : processor.remove(target, files))
			writer.write(file.toJsonObject());

		writer.finishArray();

	}

}