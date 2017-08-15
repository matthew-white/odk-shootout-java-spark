package org.opendatakit.thin.models;

import org.opendatakit.thin.ApiRepresentable;
import org.opendatakit.thin.Application;
import org.opendatakit.thin.Config;
import org.sql2o.Connection;
import org.sql2o.Query;
import org.sql2o.Sql2o;
import org.sql2o.Sql2oException;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class AbstractModel implements ApiRepresentable {
	private static final Sql2o SQL2O = Config.newSql2o();
	// Maps the name of a column to its value.
	private final Map<String, Object> values;

	public static Sql2o sql2o() {
		return SQL2O;
	}

	public static Connection connection() {
		return sql2o().open();
	}

	public static Connection transaction() {
		return sql2o().beginTransaction();
	}

	// TODO: Do Spark or Sql2o provide built-in support for logging?
	protected static void log(Query query, Map<String, Object> parameters) {
		Application.logger().info("{}  {}", query, parameters);
	}

	protected static void log(Query query, String... parameters) {
		if (parameters.length % 2 != 0)
			throw new IllegalArgumentException("invalid parameters");
		Map map = new LinkedHashMap();
		for (int i = 0; i < parameters.length; i += 2)
			map.put(parameters[i], parameters[i + 1]);
		log(query, map);
	}

	public AbstractModel() {
		values = new HashMap<>();
	}

	// Returns the metadata of the underlying table.
	public abstract TableMetadata table();

	// Returns true if the record is valid and false if not.
	public abstract boolean isValid();

	// Gets a column value by column name, throwing an exception if no such column
	// exists.
	public Object value(String columnName) {
		if (!table().columnNames().contains(columnName))
			throw new IllegalArgumentException();
		return values.get(columnName);
	}

	// Sets a column value by column name, throwing an exception if no such column
	// exists.
	// TODO: Is there a way to make this more type-safe?
	protected void value(String columnName, Object value) {
		if (!table().columnNames().contains(columnName))
			throw new IllegalArgumentException();
		values.put(columnName, value);
	}

	public Long getId() {
		return (Long) value("id");
	}

	public void setId(Long id) {
		value("id", id);
	}

	public boolean isPersisted() {
		return getId() != null;
	}

	// executeUpdate() calls the executeUpdate() method of a Query object,
	// providing logging and committing the Query's transaction. If the Query is
	// an INSERT statement and returnGeneratedKey is true, executeUpdate() returns
	// the new row's ID; otherwise it returns null.
	private Long executeUpdate(Query query, boolean returnGeneratedKey) {
		Long id = null;
		log(query, values);
		try {
			Connection connection = query.executeUpdate();
			if (returnGeneratedKey)
				id = connection.getKey(Long.class);
			query.getConnection().commit();
		}
		catch (Sql2oException e) {
			Application.logger().info("Query threw an exception:\n{}",
				e.getMessage());
			throw e;
		}
		return id;
	}

	// Returns a SQL INSERT statement as String.
	private String insertSql() {
		StringBuilder sql = new StringBuilder("INSERT INTO ")
			.append(table().name())
			.append(" (");
		List<String> notId = table()
			.columnNames()
			.stream()
			.filter(name -> !name.equals("id"))
			.collect(Collectors.toList());
		sql
			.append(String.join(", ", notId))
			.append(") VALUES (:")
			.append(String.join(", :", notId))
			.append(')');
		return sql.toString();
	}

	// Returns a Query object for an INSERT statement.
	private Query insertQuery(Connection connection) {
		Query query = connection.createQuery(insertSql(), true);
		for (String name : table().columnNames()) {
			if (name.equals("id"))
				continue;
			query.addParameter(name, value(name));
		}
		return query;
	}

	// Executes a SQL INSERT statement, returning true if the statement is
	// successful and false if not.
	private boolean insert() {
		try (Connection connection = transaction()) {
			boolean success = true;
			Long id = null;
			try {
				id = executeUpdate(insertQuery(connection), true);
			}
			catch (Sql2oException e) {
				// TODO: Confirm that a failed statement always throws a Sql2oException.
				success = false;
			}
			if (success)
				setId(id);
			return success;
		}
	}

	// Returns a SQL UPDATE statement as String.
	private String updateSql() {
		StringBuilder sql = new StringBuilder("UPDATE ")
			.append(table().name())
			.append(" SET ");
		boolean first = true;
		for (String name : table().columnNames()) {
			if (!first)
				sql.append(", ");
			sql.append(name).append(" = :").append(name);
			first = false;
		}
		sql.append(" WHERE id = :id");
		return sql.toString();
	}

	// Returns a Query object for an UPDATE statement.
	private Query updateQuery(Connection connection) {
		Query query = connection.createQuery(updateSql());
		for (String name : table().columnNames())
			query.addParameter(name, value(name));
		return query;
	}

	// Executes a SQL UPDATE statement, returning true if the statement is
	// successful and false if not.
	private boolean update() {
		try (Connection connection = transaction()) {
			boolean success = true;
			try {
				executeUpdate(updateQuery(connection), false);
			}
			catch (Sql2oException e) {
				success = false;
			}
			return success;
		}
	}

	// save() saves the record, executing either an INSERT or UPDATE statement
	// after checking that the record is valid. It returns true if the record was
	// saved successfully and false if not.
	public boolean save() {
		if (!isValid())
			return false;
		return isPersisted() ? update() : insert();
	}

	public String toString() {
		return values.toString();
	}
}
