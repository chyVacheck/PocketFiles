package com.chyvacheck.pocketfiles;

import com.chyvacheck.pocketfiles.config.DirectoryDepth;
import com.chyvacheck.pocketfiles.config.PocketFilesConfig;
import com.chyvacheck.pocketfiles.metadata.model.FileUsageMetadata;
import com.chyvacheck.pocketfiles.metadata.status.FileUsageStatus;
import com.chyvacheck.pocketfiles.metadata.status.PhysicalFileStatus;
import com.chyvacheck.pocketfiles.service.OpenFileResult;
import com.chyvacheck.pocketfiles.service.SaveFileCommand;
import com.chyvacheck.pocketfiles.service.SaveFileResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PocketFilesTest {

	private static final Instant FIXED_INSTANT = Instant.parse("2026-01-02T03:15:00Z");

	private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);

	private static final long CREATED_AT = FIXED_INSTANT.toEpochMilli();

	private static final String ORIGINAL_NAME = "photo.PNG";

	private static final String DIFFERENT_ORIGINAL_NAME = "copy.PNG";

	private static final String CONTENT = "Hello";

	private static final String DIFFERENT_CONTENT = "World";

	private static final String MIME_TYPE = "image/png";

	private static final String USAGE_TYPE = "invoice_attachment";

	private static final String OWNER_TYPE = "invoice";

	private static final String OWNER_ID = "777";

	private static final String DISPLAY_NAME = "Invoice January.pdf";

	private static final String METADATA_JSON = """
			{"source":"upload-form","category":"invoice"}
			""";

	private static final String HELLO_SHA256 = "185f8db32271fe25f561a6fc938b2e264306ec304eda518007d1764826381969";

	@TempDir
	Path tempDir;

	// ? create

	@Test
	void shouldThrowExceptionWhenConfigIsNull() {
		NullPointerException exception = assertThrows(
				NullPointerException.class,
				() -> PocketFiles.create(null, FIXED_CLOCK));

		assertEquals("config must not be null", exception.getMessage());
	}

	@Test
	void shouldThrowExceptionWhenClockIsNull() {
		NullPointerException exception = assertThrows(
				NullPointerException.class,
				() -> PocketFiles.create(this.createConfig(), null));

		assertEquals("clock must not be null", exception.getMessage());
	}

	@Test
	void shouldCreateStorageDirectoriesAndMetadataDatabase() throws IOException, SQLException {
		PocketFiles.create(this.createConfig(), FIXED_CLOCK);

		Path baseDirectory = this.getBaseDirectory();

		assertTrue(Files.isDirectory(baseDirectory));
		assertTrue(Files.isDirectory(baseDirectory.resolve("files")));
		assertTrue(Files.isDirectory(baseDirectory.resolve(".tmp")));
		assertTrue(Files.isRegularFile(baseDirectory.resolve("metadata.db")));
	}

	// ? save

	@Test
	void shouldSaveFileThroughFacade() throws IOException, SQLException {
		PocketFiles pocketFiles = PocketFiles.create(this.createConfig(), FIXED_CLOCK);

		SaveFileResult result = this.saveFile(pocketFiles, CONTENT, ORIGINAL_NAME);

		assertNotNull(result.storedFile());
		assertNotNull(result.physicalFileMetadata());
		assertNotNull(result.fileUsageMetadata());

		assertTrue(Files.exists(result.storedFile().absolutePath()));
		assertEquals(CONTENT, Files.readString(result.storedFile().absolutePath()));

		assertEquals(result.storedFile().uuid(), result.physicalFileMetadata().uuid());
		assertEquals(ORIGINAL_NAME, result.physicalFileMetadata().originalName());
		assertEquals("png", result.physicalFileMetadata().extension());
		assertEquals(MIME_TYPE, result.physicalFileMetadata().mimeType());
		assertEquals(5L, result.physicalFileMetadata().sizeBytes());
		assertEquals(HELLO_SHA256, result.physicalFileMetadata().sha256());
		assertEquals(PhysicalFileStatus.ACTIVE, result.physicalFileMetadata().status());
		assertEquals(CREATED_AT, result.physicalFileMetadata().createdAt());
		assertEquals(CREATED_AT, result.physicalFileMetadata().statusChangedAt());

		assertNotNull(result.fileUsageMetadata().uuid());
		assertEquals(result.physicalFileMetadata().id(), result.fileUsageMetadata().physicalFileId());
		assertEquals(USAGE_TYPE, result.fileUsageMetadata().usageType());
		assertEquals(OWNER_TYPE, result.fileUsageMetadata().ownerType());
		assertEquals(OWNER_ID, result.fileUsageMetadata().ownerId());
		assertEquals(DISPLAY_NAME, result.fileUsageMetadata().displayName());
		assertEquals(METADATA_JSON.trim(), result.fileUsageMetadata().metadataJson());
		assertEquals(FileUsageStatus.ACTIVE, result.fileUsageMetadata().status());
		assertEquals(CREATED_AT, result.fileUsageMetadata().createdAt());
	}

	@Test
	void shouldSaveFileWithRequiredFieldsOnlyThroughFacade() throws IOException, SQLException {
		PocketFiles pocketFiles = PocketFiles.create(this.createConfig(), FIXED_CLOCK);

		SaveFileResult result;

		try (InputStream inputStream = this.createInputStream(CONTENT)) {
			SaveFileCommand command = SaveFileCommand.of(inputStream, ORIGINAL_NAME);

			result = pocketFiles.save(command);
		}

		assertEquals("default", result.fileUsageMetadata().usageType());
		assertNull(result.fileUsageMetadata().ownerType());
		assertNull(result.fileUsageMetadata().ownerId());
		assertNull(result.fileUsageMetadata().displayName());
		assertNull(result.fileUsageMetadata().metadataJson());

		assertEquals(ORIGINAL_NAME, result.physicalFileMetadata().originalName());
		assertEquals("png", result.physicalFileMetadata().extension());
		assertEquals(HELLO_SHA256, result.physicalFileMetadata().sha256());

		assertTrue(Files.exists(result.storedFile().absolutePath()));
	}

	@Test
	void shouldReuseExistingPhysicalFileWhenSavingSameContentThroughFacade() throws IOException, SQLException {
		PocketFiles pocketFiles = PocketFiles.create(this.createConfig(), FIXED_CLOCK);

		SaveFileResult firstResult = this.saveFile(pocketFiles, CONTENT, ORIGINAL_NAME);
		SaveFileResult secondResult = this.saveFile(pocketFiles, CONTENT, DIFFERENT_ORIGINAL_NAME);

		assertEquals(firstResult.physicalFileMetadata().id(), secondResult.physicalFileMetadata().id());
		assertEquals(firstResult.physicalFileMetadata().uuid(), secondResult.physicalFileMetadata().uuid());
		assertEquals(firstResult.physicalFileMetadata().relativePath(),
				secondResult.physicalFileMetadata().relativePath());
		assertEquals(firstResult.storedFile().absolutePath(), secondResult.storedFile().absolutePath());

		assertNotEquals(firstResult.fileUsageMetadata().id(), secondResult.fileUsageMetadata().id());
		assertNotEquals(firstResult.fileUsageMetadata().uuid(), secondResult.fileUsageMetadata().uuid());

		assertEquals(firstResult.physicalFileMetadata().id(), firstResult.fileUsageMetadata().physicalFileId());
		assertEquals(firstResult.physicalFileMetadata().id(), secondResult.fileUsageMetadata().physicalFileId());

		assertEquals(1L, this.countRegularFiles(this.getBaseDirectory().resolve("files")));
		assertEquals(0L, this.countRegularFiles(this.getBaseDirectory().resolve(".tmp")));
	}

	@Test
	void shouldCreateNewPhysicalFileWhenContentIsDifferentThroughFacade() throws IOException, SQLException {
		PocketFiles pocketFiles = PocketFiles.create(this.createConfig(), FIXED_CLOCK);

		SaveFileResult firstResult = this.saveFile(pocketFiles, CONTENT, ORIGINAL_NAME);
		SaveFileResult secondResult = this.saveFile(pocketFiles, DIFFERENT_CONTENT, ORIGINAL_NAME);

		assertNotEquals(firstResult.physicalFileMetadata().id(), secondResult.physicalFileMetadata().id());
		assertNotEquals(firstResult.physicalFileMetadata().uuid(), secondResult.physicalFileMetadata().uuid());
		assertNotEquals(firstResult.physicalFileMetadata().relativePath(),
				secondResult.physicalFileMetadata().relativePath());
		assertNotEquals(firstResult.physicalFileMetadata().sha256(), secondResult.physicalFileMetadata().sha256());

		assertNotEquals(firstResult.fileUsageMetadata().id(), secondResult.fileUsageMetadata().id());
		assertNotEquals(firstResult.fileUsageMetadata().uuid(), secondResult.fileUsageMetadata().uuid());

		assertEquals(2L, this.countRegularFiles(this.getBaseDirectory().resolve("files")));
		assertEquals(0L, this.countRegularFiles(this.getBaseDirectory().resolve(".tmp")));
	}

	// ? open

	@Test
	void shouldOpenFileThroughFacade() throws IOException, SQLException {
		PocketFiles pocketFiles = PocketFiles.create(this.createConfig(), FIXED_CLOCK);

		SaveFileResult saveResult;

		try (InputStream inputStream = this.createInputStream(CONTENT)) {
			SaveFileCommand command = SaveFileCommand.of(inputStream, ORIGINAL_NAME);

			saveResult = pocketFiles.save(command);
		}

		OpenFileResult openResult = pocketFiles.open(saveResult.fileUsageMetadata().uuid());

		assertEquals(saveResult.fileUsageMetadata(), openResult.fileUsageMetadata());
		assertEquals(saveResult.physicalFileMetadata(), openResult.physicalFileMetadata());
		assertEquals(saveResult.storedFile().absolutePath(), openResult.absolutePath());

		assertTrue(Files.isRegularFile(openResult.absolutePath()));
		assertEquals(CONTENT, Files.readString(openResult.absolutePath()));
	}

	@Test
	void shouldNotOpenDeletedFileUsageThroughFacade() throws IOException, SQLException {
		PocketFiles pocketFiles = PocketFiles.create(this.createConfig(), FIXED_CLOCK);

		SaveFileResult saveResult;

		try (InputStream inputStream = this.createInputStream(CONTENT)) {
			SaveFileCommand command = SaveFileCommand.of(inputStream, ORIGINAL_NAME);

			saveResult = pocketFiles.save(command);
		}

		pocketFiles.delete(saveResult.fileUsageMetadata().uuid());

		IllegalArgumentException exception = assertThrows(
				IllegalArgumentException.class,
				() -> pocketFiles.open(saveResult.fileUsageMetadata().uuid()));

		assertEquals(
				"File usage is not active: " + saveResult.fileUsageMetadata().uuid(),
				exception.getMessage());
	}

	@Test
	void shouldOpenRestoredFileUsageThroughFacade() throws IOException, SQLException {
		PocketFiles pocketFiles = PocketFiles.create(this.createConfig(), FIXED_CLOCK);

		SaveFileResult saveResult;

		try (InputStream inputStream = this.createInputStream(CONTENT)) {
			SaveFileCommand command = SaveFileCommand.of(inputStream, ORIGINAL_NAME);

			saveResult = pocketFiles.save(command);
		}

		pocketFiles.delete(saveResult.fileUsageMetadata().uuid());
		pocketFiles.restore(saveResult.fileUsageMetadata().uuid());

		OpenFileResult openResult = pocketFiles.open(saveResult.fileUsageMetadata().uuid());

		assertEquals(saveResult.fileUsageMetadata().uuid(), openResult.fileUsageMetadata().uuid());
		assertEquals(FileUsageStatus.ACTIVE, openResult.fileUsageMetadata().status());
		assertEquals(saveResult.storedFile().absolutePath(), openResult.absolutePath());
		assertTrue(Files.isRegularFile(openResult.absolutePath()));
		assertEquals(CONTENT, Files.readString(openResult.absolutePath()));
	}

	// ? delete

	@Test
	void shouldDeleteFileUsageThroughFacade() throws IOException, SQLException {
		PocketFiles pocketFiles = PocketFiles.create(this.createConfig(), FIXED_CLOCK);

		SaveFileResult saveResult;

		try (InputStream inputStream = this.createInputStream(CONTENT)) {
			SaveFileCommand command = SaveFileCommand.of(inputStream, ORIGINAL_NAME);

			saveResult = pocketFiles.save(command);
		}

		FileUsageMetadata deletedMetadata = pocketFiles.delete(
				saveResult.fileUsageMetadata().uuid());

		assertEquals(saveResult.fileUsageMetadata().id(), deletedMetadata.id());
		assertEquals(saveResult.fileUsageMetadata().uuid(), deletedMetadata.uuid());
		assertEquals(FileUsageStatus.DELETED, deletedMetadata.status());
		assertEquals(CREATED_AT, deletedMetadata.createdAt());
		assertEquals(CREATED_AT, deletedMetadata.deletedAt());
	}

	// ? restore

	@Test
	void shouldRestoreFileUsageThroughFacade() throws IOException, SQLException {
		PocketFiles pocketFiles = PocketFiles.create(this.createConfig(), FIXED_CLOCK);

		SaveFileResult saveResult;

		try (InputStream inputStream = this.createInputStream(CONTENT)) {
			SaveFileCommand command = SaveFileCommand.of(inputStream, ORIGINAL_NAME);

			saveResult = pocketFiles.save(command);
		}

		FileUsageMetadata deletedMetadata = pocketFiles.delete(
				saveResult.fileUsageMetadata().uuid());

		FileUsageMetadata restoredMetadata = pocketFiles.restore(
				deletedMetadata.uuid());

		assertEquals(deletedMetadata.id(), restoredMetadata.id());
		assertEquals(deletedMetadata.uuid(), restoredMetadata.uuid());
		assertEquals(deletedMetadata.physicalFileId(), restoredMetadata.physicalFileId());
		assertEquals(FileUsageStatus.ACTIVE, restoredMetadata.status());
		assertEquals(CREATED_AT, restoredMetadata.createdAt());
		assertNull(restoredMetadata.deletedAt());
	}

	// ? helpers

	private SaveFileResult saveFile(
			PocketFiles pocketFiles,
			String content,
			String originalName) throws IOException, SQLException {
		try (InputStream inputStream = this.createInputStream(content)) {
			SaveFileCommand command = SaveFileCommand.at(
					inputStream,
					originalName,
					MIME_TYPE,
					USAGE_TYPE,
					OWNER_TYPE,
					OWNER_ID,
					DISPLAY_NAME,
					METADATA_JSON);

			return pocketFiles.save(command);
		}
	}

	private long countRegularFiles(Path directory) throws IOException {
		if (!Files.exists(directory)) {
			return 0L;
		}

		try (Stream<Path> paths = Files.walk(directory)) {
			return paths
					.filter(Files::isRegularFile)
					.count();
		}
	}

	private PocketFilesConfig createConfig() {
		return PocketFilesConfig.builder()
				.baseDirectory(this.getBaseDirectory())
				.directoryDepth(DirectoryDepth.DAY)
				.build();
	}

	private Path getBaseDirectory() {
		return this.tempDir.resolve("pocket-files");
	}

	private InputStream createInputStream(String content) {
		return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
	}
}
