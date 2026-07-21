package com.chyvacheck.pocketfiles.storage;

import com.chyvacheck.pocketfiles.config.DirectoryDepth;
import com.chyvacheck.pocketfiles.config.PocketFilesConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalFileStorageTest {
	private static final LocalDateTime DATE_TIME = LocalDateTime.of(2026, 1, 2, 3, 15);

	private static final String ORIGINAL_NAME = "photo.PNG";

	private static final String CONTENT = "Hello";

	private static final String HELLO_SHA256 = "185f8db32271fe25f561a6fc938b2e264306ec304eda518007d1764826381969";

	@TempDir
	Path tempDir;

	@Test
	void shouldThrowExceptionWhenTempFileWriterIsNull() {
		// Act + Assert
		NullPointerException exception = assertThrows(
				NullPointerException.class,
				() -> new LocalFileStorage(
						null,
						this.createSha256Calculator(),
						this.createLocalPathStrategy(),
						this.createFinalFileMover()));

		// Assert
		assertEquals("tempFileWriter must not be null", exception.getMessage());
	}

	@Test
	void shouldThrowExceptionWhenSha256CalculatorIsNull() {
		// Act + Assert
		NullPointerException exception = assertThrows(
				NullPointerException.class,
				() -> new LocalFileStorage(
						this.createTempFileWriter(),
						null,
						this.createLocalPathStrategy(),
						this.createFinalFileMover()));

		// Assert
		assertEquals("sha256Calculator must not be null", exception.getMessage());
	}

	@Test
	void shouldThrowExceptionWhenLocalPathStrategyIsNull() {
		// Act + Assert
		NullPointerException exception = assertThrows(
				NullPointerException.class,
				() -> new LocalFileStorage(
						this.createTempFileWriter(),
						this.createSha256Calculator(),
						null,
						this.createFinalFileMover()));

		// Assert
		assertEquals("localPathStrategy must not be null", exception.getMessage());
	}

	@Test
	void shouldThrowExceptionWhenFinalFileMoverIsNull() {
		// Act + Assert
		NullPointerException exception = assertThrows(
				NullPointerException.class,
				() -> new LocalFileStorage(
						this.createTempFileWriter(),
						this.createSha256Calculator(),
						this.createLocalPathStrategy(),
						null));

		// Assert
		assertEquals("finalFileMover must not be null", exception.getMessage());
	}

	@Test
	void shouldThrowExceptionWhenInputStreamIsNull() throws IOException {
		// Arrange
		LocalFileStorage localFileStorage = this.createInitializedLocalFileStorage();

		// Act + Assert
		NullPointerException exception = assertThrows(
				NullPointerException.class,
				() -> localFileStorage.save(null, ORIGINAL_NAME, DATE_TIME));

		// Assert
		assertEquals("inputStream must not be null", exception.getMessage());
	}

	@Test
	void shouldThrowExceptionWhenOriginalNameIsNull() throws IOException {
		// Arrange
		LocalFileStorage localFileStorage = this.createInitializedLocalFileStorage();

		try (InputStream inputStream = this.createInputStream(CONTENT)) {
			// Act + Assert
			NullPointerException exception = assertThrows(
					NullPointerException.class,
					() -> localFileStorage.save(inputStream, null, DATE_TIME));

			// Assert
			assertEquals("originalName must not be null", exception.getMessage());
		}
	}

	@Test
	void shouldThrowExceptionWhenOriginalNameIsBlank() throws IOException {
		// Arrange
		LocalFileStorage localFileStorage = this.createInitializedLocalFileStorage();

		try (InputStream inputStream = this.createInputStream(CONTENT)) {
			// Act + Assert
			IllegalArgumentException exception = assertThrows(
					IllegalArgumentException.class,
					() -> localFileStorage.save(inputStream, " ", DATE_TIME));

			// Assert
			assertEquals("originalName must not be blank", exception.getMessage());
		}
	}

	@Test
	void shouldThrowExceptionWhenDateTimeIsNull() throws IOException {
		// Arrange
		LocalFileStorage localFileStorage = this.createInitializedLocalFileStorage();

		try (InputStream inputStream = this.createInputStream(CONTENT)) {
			// Act + Assert
			NullPointerException exception = assertThrows(
					NullPointerException.class,
					() -> localFileStorage.save(inputStream, ORIGINAL_NAME, null));

			// Assert
			assertEquals("dateTime must not be null", exception.getMessage());
		}
	}

	@Test
	void shouldTrimOriginalName() throws IOException {
		// Arrange
		LocalFileStorage localFileStorage = this.createInitializedLocalFileStorage();

		StoredFile storedFile;

		try (InputStream inputStream = this.createInputStream(CONTENT)) {
			// Act
			storedFile = localFileStorage.save(inputStream, " photo.PNG ", DATE_TIME);
		}

		// Assert
		assertEquals("photo.PNG", storedFile.originalName());
	}

	@Test
	void shouldSaveFile() throws IOException {
		// Arrange
		LocalFileStorage localFileStorage = this.createInitializedLocalFileStorage();

		StoredFile storedFile;

		try (InputStream inputStream = this.createInputStream(CONTENT)) {
			// Act
			storedFile = localFileStorage.save(inputStream, ORIGINAL_NAME, DATE_TIME);
		}

		// Assert
		assertEquals(ORIGINAL_NAME, storedFile.originalName());
		assertEquals("png", storedFile.extension());
		assertEquals(5L, storedFile.sizeBytes());
		assertEquals(HELLO_SHA256, storedFile.sha256());

		assertTrue(storedFile.relativePath().startsWith("2026/01/02/"));
		assertTrue(storedFile.relativePath().endsWith(".png"));

		assertTrue(Files.exists(storedFile.absolutePath()));
		assertTrue(Files.isRegularFile(storedFile.absolutePath()));
		assertEquals(CONTENT, Files.readString(storedFile.absolutePath()));
	}

	private PocketFilesConfig createConfig() {
		return PocketFilesConfig.builder()
				.baseDirectory(this.tempDir.resolve("pocket-files"))
				.directoryDepth(DirectoryDepth.DAY)
				.build();
	}

	private StorageDirectories createStorageDirectories() {
		return new StorageDirectories(this.createConfig());
	}

	private TempFileWriter createTempFileWriter() {
		return new TempFileWriter(this.createStorageDirectories());
	}

	private Sha256Calculator createSha256Calculator() {
		return new Sha256Calculator();
	}

	private LocalPathStrategy createLocalPathStrategy() {
		return new LocalPathStrategy(this.createConfig());
	}

	private FinalFileMover createFinalFileMover() {
		return new FinalFileMover(this.createStorageDirectories());
	}

	private LocalFileStorage createInitializedLocalFileStorage() throws IOException {
		PocketFilesConfig config = this.createConfig();
		StorageDirectories storageDirectories = new StorageDirectories(config);

		StorageInitializer storageInitializer = new StorageInitializer(storageDirectories);
		storageInitializer.initialize();

		return new LocalFileStorage(
				new TempFileWriter(storageDirectories),
				new Sha256Calculator(),
				new LocalPathStrategy(config),
				new FinalFileMover(storageDirectories));
	}

	private InputStream createInputStream(String content) {
		return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
	}
}