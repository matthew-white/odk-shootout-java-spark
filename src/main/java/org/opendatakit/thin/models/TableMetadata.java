package org.opendatakit.thin.models;

import com.google.common.collect.ImmutableSet;

import java.util.Set;

/*
TableMetadata encapsulates the metadata of a SQL table. It makes the following
assumptions:

	1. The table must have a primary key named id that can be stored as long.
	2. It must have an additional column other than id. In theory, a table could
	   have only the single column of id, but TableMetadata currently does not
	   support that use case.
 */
public class TableMetadata {
	private final String name;
	private final Set<String> columnNames;

	public TableMetadata(String name, String... columnNames) {
		if (name == null)
			throw new NullPointerException("name cannot be null");
		this.name = name;

		if (columnNames == null)
			throw new NullPointerException("columnNames cannot be null");
		this.columnNames = ImmutableSet.<String>builder()
			.add(columnNames)
			.add("id")
			.build();
		if (this.columnNames.size() == 1)
			throw new IllegalArgumentException("table must have more than one column");
	}

	// Returns the table name.
	public String name() {
		return name;
	}

	// Returns the names of the table's columns.
	public Set<String> columnNames() {
		return columnNames;
	}
}
