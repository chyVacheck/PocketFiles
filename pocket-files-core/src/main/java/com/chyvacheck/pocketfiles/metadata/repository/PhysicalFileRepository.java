package com.chyvacheck.pocketfiles.metadata.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import com.chyvacheck.pocketfiles.metadata.model.PhysicalFileMetadata;
import com.chyvacheck.pocketfiles.metadata.status.PhysicalFileStatus;

public final class PhysicalFileRepository {

	/**
	 * The SQL statement to insert a physical file metadata into the database.
	 */
	private static final String INSERT_SQL = """
			INSERT INTO physical_files (
			    uuid,
			    original_name,
			    relative_path,
			    mime_type,
			    extension,
			    size_bytes,
			    sha256,
			    status,
			    created_at,
			    status_changed_at,
			    deleted_at
			) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
			""";

	/**
	 * The SQL statement to find a physical file metadata by its ID.
	 */
	private static final String FIND_BY_ID_SQL = """
			SELECT
			    id,
			    uuid,
			    original_name,
			    relative_path,
			    mime_type,
			    extension,
			    size_bytes,
			    sha256,
			    status,
			    created_at,
			    status_changed_at,
			    deleted_at
			FROM physical_files
			WHERE id = ?
			""";

	/**
	 * The SQL statement to find a physical file metadata by its ID.
	 */
	private static final String FIND_BY_UUID_SQL = """
			SELECT
			    id,
			    uuid,
			    original_name,
			    relative_path,
			    mime_type,
			    extension,
			    size_bytes,
			    sha256,
			    status,
			    created_at,
			    status_changed_at,
			    deleted_at
			FROM physical_files
			WHERE uuid = ?
			""";

	// Methods

	/**
	 * Inserts a physical file metadata into the database.
	 *
	 * @param connection The database connection to use.
	 * @param metadata   The physical file metadata to insert.
	 * @return The inserted physical file metadata.
	 * @throws SQLException If an SQL error occurs.
	 */
	public PhysicalFileMetadata insert(Connection connection, PhysicalFileMetadata metadata) throws SQLException {
		Objects.requireNonNull(connection, "connection must not be null");
		Objects.requireNonNull(metadata, "metadata must not be null");

		try (PreparedStatement statement = connection.prepareStatement(
				INSERT_SQL,
				Statement.RETURN_GENERATED_KEYS)) {

			statement.setString(1, metadata.uuid().toString());
			statement.setString(2, metadata.originalName());
			statement.setString(3, metadata.relativePath());

			statement.setString(4, metadata.mimeType());
			statement.setString(5, metadata.extension());

			statement.setLong(6, metadata.sizeBytes());
			statement.setString(7, metadata.sha256());

			statement.setInt(8, metadata.status().getCode());
			statement.setLong(9, metadata.createdAt());
			statement.setLong(10, metadata.statusChangedAt());
			this.setLongOrNull(statement, 11, metadata.deletedAt());

			statement.executeUpdate();

			try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
				if (!generatedKeys.next()) {
					throw new SQLException("Failed to retrieve generated physical file id");
				}

				long id = generatedKeys.getLong(1);

				return PhysicalFileMetadata.at(
						id,
						metadata.uuid(),
						metadata.originalName(),
						metadata.relativePath(),
						metadata.mimeType(),
						metadata.extension(),
						metadata.sizeBytes(),
						metadata.sha256(),
						metadata.status(),
						metadata.createdAt(),
						metadata.statusChangedAt(),
						metadata.deletedAt());
			}

		}

	}

	/**
	 * Finds physical file metadata by database ID.
	 *
	 * @param connection database connection to use
	 * @param id         database ID of the physical file metadata
	 * @return optional physical file metadata
	 * @throws SQLException if an SQL error occurs
	 */
	public Optional<PhysicalFileMetadata> findById(Connection connection, long id) throws SQLException {
		Objects.requireNonNull(connection, "connection must not be null");

		if (id <= 0) {
			throw new IllegalArgumentException("id must be positive");
		}

		try (PreparedStatement statement = connection.prepareStatement(FIND_BY_ID_SQL)) {
			statement.setLong(1, id);

			try (ResultSet resultSet = statement.executeQuery()) {
				if (!resultSet.next()) {
					return Optional.empty();
				}

				return Optional.of(this.mapRow(resultSet));
			}
		}
	}

	/**
	 * Finds physical file metadata by UUID.
	 *
	 * @param connection database connection to use
	 * @param uuid       UUID of the physical file metadata
	 * @return optional physical file metadata
	 * @throws SQLException if an SQL error occurs
	 */
	public Optional<PhysicalFileMetadata> findByUuid(Connection connection, UUID uuid) throws SQLException {
		Objects.requireNonNull(connection, "connection must not be null");
		Objects.requireNonNull(uuid, "uuid must not be null");

		try (PreparedStatement statement = connection.prepareStatement(FIND_BY_UUID_SQL)) {
			statement.setString(1, uuid.toString());

			try (ResultSet resultSet = statement.executeQuery()) {
				if (!resultSet.next()) {
					return Optional.empty();
				}

				return Optional.of(this.mapRow(resultSet));
			}
		}
	}

	// Helpers

	/**
	 * Maps the current result set row to physical file metadata.
	 *
	 * @param resultSet result set positioned on a valid row
	 * @return mapped physical file metadata
	 * @throws SQLException if a column cannot be read
	 */
	private PhysicalFileMetadata mapRow(ResultSet resultSet) throws SQLException {
		return PhysicalFileMetadata.at(
				resultSet.getLong("id"),
				UUID.fromString(resultSet.getString("uuid")),
				resultSet.getString("original_name"),
				resultSet.getString("relative_path"),
				resultSet.getString("mime_type"),
				resultSet.getString("extension"),
				resultSet.getLong("size_bytes"),
				resultSet.getString("sha256"),
				PhysicalFileStatus.fromCode(resultSet.getInt("status")),
				resultSet.getLong("created_at"),
				resultSet.getLong("status_changed_at"),
				this.getLongOrNull(resultSet, "deleted_at"));
	}

	/**
	 * Sets a long value or null in a prepared statement.
	 *
	 * @param statement The prepared statement to use.
	 * @param index     The index of the parameter to set.
	 * @param value     The value to set.
	 * @throws SQLException If an SQL error occurs.
	 */
	private void setLongOrNull(PreparedStatement statement, int index, Long value)
			throws SQLException {
		Objects.requireNonNull(statement, "statement must not be null");

		if (value != null) {
			statement.setLong(index, value);
		} else {
			statement.setNull(index, Types.INTEGER);
		}
	}

	/**
	 * Reads a nullable long value from the current result set row.
	 *
	 * @param resultSet  result set positioned on a valid row
	 * @param columnName column name to read
	 * @return long value or null if the SQL value is NULL
	 * @throws SQLException if the column cannot be read
	 */
	private Long getLongOrNull(ResultSet resultSet, String columnName) throws SQLException {
		long value = resultSet.getLong(columnName);

		if (resultSet.wasNull()) {
			return null;
		}

		return value;
	}

}
