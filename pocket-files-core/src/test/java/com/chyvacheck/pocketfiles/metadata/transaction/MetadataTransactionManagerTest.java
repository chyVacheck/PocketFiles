package com.chyvacheck.pocketfiles.metadata.transaction;

import com.chyvacheck.pocketfiles.config.PocketFilesConfig;
import com.chyvacheck.pocketfiles.metadata.DatabaseConnectionFactory;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MetadataTransactionManagerTest {

	@TempDir
	Path tempDir;

	@Test
	void shouldThrowExceptionWhenDatabaseConnectionFactoryIsNull() {
		NullPointerException exception = assertThrows(
				NullPointerException.class,
				() -> new MetadataTransactionManager(null));

		assertEquals("databaseConnectionFactory must not be null", exception.getMessage());
	}

	@Test
	void shouldThrowExceptionWhenCallbackIsNull() throws IOException {
		MetadataTransactionManager transactionManager = this.createTransactionManager();

		NullPointerException exception = assertThrows(
				NullPointerException.class,
				() -> transactionManager.execute(null));

		assertEquals("callback must not be null", exception.getMessage());
	}

	@Test
	void shouldCommitTransaction() throws IOException, SQLException {
		DatabaseConnectionFactory databaseConnectionFactory = this.createInitializedDatabaseConnectionFactory();
		MetadataTransactionManager transactionManager = new MetadataTransactionManager(databaseConnectionFactory);

		try (Connection connection = databaseConnectionFactory.createConnection()) {
			this.createTestTable(connection);
		}

		String result = transactionManager.execute(connection -> {
			try (Statement statement = connection.createStatement()) {
				statement.executeUpdate("INSERT INTO test_values (value) VALUES ('committed')");
			}

			return "success";
		});

		assertEquals("success", result);

		try (Connection connection = databaseConnectionFactory.createConnection()) {
			assertEquals(1, this.countRows(connection));
			assertEquals("committed", this.readFirstValue(connection));
		}
	}

	@Test
	void shouldRollbackTransactionWhenSqlExceptionIsThrown() throws IOException, SQLException {
		DatabaseConnectionFactory databaseConnectionFactory = this.createInitializedDatabaseConnectionFactory();
		MetadataTransactionManager transactionManager = new MetadataTransactionManager(databaseConnectionFactory);

		try (Connection connection = databaseConnectionFactory.createConnection()) {
			this.createTestTable(connection);
		}

		SQLException exception = assertThrows(
				SQLException.class,
				() -> transactionManager.execute(connection -> {
					try (Statement statement = connection.createStatement()) {
						statement.executeUpdate("INSERT INTO test_values (value) VALUES ('rollback')");
					}

					throw new SQLException("Test SQL failure");
				}));

		assertEquals("Test SQL failure", exception.getMessage());

		try (Connection connection = databaseConnectionFactory.createConnection()) {
			assertEquals(0, this.countRows(connection));
		}
	}

	@Test
	void shouldRollbackTransactionWhenRuntimeExceptionIsThrown() throws IOException, SQLException {
		DatabaseConnectionFactory databaseConnectionFactory = this.createInitializedDatabaseConnectionFactory();
		MetadataTransactionManager transactionManager = new MetadataTransactionManager(databaseConnectionFactory);

		try (Connection connection = databaseConnectionFactory.createConnection()) {
			this.createTestTable(connection);
		}

		SQLException exception = assertThrows(
				SQLException.class,
				() -> transactionManager.execute(connection -> {
					try (Statement statement = connection.createStatement()) {
						statement.executeUpdate("INSERT INTO test_values (value) VALUES ('rollback')");
					}

					throw new IllegalStateException("Test runtime failure");
				}));

		assertEquals("Transaction failed", exception.getMessage());

		try (Connection connection = databaseConnectionFactory.createConnection()) {
			assertEquals(0, this.countRows(connection));
		}
	}

	@Test
	void shouldRestoreAutoCommitAfterTransaction() throws IOException, SQLException {
		DatabaseConnectionFactory databaseConnectionFactory = this.createInitializedDatabaseConnectionFactory();
		MetadataTransactionManager transactionManager = new MetadataTransactionManager(databaseConnectionFactory);

		try (Connection connection = databaseConnectionFactory.createConnection()) {
			this.createTestTable(connection);
		}

		transactionManager.execute(connection -> {
			assertFalse(connection.getAutoCommit());

			try (Statement statement = connection.createStatement()) {
				statement.executeUpdate("INSERT INTO test_values (value) VALUES ('committed')");
			}

			return null;
		});

		try (Connection connection = databaseConnectionFactory.createConnection()) {
			assertTrue(connection.getAutoCommit());
		}
	}

	private MetadataTransactionManager createTransactionManager() throws IOException {
		DatabaseConnectionFactory databaseConnectionFactory = this.createInitializedDatabaseConnectionFactory();

		return new MetadataTransactionManager(databaseConnectionFactory);
	}

	private DatabaseConnectionFactory createInitializedDatabaseConnectionFactory() throws IOException {
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

	private void createTestTable(Connection connection) throws SQLException {
		try (Statement statement = connection.createStatement()) {
			statement.executeUpdate("""
					CREATE TABLE test_values (
					    value TEXT NOT NULL
					)
					""");
		}
	}

	private int countRows(Connection connection) throws SQLException {
		try (
				Statement statement = connection.createStatement();
				ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM test_values")) {
			resultSet.next();

			return resultSet.getInt(1);
		}
	}

	private String readFirstValue(Connection connection) throws SQLException {
		try (
				Statement statement = connection.createStatement();
				ResultSet resultSet = statement.executeQuery("SELECT value FROM test_values LIMIT 1")) {
			resultSet.next();

			return resultSet.getString("value");
		}
	}
}
