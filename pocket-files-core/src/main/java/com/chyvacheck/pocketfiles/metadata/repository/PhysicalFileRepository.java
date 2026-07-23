package com.chyvacheck.pocketfiles.metadata.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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

	/**
	 * SQL statement to find a file usage metadata by SHA-256 and size bytes.
	 */
	private static final String FIND_BY_SHA256_AND_SIZE_BYTES_SQL = """
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
			WHERE sha256 = ?
			  AND size_bytes = ?
			  AND status IN (?, ?)
			ORDER BY created_at ASC
			LIMIT 1
			""";

	/**
	 * The SQL statement to mark a physical file as active.
	 */
	private static final String MARK_ACTIVE_SQL = """
			UPDATE physical_files
			SET
				status = ?,
				status_changed_at = ?,
				deleted_at = NULL
			WHERE id = ?
			""";

	/**
	 * The SQL statement to mark a physical file as orphaned.
	 */
	private static final String MARK_ORPHANED_SQL = """
			UPDATE physical_files
			SET
			    status = ?,
			    status_changed_at = ?,
			    deleted_at = NULL
			WHERE id = ?
			""";

	/**
	 * The SQL statement to mark a physical file as deleted.
	 */
	private static final String MARK_DELETED_SQL = """
			UPDATE physical_files
			SET
			    status = ?,
			    status_changed_at = ?,
			    deleted_at = ?
			WHERE id = ?
			""";

	// ? methods

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
			JdbcValueUtils.setLongOrNull(statement, 11, metadata.deletedAt());

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

	/**
	 * Finds a file usage metadata by SHA-256 and size bytes.
	 *
	 * @param connection The database connection to use.
	 * @param sha256     The SHA-256 hash of the file to find.
	 * @param sizeBytes  The size of the file to find.
	 * @return The file usage metadata if found, or an empty optional otherwise.
	 * @throws SQLException If an SQL error occurs.
	 */
	public Optional<PhysicalFileMetadata> findBySha256AndSizeBytes(
			Connection connection,
			String sha256,
			long sizeBytes) throws SQLException {
		Objects.requireNonNull(connection, "connection must not be null");
		Objects.requireNonNull(sha256, "sha256 must not be null");

		String trimmedSha256 = sha256.trim();

		if (trimmedSha256.isBlank()) {
			throw new IllegalArgumentException("sha256 must not be blank");
		}

		if (sizeBytes < 0) {
			throw new IllegalArgumentException("sizeBytes must not be negative");
		}

		try (PreparedStatement statement = connection.prepareStatement(FIND_BY_SHA256_AND_SIZE_BYTES_SQL)) {
			statement.setString(1, trimmedSha256);
			statement.setLong(2, sizeBytes);
			statement.setInt(3, PhysicalFileStatus.ACTIVE.getCode());
			statement.setInt(4, PhysicalFileStatus.ORPHANED.getCode());

			try (ResultSet resultSet = statement.executeQuery()) {
				if (!resultSet.next()) {
					return Optional.empty();
				}

				return Optional.of(this.mapRow(resultSet));
			}
		}
	}

	/**
	 * Marks a physical file as active.
	 *
	 * @param connection      database connection to use
	 * @param id              database ID of the physical file metadata
	 * @param statusChangedAt the timestamp when the status changed
	 * @return the updated physical file metadata
	 * @throws SQLException if an SQL error occurs
	 */
	public PhysicalFileMetadata markActive(
			Connection connection,
			long id,
			long statusChangedAt) throws SQLException {
		Objects.requireNonNull(connection, "connection must not be null");

		if (id <= 0) {
			throw new IllegalArgumentException("id must be positive");
		}

		if (statusChangedAt < 0) {
			throw new IllegalArgumentException("statusChangedAt must not be negative");
		}

		try (PreparedStatement statement = connection.prepareStatement(MARK_ACTIVE_SQL)) {
			statement.setInt(1, PhysicalFileStatus.ACTIVE.getCode());
			statement.setLong(2, statusChangedAt);
			statement.setLong(3, id);

			statement.executeUpdate();
		}

		return this.findById(connection, id)
				.orElseThrow(() -> new SQLException("Failed to find active physical file: " + id));
	}

	/**
	 * Marks a physical file as orphaned.
	 *
	 * @param connection      database connection to use
	 * @param id              database ID of the physical file metadata
	 * @param statusChangedAt the timestamp when the status changed
	 * @return the updated physical file metadata
	 * @throws SQLException if an SQL error occurs
	 */
	public PhysicalFileMetadata markOrphaned(
			Connection connection,
			long id,
			long statusChangedAt) throws SQLException {
		Objects.requireNonNull(connection, "connection must not be null");

		if (id <= 0) {
			throw new IllegalArgumentException("id must be positive");
		}

		if (statusChangedAt < 0) {
			throw new IllegalArgumentException("statusChangedAt must not be negative");
		}

		try (PreparedStatement statement = connection.prepareStatement(MARK_ORPHANED_SQL)) {
			statement.setInt(1, PhysicalFileStatus.ORPHANED.getCode());
			statement.setLong(2, statusChangedAt);
			statement.setLong(3, id);

			statement.executeUpdate();
		}

		return this.findById(connection, id)
				.orElseThrow(() -> new SQLException("Failed to find orphaned physical file: " + id));
	}

	/**
	 * Marks a physical file as deleted.
	 *
	 * @param connection database connection to use
	 * @param id         database ID of the physical file metadata
	 * @param deletedAt  the timestamp when the file was deleted
	 * @return the updated physical file metadata
	 * @throws SQLException if an SQL error occurs
	 */
	public PhysicalFileMetadata markDeleted(
			Connection connection,
			long id,
			long deletedAt) throws SQLException {
		Objects.requireNonNull(connection, "connection must not be null");

		if (id <= 0) {
			throw new IllegalArgumentException("id must be positive");
		}

		if (deletedAt < 0) {
			throw new IllegalArgumentException("deletedAt must not be negative");
		}

		try (PreparedStatement statement = connection.prepareStatement(MARK_DELETED_SQL)) {
			statement.setInt(1, PhysicalFileStatus.DELETED.getCode());
			statement.setLong(2, deletedAt);
			statement.setLong(3, deletedAt);
			statement.setLong(4, id);

			statement.executeUpdate();
		}

		return this.findById(connection, id)
				.orElseThrow(() -> new SQLException("Failed to find deleted physical file: " + id));
	}

	// ? helpers

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
				JdbcValueUtils.getLongOrNull(resultSet, "deleted_at"));
	}

}
