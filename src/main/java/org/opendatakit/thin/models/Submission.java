package org.opendatakit.thin.models;

import com.google.common.collect.ImmutableSet;
import org.sql2o.Connection;
import org.sql2o.Query;
import org.sql2o.ResultSetIterable;

import java.util.List;
import java.util.Set;

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
		String PAGE_OF_FIELDS =
			"select                                      " +
			"    id,                                     " +
			"    (data->>'age')::int as age,             " +
			"    (data->>'kilograms')::int as kilograms, " +
			"    (data->>'year')::int as year            " +
			"from submissions                            " +
			"where formId = :formId                      " +
			"order by :order, id                         " +
			"limit :limit                                " +
			"offset :offset                              " ;
		String FIND_BY_INSTANCE_ID =
			"SELECT *                     " +
			"FROM submissions             " +
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

	private static class Fields {
		private long id;
		private Integer age, kilograms, year;

		private static final Set<String> NAMES =
			ImmutableSet.of("id", "age", "kilograms", "year");

		public static Set<String> names() {
			return NAMES;
		}

		public Fields() { }

		public long getId() {
			return id;
		}

		public void setId(long id) {
			this.id = id;
		}

		public Integer getAge() {
			return age;
		}

		public void setAge(Integer age) {
			this.age = age;
		}

		public Integer getKilograms() {
			return kilograms;
		}

		public void setKilograms(Integer kilograms) {
			this.kilograms = kilograms;
		}

		public Integer getYear() {
			return year;
		}

		public void setYear(Integer year) {
			this.year = year;
		}
	}

	public static List<?> pageOfFields(String formId, String sortField,
		int perPage, int page) {
		if (perPage < 0 || page < 1 ||
			(sortField != null && !Fields.names().contains(sortField)))
			throw new IllegalArgumentException();
		try (Connection connection = connection()) {
			String order = sortField != null ? sortField : "id";
			String sql = Queries.PAGE_OF_FIELDS.replace(":order", order);
			int offset = perPage * (page - 1);
			Query query = connection
				.createQuery(sql)
				.addParameter("formId", formId)
				.addParameter("limit", perPage)
				.addParameter("offset", offset);
			log(query, "formId", formId, "order", order, "limit", perPage,
				"offset", offset);
			return query.executeAndFetch(Fields.class);
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
