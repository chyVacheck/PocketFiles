package com.chyvacheck.pocketfiles.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.chyvacheck.pocketfiles.config.PocketFilesConfig;

class TempFileWriterTest {

	@TempDir
	Path tempDir;

	@Test
	void shouldThrowExceptionWhenStorageDirectoriesIsNull() {
		// Act + Assert
		NullPointerException exception = assertThrows(
				NullPointerException.class,
				() -> new TempFileWriter(null));

		assertEquals("storageDirectories must not be null", exception.getMessage());
	}

	@Test
	void shouldThrowExceptionWhenInputStreamIsNull() throws IOException {
		// Assert
		StorageDirectories storageDirectories = createStorageDirectories();
		StorageInitializer storageInitializer = new StorageInitializer(storageDirectories);
		storageInitializer.initialize();

		TempFileWriter tempFileWriter = new TempFileWriter(storageDirectories);

		NullPointerException exception = assertThrows(
				NullPointerException.class,
				() -> tempFileWriter.write(null));

		assertEquals("inputStream must not be null", exception.getMessage());
	}

	@Test
	void shouldWriteInputStreamToTempFile() throws IOException {
		// Arrange
		StorageDirectories storageDirectories = createStorageDirectories();
		StorageInitializer storageInitializer = new StorageInitializer(storageDirectories);

		storageInitializer.initialize();

		TempFileWriter tempFileWriter = new TempFileWriter(storageDirectories);

		// Act
		Path tempFilePath;

		try (InputStream inputStream = new ByteArrayInputStream("Hello".getBytes(StandardCharsets.UTF_8))) {
			tempFilePath = tempFileWriter.write(inputStream);
		}

		String content = Files.readString(tempFilePath);

		// Assert
		assertTrue(Files.exists(tempFilePath));
		assertTrue(Files.isRegularFile(tempFilePath));
		assertTrue(tempFilePath.startsWith(storageDirectories.getTempDirectory()));
		assertEquals("Hello", content);
	}

	private StorageDirectories createStorageDirectories() {
		// Создаем конфигурацию, которая содержит настройки хранилища.
		PocketFilesConfig config = PocketFilesConfig.builder()
				.baseDirectory(tempDir.resolve("pocket-files"))
				.build();

		return new StorageDirectories(config);
	}
}
