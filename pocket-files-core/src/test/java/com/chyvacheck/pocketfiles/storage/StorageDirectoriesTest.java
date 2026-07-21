package com.chyvacheck.pocketfiles.storage;

import com.chyvacheck.pocketfiles.config.PocketFilesConfig;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StorageDirectoriesTest {

	private static final Path BASE_DIRECTORY = Path.of("/tmp/pocket-files");

	@Test
	void shouldReturnBaseDirectory() {
		// Arrange
		StorageDirectories storageDirectories = createStorageDirectories();

		// Assert
		assertEquals(BASE_DIRECTORY, storageDirectories.getBaseDirectory());
	}

	@Test
	void shouldReturnFilesDirectory() {
		// Arrange
		StorageDirectories storageDirectories = createStorageDirectories();

		// Assert
		assertEquals(BASE_DIRECTORY.resolve("files"), storageDirectories.getFilesDirectory());
	}

	@Test
	void shouldReturnTempDirectory() {
		// Arrange
		StorageDirectories storageDirectories = createStorageDirectories();

		// Assert
		assertEquals(BASE_DIRECTORY.resolve(".tmp"), storageDirectories.getTempDirectory());
	}

	@Test
	void shouldReturnDatabasePath() {
		// Arrange
		StorageDirectories storageDirectories = createStorageDirectories();

		// Assert
		assertEquals(BASE_DIRECTORY.resolve("metadata.db"), storageDirectories.getDatabasePath());
	}

	@Test
	void shouldResolveRelativeFilePath() {
		// Arrange
		StorageDirectories storageDirectories = createStorageDirectories();

		// Act
		Path filePath = storageDirectories.resolveFilePath("2026/01/02/file.png");

		// Assert
		assertEquals(
				BASE_DIRECTORY.resolve("files/2026/01/02/file.png"),
				filePath);
	}

	@Test
	void shouldThrowExceptionWhenRelativePathIsNull() {
		// Arrange
		StorageDirectories storageDirectories = createStorageDirectories();

		// Act + Assert
		NullPointerException exception = assertThrows(
				NullPointerException.class,
				() -> storageDirectories.resolveFilePath(null));

		assertEquals("relativePath must not be null", exception.getMessage());
	}

	@Test
	void shouldThrowExceptionWhenRelativePathEscapesFilesDirectory() {
		// Arrange
		StorageDirectories storageDirectories = createStorageDirectories();

		// Act + Assert
		IllegalArgumentException exception = assertThrows(
				IllegalArgumentException.class,
				() -> storageDirectories.resolveFilePath("../../etc/passwd"));

		assertEquals("relativePath escapes files directory", exception.getMessage());
	}

	private StorageDirectories createStorageDirectories() {
		// Создаем конфигурацию, которая содержит настройки хранилища.
		PocketFilesConfig config = PocketFilesConfig.builder()
				.baseDirectory(BASE_DIRECTORY)
				.build();

		return new StorageDirectories(config);
	}
}
