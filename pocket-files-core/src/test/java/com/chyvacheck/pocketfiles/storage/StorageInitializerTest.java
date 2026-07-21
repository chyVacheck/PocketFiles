package com.chyvacheck.pocketfiles.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.chyvacheck.pocketfiles.config.PocketFilesConfig;

class StorageInitializerTest {

	@TempDir
	Path tempDir;

	@Test
	void shouldCreateBaseDirectory() throws IOException {
		// Arrange
		StorageDirectories storageDirectories = createStorageDirectories();
		StorageInitializer storageInitializer = new StorageInitializer(storageDirectories);

		// Act
		storageInitializer.initialize();

		// Assert
		this.assertDirectoryExists(storageDirectories.getBaseDirectory());
	}

	@Test
	void shouldCreateFilesDirectory() throws IOException {
		// Arrange
		StorageDirectories storageDirectories = createStorageDirectories();
		StorageInitializer storageInitializer = new StorageInitializer(storageDirectories);

		// Act
		storageInitializer.initialize();

		// Assert
		this.assertDirectoryExists(storageDirectories.getFilesDirectory());
	}

	@Test
	void shouldCreateTempDirectory() throws IOException {
		// Arrange
		StorageDirectories storageDirectories = createStorageDirectories();
		StorageInitializer storageInitializer = new StorageInitializer(storageDirectories);

		// Act
		storageInitializer.initialize();

		// Assert
		this.assertDirectoryExists(storageDirectories.getTempDirectory());
	}

	@Test
	void shouldNotFailWhenDirectoriesAlreadyExist() throws IOException {
		// Arrange
		StorageDirectories storageDirectories = createStorageDirectories();
		StorageInitializer storageInitializer = new StorageInitializer(storageDirectories);

		// Act
		storageInitializer.initialize();
		storageInitializer.initialize();

		// Assert
		this.assertDirectoryExists(storageDirectories.getBaseDirectory());
		this.assertDirectoryExists(storageDirectories.getFilesDirectory());
		this.assertDirectoryExists(storageDirectories.getTempDirectory());
	}

	@Test
	void shouldThrowExceptionWhenStorageDirectoriesIsNull() {
		// Act + Assert
		NullPointerException exception = assertThrows(
				NullPointerException.class,
				() -> new StorageInitializer(null));

		assertEquals("directories must not be null", exception.getMessage());
	}

	private StorageDirectories createStorageDirectories() {
		// Создаем конфигурацию, которая содержит настройки хранилища.
		PocketFilesConfig config = PocketFilesConfig.builder()
				.baseDirectory(tempDir.resolve("pocket-files"))
				.build();

		return new StorageDirectories(config);
	}

	private void assertDirectoryExists(Path directory) {
		assertTrue(Files.isDirectory(directory));
	}
}
