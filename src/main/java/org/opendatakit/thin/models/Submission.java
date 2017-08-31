package org.opendatakit.thin.models;

import org.sql2o.Connection;
import org.sql2o.Query;
import org.sql2o.ResultSetIterable;

import java.util.List;

public class Submission extends AbstractModel {
	private static final TableMetadata TABLE =
		new TableMetadata("submissions", "formId", "instanceId", "data");

	public TableMetadata table() {
		return TABLE;
	}

	public String getFormId() {
		return (String) value("formId");
	}

	public void setFormId(String formId) {
		value("formId", formId);
	}

	public String getInstanceId() {
		return (String) value("instanceId");
	}

	public void setInstanceId(String instanceId) {
		value("instanceId", instanceId);
	}

	public String getData() {
		return (String) value("data");
	}

	public void setData(String data) {
		value("data", data);
	}

	public boolean isValid() {
		boolean valid = true;
		valid = valid && getFormId() != null && !getFormId().isEmpty();
		valid = valid && getInstanceId() != null && !getInstanceId().isEmpty();
		valid = valid && getData() != null && !getData().isEmpty();
		return valid;
	}

	private static class ApiRepresentation {
		private String formId, instanceId, data;

		// Needed for Gson to deserialize.
		public ApiRepresentation() { }

		public ApiRepresentation(Submission submission) {
			this.formId = submission.getFormId();
			this.instanceId = submission.getInstanceId();
			this.data = submission.getData();
		}
	}

	public Object forApi() {
		return new ApiRepresentation(this);
	}

	private interface Queries {
		// Leaving a trailing space so that it's easy to append additional SQL.
		String FOR_FORM_ID = "SELECT * FROM submissions WHERE formId = :formId ";
		String SAMPLE_FOR_FORM_ID = FOR_FORM_ID + "LIMIT 1000 ";
		String FIND_BY_INSTANCE_ID =
			"SELECT * FROM submissions    " +
			"WHERE                        " +
			"    formId = :formId AND     " +
			"    instanceId = :instanceId " ;
	}

	public static ResultSetIterable<Submission> forFormId(Connection connection,
		String formId) {
		Query query = connection
			.createQuery(Queries.FOR_FORM_ID)
			.addParameter("formId", formId);
		log(query, "formId", formId);
		return query.executeAndFetchLazy(Submission.class);
	}

	public static List<Submission> sampleForFormId(String formId) {
		try (Connection connection = connection()) {
			Query query = connection
				.createQuery(Queries.SAMPLE_FOR_FORM_ID)
				.addParameter("formId", formId);
			log(query, "formId", formId);
			return query.executeAndFetch(Submission.class);
		}
	}

	public static Submission findByFormAndInstance(String formId,
		String instanceId) {
		try (Connection connection = connection()) {
			Query query = connection
				.createQuery(Queries.FIND_BY_INSTANCE_ID)
				.addParameter("formId", formId)
				.addParameter("instanceId", instanceId);
			log(query, "formId", formId, "instanceId", instanceId);
			return query.executeAndFetchFirst(Submission.class);
		}
	}
}
