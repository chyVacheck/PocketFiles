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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalFileStorageTest {

	private static final LocalDateTime DATE_TIME = LocalDateTime.of(2026, 1, 2, 3, 15);

	private static final String ORIGINAL_NAME = "photo.PNG";

	private static final String ORIGINAL_NAME_WITHOUT_EXTENSION = "README";

	private static final String CONTENT = "Hello";

	private static final String HELLO_SHA256 = "185f8db32271fe25f561a6fc938b2e264306ec304eda518007d1764826381969";

	@TempDir
	Path tempDir;

	// ? constructor

	@Test
	void shouldThrowExceptionWhenTempFileWriterIsNull() {
		NullPointerException exception = assertThrows(
				NullPointerException.class,
				() -> new LocalFileStorage(
						null,
						this.createSha256Calculator(),
						this.createLocalPathStrategy(),
						this.createFinalFileMover()));

		assertEquals("tempFileWriter must not be null", exception.getMessage());
	}

	@Test
	void shouldThrowExceptionWhenSha256CalculatorIsNull() {
		NullPointerException exception = assertThrows(
				NullPointerException.class,
				() -> new LocalFileStorage(
						this.createTempFileWriter(),
						null,
						this.createLocalPathStrategy(),
						this.createFinalFileMover()));

		assertEquals("sha256Calculator must not be null", exception.getMessage());
	}

	@Test
	void shouldThrowExceptionWhenLocalPathStrategyIsNull() {
		NullPointerException exception = assertThrows(
				NullPointerException.class,
				() -> new LocalFileStorage(
						this.createTempFileWriter(),
						this.createSha256Calculator(),
						null,
						this.createFinalFileMover()));

		assertEquals("localPathStrategy must not be null", exception.getMessage());
	}

	@Test
	void shouldThrowExceptionWhenFinalFileMoverIsNull() {
		NullPointerException exception = assertThrows(
				NullPointerException.class,
				() -> new LocalFileStorage(
						this.createTempFileWriter(),
						this.createSha256Calculator(),
						this.createLocalPathStrategy(),
						null));

		assertEquals("finalFileMover must not be null", exception.getMessage());
	}

	// ? stage

	@Test
	void shouldThrowExceptionWhenStageInputStreamIsNull() throws IOException {
		LocalFileStorage localFileStorage = this.createInitializedLocalFileStorage();

		NullPointerException exception = assertThrows(
				NullPointerException.class,
				() -> localFileStorage.stage(null, ORIGINAL_NAME));

		assertEquals("inputStream must not be null", exception.getMessage());
	}

	@Test
	void shouldThrowExceptionWhenStageOriginalNameIsNull() throws IOException {
		LocalFileStorage localFileStorage = this.createInitializedLocalFileStorage();

		try (InputStream inputStream = this.createInputStream(CONTENT)) {
			NullPointerException exception = assertThrows(
					NullPointerException.class,
					() -> localFileStorage.stage(inputStream, null));

			assertEquals("originalName must not be null", exception.getMessage());
		}
	}

	@Test
	void shouldThrowExceptionWhenStageOriginalNameIsBlank() throws IOException {
		LocalFileStorage localFileStorage = this.createInitializedLocalFileStorage();

		try (InputStream inputStream = this.createInputStream(CONTENT)) {
			IllegalArgumentException exception = assertThrows(
					IllegalArgumentException.class,
					() -> localFileStorage.stage(inputStream, "   "));

			assertEquals("originalName must not be blank", exception.getMessage());
		}
	}

	@Test
	void shouldStageFile() throws IOException {
		LocalFileStorage localFileStorage = this.createInitializedLocalFileStorage();

		StagedFile stagedFile;

		try (InputStream inputStream = this.createInputStream(CONTENT)) {
			stagedFile = localFileStorage.stage(inputStream, ORIGINAL_NAME);
		}

		assertEquals(ORIGINAL_NAME, stagedFile.originalName());
		assertEquals("png", stagedFile.extension());
		assertEquals(5L, stagedFile.sizeBytes());
		assertEquals(HELLO_SHA256, stagedFile.sha256());

		assertTrue(Files.exists(stagedFile.tempFilePath()));
		assertTrue(Files.isRegularFile(stagedFile.tempFilePath()));
		assertEquals(CONTENT, Files.readString(stagedFile.tempFilePath()));
	}

	@Test
	void shouldStageFileWithoutExtension() throws IOException {
		LocalFileStorage localFileStorage = this.createInitializedLocalFileStorage();

		StagedFile stagedFile;

		try (InputStream inputStream = this.createInputStream(CONTENT)) {
			stagedFile = localFileStorage.stage(inputStream, ORIGINAL_NAME_WITHOUT_EXTENSION);
		}

		assertEquals(ORIGINAL_NAME_WITHOUT_EXTENSION, stagedFile.originalName());
		assertNull(stagedFile.extension());
		assertEquals(5L, stagedFile.sizeBytes());
		assertEquals(HELLO_SHA256, stagedFile.sha256());

		assertTrue(Files.exists(stagedFile.tempFilePath()));
		assertTrue(Files.isRegularFile(stagedFile.tempFilePath()));
		assertEquals(CONTENT, Files.readString(stagedFile.tempFilePath()));
	}

	@Test
	void shouldTrimOriginalNameWhenStageFile() throws IOException {
		LocalFileStorage localFileStorage = this.createInitializedLocalFileStorage();

		StagedFile stagedFile;

		try (InputStream inputStream = this.createInputStream(CONTENT)) {
			stagedFile = localFileStorage.stage(inputStream, " photo.PNG ");
		}

		assertEquals(ORIGINAL_NAME, stagedFile.originalName());
		assertEquals("png", stagedFile.extension());
	}

	// ? store

	@Test
	void shouldThrowExceptionWhenStoreStagedFileIsNull() throws IOException {
		LocalFileStorage localFileStorage = this.createInitializedLocalFileStorage();

		NullPointerException exception = assertThrows(
				NullPointerException.class,
				() -> localFileStorage.store(null, DATE_TIME));

		assertEquals("stagedFile must not be null", exception.getMessage());
	}

	@Test
	void shouldThrowExceptionWhenStoreDateTimeIsNull() throws IOException {
		LocalFileStorage localFileStorage = this.createInitializedLocalFileStorage();
		StagedFile stagedFile = this.stageTestFile(localFileStorage);

		NullPointerException exception = assertThrows(
				NullPointerException.class,
				() -> localFileStorage.store(stagedFile, null));

		assertEquals("dateTime must not be null", exception.getMessage());
	}

	@Test
	void shouldStoreStagedFile() throws IOException {
		LocalFileStorage localFileStorage = this.createInitializedLocalFileStorage();
		StagedFile stagedFile = this.stageTestFile(localFileStorage);

		Path tempFilePath = stagedFile.tempFilePath();

		StoredFile storedFile = localFileStorage.store(stagedFile, DATE_TIME);

		assertEquals(ORIGINAL_NAME, storedFile.originalName());
		assertEquals("png", storedFile.extension());
		assertEquals(5L, storedFile.sizeBytes());
		assertEquals(HELLO_SHA256, storedFile.sha256());

		assertTrue(storedFile.relativePath().startsWith("2026/01/02/"));
		assertTrue(storedFile.relativePath().endsWith(".png"));

		assertFalse(Files.exists(tempFilePath));
		assertTrue(Files.exists(storedFile.absolutePath()));
		assertTrue(Files.isRegularFile(storedFile.absolutePath()));
		assertEquals(CONTENT, Files.readString(storedFile.absolutePath()));
	}

	// ? helpers

	private StagedFile stageTestFile(LocalFileStorage localFileStorage) throws IOException {
		try (InputStream inputStream = this.createInputStream(CONTENT)) {
			return localFileStorage.stage(inputStream, ORIGINAL_NAME);
		}
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
