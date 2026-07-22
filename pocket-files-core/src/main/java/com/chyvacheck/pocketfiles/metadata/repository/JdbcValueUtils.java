package com.chyvacheck.pocketfiles.metadata.repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Objects;

/**
 * Utility methods for working with nullable JDBC values.
 */
final class JdbcValueUtils {
	private JdbcValueUtils() {
		// Utility class should not be instantiated.
	}

	/**
	 * Sets a nullable long value in a prepared statement.
	 *
	 * @param statement prepared statement to use
	 * @param index     parameter index
	 * @param value     value to set, or null
	 * @throws SQLException if the value cannot be set
	 */
	static void setLongOrNull(PreparedStatement statement, int index, Long value) throws SQLException {
		Objects.requireNonNull(statement, "statement must not be null");

		if (value == null) {
			statement.setNull(index, Types.INTEGER);
			return;
		}

		statement.setLong(index, value);
	}

	/**
	 * Reads a nullable long value from the current result set row.
	 *
	 * @param resultSet  result set positioned on a valid row
	 * @param columnName column name to read
	 * @return long value or null if the SQL value is NULL
	 * @throws SQLException if the value cannot be read
	 */
	static Long getLongOrNull(ResultSet resultSet, String columnName) throws SQLException {
		Objects.requireNonNull(resultSet, "resultSet must not be null");
		Objects.requireNonNull(columnName, "columnName must not be null");

		long value = resultSet.getLong(columnName);

		if (resultSet.wasNull()) {
			return null;
		}

		return value;
	}
}
