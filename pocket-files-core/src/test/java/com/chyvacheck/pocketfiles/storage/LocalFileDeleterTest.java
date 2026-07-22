package com.chyvacheck.pocketfiles.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LocalFileDeleterTest {
	private static final UUID UUID_VALUE = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

	private static final String ORIGINAL_NAME = "photo.png";

	private static final String RELATIVE_PATH = "2026/01/02/550e8400-e29b-41d4-a716-446655440000.png";

	private static final String EXTENSION = "png";

	private static final long SIZE_BYTES = 5L;

	private static final String SHA256 = "185f8db32271fe25f561a6fc938b2e264306ec304eda518007d1764826381969";

	@TempDir
	Path tempDir;

	@Test
	void shouldThrowExceptionWhenStoredFileIsNull() {
		LocalFileDeleter localFileDeleter = new LocalFileDeleter();

		NullPointerException exception = assertThrows(
				NullPointerException.class,
				() -> localFileDeleter.deleteIfExists(null));

		assertEquals("storedFile must not be null", exception.getMessage());
	}

	@Test
	void shouldDeleteStoredFileWhenFileExists() throws IOException {
		LocalFileDeleter localFileDeleter = new LocalFileDeleter();

		Path filePath = this.createFile("photo.png", "Hello");
		StoredFile storedFile = this.createStoredFile(filePath);

		localFileDeleter.deleteIfExists(storedFile);

		assertFalse(Files.exists(filePath));
	}

	@Test
	void shouldNotFailWhenFileDoesNotExist() throws IOException {
		LocalFileDeleter localFileDeleter = new LocalFileDeleter();

		Path filePath = this.tempDir.resolve("missing.png");
		StoredFile storedFile = this.createStoredFile(filePath);

		localFileDeleter.deleteIfExists(storedFile);

		assertFalse(Files.exists(filePath));
	}

	/**
	 * Creates a file in the temporary directory with the given name and content.
	 *
	 * @param fileName file name
	 * @param content  file content
	 * @return file path
	 * @throws IOException if the file cannot be created
	 */
	private Path createFile(String fileName, String content) throws IOException {
		Path filePath = this.tempDir.resolve(fileName);

		Files.writeString(filePath, content, StandardCharsets.UTF_8);

		return filePath;
	}

	/**
	 * Creates a stored file with the given absolute path.
	 *
	 * @param absolutePath absolute path of the file
	 * @return stored file
	 */
	private StoredFile createStoredFile(Path absolutePath) {
		return StoredFile.at(
				UUID_VALUE,
				ORIGINAL_NAME,
				RELATIVE_PATH,
				EXTENSION,
				SIZE_BYTES,
				SHA256,
				absolutePath);
	}
}
