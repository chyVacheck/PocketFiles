package com.chyvacheck.pocketfiles.metadata;

import com.chyvacheck.pocketfiles.config.PocketFilesConfig;
import com.chyvacheck.pocketfiles.storage.StorageDirectories;
import com.chyvacheck.pocketfiles.storage.StorageInitializer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MetadataSchemaInitializerTest {

	@TempDir
	Path tempDir;

	/**
	 * Checks if an exception is thrown when the {@code databaseConnectionFactory}
	 * is null.
	 *
	 * @throws IOException if the storage directories cannot be initialized
	 */
	@Test
	void shouldThrowExceptionWhenDatabaseConnectionFactoryIsNull() {
		// Act + Assert
		NullPointerException exception = assertThrows(
				NullPointerException.class,
				() -> new MetadataSchemaInitializer(null));

		// Assert
		assertEquals("databaseConnectionFactory must not be null", exception.getMessage());
	}

	/**
	 * Checks if the {@code physical_files} table is created.
	 *
	 * @throws IOException  if the storage directories cannot be initialized
	 * @throws SQLException if the database cannot be initialized
	 */
	@Test
	void shouldCreatePhysicalFilesTable() throws IOException, SQLException {
		// Arrange
		DatabaseConnectionFactory databaseConnectionFactory = this.createDatabaseConnectionFactory();
		MetadataSchemaInitializer metadataSchemaInitializer = new MetadataSchemaInitializer(databaseConnectionFactory);

		// Act
		metadataSchemaInitializer.initialize();

		// Assert
		assertTrue(this.tableExists(databaseConnectionFactory, "physical_files"));
	}

	/**
	 * Checks if the {@code file_usages} table is created.
	 *
	 * @throws IOException  if the storage directories cannot be initialized
	 * @throws SQLException if the database cannot be initialized
	 */
	@Test
	void shouldCreateFileUsagesTable() throws IOException, SQLException {
		// Arrange
		DatabaseConnectionFactory databaseConnectionFactory = this.createDatabaseConnectionFactory();
		MetadataSchemaInitializer metadataSchemaInitializer = new MetadataSchemaInitializer(databaseConnectionFactory);

		// Act
		metadataSchemaInitializer.initialize();

		// Assert
		assertTrue(this.tableExists(databaseConnectionFactory, "file_usages"));
	}

	/**
	 * Checks if the indexes for the {@code physical_files} table are created.
	 *
	 * @throws IOException  if the storage directories cannot be initialized
	 * @throws SQLException if the database cannot be initialized
	 */
	@Test
	void shouldCreatePhysicalFilesIndexes() throws IOException, SQLException {
		// Arrange
		DatabaseConnectionFactory databaseConnectionFactory = this.createDatabaseConnectionFactory();
		MetadataSchemaInitializer metadataSchemaInitializer = new MetadataSchemaInitializer(databaseConnectionFactory);

		// Act
		metadataSchemaInitializer.initialize();

		// Assert
		assertTrue(this.indexExists(databaseConnectionFactory, "idx_physical_files_sha256"));
		assertTrue(this.indexExists(databaseConnectionFactory, "idx_physical_files_extension"));
		assertTrue(this.indexExists(databaseConnectionFactory, "idx_physical_files_mime_type"));
		assertTrue(this.indexExists(databaseConnectionFactory, "idx_physical_files_status"));
		assertTrue(this.indexExists(databaseConnectionFactory, "idx_physical_files_created_at"));
		assertTrue(this.indexExists(databaseConnectionFactory, "idx_physical_files_status_changed_at"));
		assertTrue(this.indexExists(databaseConnectionFactory, "idx_physical_files_deleted_at"));
	}

	/**
	 * Checks if the indexes for the {@code file_usages} table are created.
	 *
	 * @throws IOException  if the storage directories cannot be initialized
	 * @throws SQLException if the database cannot be initialized
	 */
	@Test
	void shouldCreateFileUsagesIndexes() throws IOException, SQLException {
		// Arrange
		DatabaseConnectionFactory databaseConnectionFactory = this.createDatabaseConnectionFactory();
		MetadataSchemaInitializer metadataSchemaInitializer = new MetadataSchemaInitializer(databaseConnectionFactory);

		// Act
		metadataSchemaInitializer.initialize();

		// Assert
		assertTrue(this.indexExists(databaseConnectionFactory, "idx_file_usages_physical_file_id"));
		assertTrue(this.indexExists(databaseConnectionFactory, "idx_file_usages_usage_type"));
		assertTrue(this.indexExists(databaseConnectionFactory, "idx_file_usages_owner"));
		assertTrue(this.indexExists(databaseConnectionFactory, "idx_file_usages_status"));
		assertTrue(this.indexExists(databaseConnectionFactory, "idx_file_usages_created_at"));
		assertTrue(this.indexExists(databaseConnectionFactory, "idx_file_usages_deleted_at"));
	}

	/**
	 * Checks if the metadata schema initializer can be initialized multiple times.
	 *
	 * @throws IOException  if the storage directories cannot be initialized
	 * @throws SQLException if the database cannot be initialized
	 */
	@Test
	void shouldNotFailWhenInitializedTwice() throws IOException, SQLException {
		// Arrange
		DatabaseConnectionFactory databaseConnectionFactory = this.createDatabaseConnectionFactory();
		MetadataSchemaInitializer metadataSchemaInitializer = new MetadataSchemaInitializer(databaseConnectionFactory);

		// Act
		metadataSchemaInitializer.initialize();
		metadataSchemaInitializer.initialize();

		// Assert
		assertTrue(this.tableExists(databaseConnectionFactory, "physical_files"));
		assertTrue(this.tableExists(databaseConnectionFactory, "file_usages"));
	}

	/**
	 * Checks if foreign key checks are enabled for the database connection.
	 *
	 * @throws IOException  if the storage directories cannot be initialized
	 * @throws SQLException if the database cannot be initialized
	 */
	@Test
	void shouldEnableForeignKeysForConnection() throws IOException, SQLException {
		// Arrange
		DatabaseConnectionFactory databaseConnectionFactory = this.createDatabaseConnectionFactory();
		MetadataSchemaInitializer metadataSchemaInitializer = new MetadataSchemaInitializer(databaseConnectionFactory);

		metadataSchemaInitializer.initialize();

		String sql = """
				INSERT INTO file_usages (
				    uuid,
				    physical_file_id,
				    usage_type,
				    status,
				    created_at,
				    deleted_at
				) VALUES (
				    '550e8400-e29b-41d4-a716-446655440000',
				    999,
				    'default',
				    1,
				    1760000000000,
				    NULL
				)
				""";

		// Act + Assert
		assertThrows(
				SQLException.class,
				() -> this.execute(databaseConnectionFactory, sql));
	}

	private DatabaseConnectionFactory createDatabaseConnectionFactory() throws IOException {
		StorageDirectories storageDirectories = this.createInitializedStorageDirectories();

		return new DatabaseConnectionFactory(storageDirectories);
	}

	private StorageDirectories createInitializedStorageDirectories() throws IOException {
		PocketFilesConfig config = PocketFilesConfig.builder()
				.baseDirectory(this.tempDir.resolve("pocket-files"))
				.build();

		StorageDirectories storageDirectories = new StorageDirectories(config);

		StorageInitializer storageInitializer = new StorageInitializer(storageDirectories);
		storageInitializer.initialize();

		return storageDirectories;
	}

	/**
	 * Checks if the given table exists in the database.
	 *
	 * @param databaseConnectionFactory database connection factory
	 * @param tableName                 name of the table to check
	 * @return true if the table exists, false otherwise
	 * @throws SQLException if the statement cannot be executed
	 */
	private boolean tableExists(DatabaseConnectionFactory databaseConnectionFactory, String tableName)
			throws SQLException {
		String sql = """
				SELECT name
				FROM sqlite_master
				WHERE type = 'table'
				  AND name = '%s'
				""".formatted(tableName);

		return this.exists(databaseConnectionFactory, sql);
	}

	/**
	 * Checks if the given index exists in the database.
	 *
	 * @param databaseConnectionFactory database connection factory
	 * @param indexName                 name of the index to check
	 * @return true if the index exists, false otherwise
	 * @throws SQLException if the statement cannot be executed
	 */
	private boolean indexExists(DatabaseConnectionFactory databaseConnectionFactory, String indexName)
			throws SQLException {
		String sql = """
				SELECT name
				FROM sqlite_master
				WHERE type = 'index'
				  AND name = '%s'
				""".formatted(indexName);

		return this.exists(databaseConnectionFactory, sql);
	}

	/**
	 * Checks if the given SQL statement returns any rows.
	 *
	 * @param databaseConnectionFactory database connection factory
	 * @param sql                       SQL statement to execute
	 * @return true if the statement returns any rows, false otherwise
	 * @throws SQLException if the statement cannot be executed
	 */
	private boolean exists(DatabaseConnectionFactory databaseConnectionFactory, String sql) throws SQLException {
		try (
				Connection connection = databaseConnectionFactory.createConnection();
				Statement statement = connection.createStatement();
				ResultSet resultSet = statement.executeQuery(sql)) {
			return resultSet.next();
		}
	}

	/**
	 * Executes the given SQL statement on the database connection.
	 *
	 * @param databaseConnectionFactory database connection factory
	 * @param sql                       SQL statement to execute
	 * @throws SQLException if the statement cannot be executed
	 */
	private void execute(DatabaseConnectionFactory databaseConnectionFactory, String sql) throws SQLException {
		try (
				Connection connection = databaseConnectionFactory.createConnection();
				Statement statement = connection.createStatement()) {
			statement.executeUpdate(sql);
		}
	}
}