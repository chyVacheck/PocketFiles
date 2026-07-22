package com.chyvacheck.pocketfiles.metadata.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import com.chyvacheck.pocketfiles.metadata.model.FileUsageMetadata;
import com.chyvacheck.pocketfiles.metadata.status.FileUsageStatus;

public final class FileUsageRepository {

	/**
	 * SQL statement to insert a file usage metadata into the database.
	 */
	private static final String INSERT_SQL = """
			INSERT INTO file_usages (
			    uuid,
			    physical_file_id,
			    usage_type,
			    owner_type,
			    owner_id,
			    display_name,
			    metadata_json,
			    status,
			    created_at,
			    deleted_at
			) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
			""";

	/**
	 * SQL statement to find a file usage metadata by ID.
	 */
	private static final String FIND_BY_ID_SQL = """
			SELECT
			    id,
			    uuid,
			    physical_file_id,
			    usage_type,
			    owner_type,
			    owner_id,
			    display_name,
			    metadata_json,
			    status,
			    created_at,
			    deleted_at
			FROM file_usages
			WHERE id = ?
			""";

	/**
	 * SQL statement to find a file usage metadata by UUID.
	 */
	private static final String FIND_BY_UUID_SQL = """
			SELECT
			    id,
			    uuid,
			    physical_file_id,
			    usage_type,
			    owner_type,
			    owner_id,
			    display_name,
			    metadata_json,
			    status,
			    created_at,
			    deleted_at
			FROM file_usages
			WHERE uuid = ?
			""";

	// Methods

	/**
	 * Inserts a file usage metadata into the database.
	 *
	 * @param connection The database connection to use.
	 * @param metadata   The file usage metadata to insert.
	 * @return The inserted file usage metadata.
	 * @throws SQLException If an SQL error occurs.
	 */
	public FileUsageMetadata insert(Connection connection, FileUsageMetadata metadata) throws SQLException {
		Objects.requireNonNull(connection, "connection must not be null");
		Objects.requireNonNull(metadata, "metadata must not be null");

		try (PreparedStatement statement = connection.prepareStatement(
				INSERT_SQL,
				Statement.RETURN_GENERATED_KEYS)) {

			statement.setString(1, metadata.uuid().toString());
			statement.setLong(2, metadata.physicalFileId());
			statement.setString(3, metadata.usageType());

			statement.setString(4, metadata.ownerType());
			statement.setString(5, metadata.ownerId());

			statement.setString(6, metadata.displayName());
			statement.setString(7, metadata.metadataJson());

			statement.setInt(8, metadata.status().getCode());
			statement.setLong(9, metadata.createdAt());
			JdbcValueUtils.setLongOrNull(statement, 10, metadata.deletedAt());

			statement.executeUpdate();

			try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
				if (!generatedKeys.next()) {
					throw new SQLException("Failed to retrieve generated file usage id");
				}

				long id = generatedKeys.getLong(1);

				return FileUsageMetadata.at(
						id,
						metadata.uuid(),
						metadata.physicalFileId(),
						metadata.usageType(),
						metadata.ownerType(),
						metadata.ownerId(),
						metadata.displayName(),
						metadata.metadataJson(),
						metadata.status(),
						metadata.createdAt(),
						metadata.deletedAt());
			}

		}

	}

	/**
	 * Finds a file usage metadata by ID.
	 *
	 * @param connection The database connection to use.
	 * @param id         The ID of the file usage metadata to find.
	 * @return The file usage metadata if found, or an empty optional otherwise.
	 * @throws SQLException If an SQL error occurs.
	 */
	public Optional<FileUsageMetadata> findById(Connection connection, long id) throws SQLException {
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
	 * Finds a file usage metadata by UUID.
	 *
	 * @param connection The database connection to use.
	 * @param uuid       The UUID of the file usage metadata to find.
	 * @return The file usage metadata if found, or an empty optional otherwise.
	 * @throws SQLException If an SQL error occurs.
	 */
	public Optional<FileUsageMetadata> findByUuid(Connection connection, UUID uuid) throws SQLException {
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
	 * Maps the current result set row to file usage metadata.
	 *
	 * @param resultSet result set positioned on a valid row
	 * @return mapped file usage metadata
	 * @throws SQLException if a column cannot be read
	 */
	private FileUsageMetadata mapRow(ResultSet resultSet) throws SQLException {
		return FileUsageMetadata.at(
				resultSet.getLong("id"),
				UUID.fromString(resultSet.getString("uuid")),
				resultSet.getLong("physical_file_id"),
				resultSet.getString("usage_type"),
				resultSet.getString("owner_type"),
				resultSet.getString("owner_id"),
				resultSet.getString("display_name"),
				resultSet.getString("metadata_json"),
				FileUsageStatus.fromCode(resultSet.getInt("status")),
				resultSet.getLong("created_at"),
				JdbcValueUtils.getLongOrNull(resultSet, "deleted_at"));
	}

}
