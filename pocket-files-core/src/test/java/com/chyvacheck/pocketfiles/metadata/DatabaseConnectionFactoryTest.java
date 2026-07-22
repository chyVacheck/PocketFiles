package com.chyvacheck.pocketfiles.metadata;

import com.chyvacheck.pocketfiles.config.PocketFilesConfig;
import com.chyvacheck.pocketfiles.storage.StorageDirectories;
import com.chyvacheck.pocketfiles.storage.StorageInitializer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseConnectionFactoryTest {

	@TempDir
	Path tempDir;

	@Test
	void shouldThrowExceptionWhenStorageDirectoriesIsNull() {
		// Act + Assert
		NullPointerException exception = assertThrows(
				NullPointerException.class,
				() -> new DatabaseConnectionFactory(null));

		// Assert
		assertEquals("storageDirectories must not be null", exception.getMessage());
	}

	@Test
	void shouldCreateConnection() throws IOException, SQLException {
		// Arrange
		StorageDirectories storageDirectories = this.createInitializedStorageDirectories();
		DatabaseConnectionFactory databaseConnectionFactory = new DatabaseConnectionFactory(storageDirectories);

		// Act
		try (Connection connection = databaseConnectionFactory.createConnection()) {
			// Assert
			assertFalse(connection.isClosed());
		}
	}

	@Test
	void shouldCreateDatabaseFile() throws IOException, SQLException {
		// Arrange
		StorageDirectories storageDirectories = this.createInitializedStorageDirectories();
		DatabaseConnectionFactory databaseConnectionFactory = new DatabaseConnectionFactory(storageDirectories);

		assertFalse(Files.exists(storageDirectories.getDatabasePath()));

		// Act
		try (Connection connection = databaseConnectionFactory.createConnection()) {
			assertFalse(connection.isClosed());
		}

		// Assert
		assertTrue(Files.exists(storageDirectories.getDatabasePath()));
		assertTrue(Files.isRegularFile(storageDirectories.getDatabasePath()));
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