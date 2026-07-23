package com.chyvacheck.pocketfiles.metadata;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;

/**
 * Initializes the SQLite metadata schema used by PocketFiles.
 *
 * <p>
 * This class creates all required metadata tables and indexes if they do not
 * already exist. It does not create storage directories by itself, so the
 * storage
 * directory initialization should be completed before this initializer is used.
 */
public final class MetadataSchemaInitializer {

	private static final String CREATE_PHYSICAL_FILES_TABLE_SQL = """
			CREATE TABLE IF NOT EXISTS physical_files (
			    id INTEGER PRIMARY KEY AUTOINCREMENT,
			    uuid TEXT NOT NULL UNIQUE,

			    original_name TEXT NOT NULL,
			    relative_path TEXT NOT NULL UNIQUE,

			    mime_type TEXT,
			    extension TEXT,

			    size_bytes INTEGER NOT NULL,
			    sha256 TEXT NOT NULL,

			    status INTEGER NOT NULL,
			    created_at INTEGER NOT NULL,
			    status_changed_at INTEGER NOT NULL,
			    deleted_at INTEGER
			)
			""";

	private static final String CREATE_FILE_USAGES_TABLE_SQL = """
			CREATE TABLE IF NOT EXISTS file_usages (
			    id INTEGER PRIMARY KEY AUTOINCREMENT,
			    uuid TEXT NOT NULL UNIQUE,

			    physical_file_id INTEGER NOT NULL,

			    usage_type TEXT NOT NULL,
			    owner_type TEXT,
			    owner_id TEXT,

			    display_name TEXT,
			    metadata_json TEXT,

			    status INTEGER NOT NULL,
			    created_at INTEGER NOT NULL,
			    deleted_at INTEGER,

			    FOREIGN KEY (physical_file_id) REFERENCES physical_files(id)
			)
			""";

	// ? physical files indexes

	private static final String CREATE_PHYSICAL_FILES_SHA256_SIZE_BYTES_INDEX_SQL = """
			CREATE INDEX IF NOT EXISTS idx_physical_files_sha256_size_bytes
			ON physical_files (sha256, size_bytes)
			""";

	private static final String CREATE_PHYSICAL_FILES_EXTENSION_INDEX_SQL = """
			CREATE INDEX IF NOT EXISTS idx_physical_files_extension
			ON physical_files (extension)
			""";

	private static final String CREATE_PHYSICAL_FILES_MIME_TYPE_INDEX_SQL = """
			CREATE INDEX IF NOT EXISTS idx_physical_files_mime_type
			ON physical_files (mime_type)
			""";

	private static final String CREATE_PHYSICAL_FILES_STATUS_INDEX_SQL = """
			CREATE INDEX IF NOT EXISTS idx_physical_files_status
			ON physical_files (status)
			""";

	private static final String CREATE_PHYSICAL_FILES_CREATED_AT_INDEX_SQL = """
			CREATE INDEX IF NOT EXISTS idx_physical_files_created_at
			ON physical_files (created_at)
			""";

	private static final String CREATE_PHYSICAL_FILES_STATUS_CHANGED_AT_INDEX_SQL = """
			CREATE INDEX IF NOT EXISTS idx_physical_files_status_changed_at
			ON physical_files (status_changed_at)
			""";

	private static final String CREATE_PHYSICAL_FILES_DELETED_AT_INDEX_SQL = """
			CREATE INDEX IF NOT EXISTS idx_physical_files_deleted_at
			ON physical_files (deleted_at)
			""";

	// ? file usages indexes

	private static final String CREATE_FILE_USAGES_PHYSICAL_FILE_ID_INDEX_SQL = """
			CREATE INDEX IF NOT EXISTS idx_file_usages_physical_file_id
			ON file_usages (physical_file_id)
			""";

	private static final String CREATE_FILE_USAGES_USAGE_TYPE_INDEX_SQL = """
			CREATE INDEX IF NOT EXISTS idx_file_usages_usage_type
			ON file_usages (usage_type)
			""";

	private static final String CREATE_FILE_USAGES_OWNER_INDEX_SQL = """
			CREATE INDEX IF NOT EXISTS idx_file_usages_owner
			ON file_usages (owner_type, owner_id)
			""";

	private static final String CREATE_FILE_USAGES_STATUS_INDEX_SQL = """
			CREATE INDEX IF NOT EXISTS idx_file_usages_status
			ON file_usages (status)
			""";

	private static final String CREATE_FILE_USAGES_CREATED_AT_INDEX_SQL = """
			CREATE INDEX IF NOT EXISTS idx_file_usages_created_at
			ON file_usages (created_at)
			""";

	private static final String CREATE_FILE_USAGES_DELETED_AT_INDEX_SQL = """
			CREATE INDEX IF NOT EXISTS idx_file_usages_deleted_at
			ON file_usages (deleted_at)
			""";

	private final DatabaseConnectionFactory databaseConnectionFactory;

	/**
	 * Creates a new metadata schema initializer.
	 *
	 * @param databaseConnectionFactory factory used to open SQLite connections
	 * @throws NullPointerException if {@code databaseConnectionFactory} is null
	 */
	public MetadataSchemaInitializer(DatabaseConnectionFactory databaseConnectionFactory) {
		this.databaseConnectionFactory = Objects.requireNonNull(
				databaseConnectionFactory,
				"databaseConnectionFactory must not be null");
	}

	// ? methods

	/**
	 * Initializes all required metadata tables and indexes.
	 *
	 * <p>
	 * This method is safe to call multiple times because all SQL statements use
	 * {@code IF NOT EXISTS}.
	 *
	 * @throws SQLException if schema initialization fails
	 */
	public void initialize() throws SQLException {
		try (Connection connection = this.databaseConnectionFactory.createConnection()) {

			this.createPhysicalFilesTable(connection);
			this.createFileUsagesTable(connection);

			this.createPhysicalFilesIndexes(connection);
			this.createFileUsagesIndexes(connection);
		}
	}

	// ? create tables

	/**
	 * Creates the table that stores metadata about physical files on disk.
	 *
	 * @param connection active JDBC connection
	 * @throws SQLException if the table cannot be created
	 */
	private void createPhysicalFilesTable(Connection connection) throws SQLException {
		this.execute(connection, CREATE_PHYSICAL_FILES_TABLE_SQL);
	}

	/**
	 * Creates the table that stores logical usages of physical files.
	 *
	 * @param connection active JDBC connection
	 * @throws SQLException if the table cannot be created
	 */
	private void createFileUsagesTable(Connection connection) throws SQLException {
		this.execute(connection, CREATE_FILE_USAGES_TABLE_SQL);
	}

	// ? create indexes

	/**
	 * Creates indexes for the {@code physical_files} table.
	 *
	 * <p>
	 * These indexes are used to speed up searches by hash, extension, MIME type,
	 * status and timestamps.
	 *
	 * @param connection active JDBC connection
	 * @throws SQLException if one of the indexes cannot be created
	 */
	private void createPhysicalFilesIndexes(Connection connection) throws SQLException {
		this.execute(connection, CREATE_PHYSICAL_FILES_SHA256_SIZE_BYTES_INDEX_SQL);
		this.execute(connection, CREATE_PHYSICAL_FILES_EXTENSION_INDEX_SQL);
		this.execute(connection, CREATE_PHYSICAL_FILES_MIME_TYPE_INDEX_SQL);
		this.execute(connection, CREATE_PHYSICAL_FILES_STATUS_INDEX_SQL);
		this.execute(connection, CREATE_PHYSICAL_FILES_CREATED_AT_INDEX_SQL);
		this.execute(connection, CREATE_PHYSICAL_FILES_STATUS_CHANGED_AT_INDEX_SQL);
		this.execute(connection, CREATE_PHYSICAL_FILES_DELETED_AT_INDEX_SQL);
	}

	/**
	 * Creates indexes for the {@code file_usages} table.
	 *
	 * <p>
	 * These indexes are used to speed up searches by physical file, usage type,
	 * owner, status and timestamps.
	 *
	 * @param connection active JDBC connection
	 * @throws SQLException if one of the indexes cannot be created
	 */
	private void createFileUsagesIndexes(Connection connection) throws SQLException {
		this.execute(connection, CREATE_FILE_USAGES_PHYSICAL_FILE_ID_INDEX_SQL);
		this.execute(connection, CREATE_FILE_USAGES_USAGE_TYPE_INDEX_SQL);
		this.execute(connection, CREATE_FILE_USAGES_OWNER_INDEX_SQL);
		this.execute(connection, CREATE_FILE_USAGES_STATUS_INDEX_SQL);
		this.execute(connection, CREATE_FILE_USAGES_CREATED_AT_INDEX_SQL);
		this.execute(connection, CREATE_FILE_USAGES_DELETED_AT_INDEX_SQL);
	}

	// ? helpers

	/**
	 * Executes a SQL statement that does not return a result set.
	 *
	 * <p>
	 * This method is used for schema-related commands such as {@code PRAGMA},
	 * {@code CREATE TABLE} and {@code CREATE INDEX}.
	 *
	 * @param connection active JDBC connection
	 * @param sql        SQL statement to execute
	 * @throws SQLException if the SQL statement cannot be executed
	 */
	private void execute(Connection connection, String sql) throws SQLException {
		try (Statement statement = connection.createStatement()) {
			statement.executeUpdate(sql);
		}
	}
}
