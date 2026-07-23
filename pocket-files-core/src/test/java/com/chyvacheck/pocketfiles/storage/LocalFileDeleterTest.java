package com.chyvacheck.pocketfiles.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalFileDeleterTest {

	private static final UUID FILE_UUID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

	private static final String ORIGINAL_NAME = "photo.png";

	private static final String RELATIVE_PATH = "2026/01/02/550e8400-e29b-41d4-a716-446655440000.png";

	private static final String EXTENSION = "png";

	private static final long SIZE_BYTES = 5L;

	private static final String SHA256 = "185f8db32271fe25f561a6fc938b2e264306ec304eda518007d1764826381969";

	private static final String CONTENT = "Hello";

	@TempDir
	Path tempDir;

	// ? deleteIfExists StoredFile

	@Test
	void shouldThrowExceptionWhenStoredFileIsNull() {
		LocalFileDeleter localFileDeleter = new LocalFileDeleter();

		NullPointerException exception = assertThrows(
				NullPointerException.class,
				() -> localFileDeleter.deleteIfExists((StoredFile) null));

		assertEquals("storedFile must not be null", exception.getMessage());
	}

	@Test
	void shouldDeleteStoredFileIfExists() throws IOException {
		LocalFileDeleter localFileDeleter = new LocalFileDeleter();
		Path filePath = this.createFile("stored-file.txt");

		StoredFile storedFile = this.createStoredFile(filePath);

		assertTrue(Files.exists(filePath));

		localFileDeleter.deleteIfExists(storedFile);

		assertFalse(Files.exists(filePath));
	}

	@Test
	void shouldNotFailWhenStoredFileDoesNotExist() throws IOException {
		LocalFileDeleter localFileDeleter = new LocalFileDeleter();
		Path filePath = this.tempDir.resolve("missing-stored-file.txt");

		StoredFile storedFile = this.createStoredFile(filePath);

		localFileDeleter.deleteIfExists(storedFile);

		assertFalse(Files.exists(filePath));
	}

	// ? deleteIfExists StagedFile

	@Test
	void shouldThrowExceptionWhenStagedFileIsNull() {
		LocalFileDeleter localFileDeleter = new LocalFileDeleter();

		NullPointerException exception = assertThrows(
				NullPointerException.class,
				() -> localFileDeleter.deleteIfExists((StagedFile) null));

		assertEquals("stagedFile must not be null", exception.getMessage());
	}

	@Test
	void shouldDeleteStagedFileIfExists() throws IOException {
		LocalFileDeleter localFileDeleter = new LocalFileDeleter();
		Path filePath = this.createFile("staged-file.tmp");

		StagedFile stagedFile = this.createStagedFile(filePath);

		assertTrue(Files.exists(filePath));

		localFileDeleter.deleteIfExists(stagedFile);

		assertFalse(Files.exists(filePath));
	}

	@Test
	void shouldNotFailWhenStagedFileDoesNotExist() throws IOException {
		LocalFileDeleter localFileDeleter = new LocalFileDeleter();
		Path filePath = this.tempDir.resolve("missing-staged-file.tmp");

		StagedFile stagedFile = this.createStagedFile(filePath);

		localFileDeleter.deleteIfExists(stagedFile);

		assertFalse(Files.exists(filePath));
	}

	// ? helpers

	private Path createFile(String fileName) throws IOException {
		Path filePath = this.tempDir.resolve(fileName);

		Files.writeString(filePath, CONTENT);

		return filePath;
	}

	private StoredFile createStoredFile(Path absolutePath) {
		return StoredFile.at(
				FILE_UUID,
				ORIGINAL_NAME,
				RELATIVE_PATH,
				EXTENSION,
				SIZE_BYTES,
				SHA256,
				absolutePath);
	}

	private StagedFile createStagedFile(Path tempFilePath) {
		return StagedFile.at(
				ORIGINAL_NAME,
				EXTENSION,
				SIZE_BYTES,
				SHA256,
				tempFilePath);
	}
}
