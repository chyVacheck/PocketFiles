package com.chyvacheck.pocketfiles.metadata.repository;

import com.chyvacheck.pocketfiles.config.PocketFilesConfig;
import com.chyvacheck.pocketfiles.metadata.DatabaseConnectionFactory;
import com.chyvacheck.pocketfiles.storage.StorageDirectories;
import com.chyvacheck.pocketfiles.storage.StorageInitializer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JdbcValueUtilsTest {
	@TempDir
	Path tempDir;

	@Test
	void shouldSetLongValue() throws IOException, SQLException {
		DatabaseConnectionFactory databaseConnectionFactory = this.createInitializedDatabaseConnectionFactory();

		try (Connection connection = databaseConnectionFactory.createConnection()) {
			this.createTestTable(connection);

			try (PreparedStatement statement = connection
					.prepareStatement("INSERT INTO test_values (value) VALUES (?)")) {
				JdbcValueUtils.setLongOrNull(statement, 1, 123L);

				statement.executeUpdate();
			}

			assertEquals(123L, this.readFirstValue(connection));
		}
	}

	@Test
	void shouldSetNullLongValue() throws IOException, SQLException {
		DatabaseConnectionFactory databaseConnectionFactory = this.createInitializedDatabaseConnectionFactory();

		try (Connection connection = databaseConnectionFactory.createConnection()) {
			this.createTestTable(connection);

			try (PreparedStatement statement = connection
					.prepareStatement("INSERT INTO test_values (value) VALUES (?)")) {
				JdbcValueUtils.setLongOrNull(statement, 1, null);

				statement.executeUpdate();
			}

			assertNull(this.readFirstValue(connection));
		}
	}

	@Test
	void shouldThrowExceptionWhenPreparedStatementIsNull() {
		NullPointerException exception = assertThrows(
				NullPointerException.class,
				() -> JdbcValueUtils.setLongOrNull(null, 1, 123L));

		assertEquals("statement must not be null", exception.getMessage());
	}

	@Test
	void shouldThrowExceptionWhenResultSetIsNull() {
		NullPointerException exception = assertThrows(
				NullPointerException.class,
				() -> JdbcValueUtils.getLongOrNull(null, "value"));

		assertEquals("resultSet must not be null", exception.getMessage());
	}

	@Test
	void shouldThrowExceptionWhenColumnNameIsNull() throws IOException, SQLException {
		DatabaseConnectionFactory databaseConnectionFactory = this.createInitializedDatabaseConnectionFactory();

		try (Connection connection = databaseConnectionFactory.createConnection()) {
			this.createTestTable(connection);

			try (Statement statement = connection.createStatement()) {
				statement.executeUpdate("INSERT INTO test_values (value) VALUES (123)");
			}

			try (
					Statement statement = connection.createStatement();
					ResultSet resultSet = statement.executeQuery("SELECT value FROM test_values")) {
				resultSet.next();

				NullPointerException exception = assertThrows(
						NullPointerException.class,
						() -> JdbcValueUtils.getLongOrNull(resultSet, null));

				assertEquals("columnName must not be null", exception.getMessage());
			}
		}
	}

	private void createTestTable(Connection connection) throws SQLException {
		try (Statement statement = connection.createStatement()) {
			statement.executeUpdate("""
					CREATE TABLE test_values (
					    value INTEGER
					)
					""");
		}
	}

	private Long readFirstValue(Connection connection) throws SQLException {
		try (
				Statement statement = connection.createStatement();
				ResultSet resultSet = statement.executeQuery("SELECT value FROM test_values LIMIT 1")) {
			resultSet.next();

			return JdbcValueUtils.getLongOrNull(resultSet, "value");
		}
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
}
