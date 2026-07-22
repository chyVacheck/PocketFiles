package com.chyvacheck.pocketfiles;

import com.chyvacheck.pocketfiles.config.DirectoryDepth;
import com.chyvacheck.pocketfiles.config.PocketFilesConfig;
import com.chyvacheck.pocketfiles.metadata.status.FileUsageStatus;
import com.chyvacheck.pocketfiles.metadata.status.PhysicalFileStatus;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PocketFilesTest {
	private static final Instant FIXED_INSTANT = Instant.parse("2026-01-02T03:15:00Z");

	private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);

	private static final long CREATED_AT = FIXED_INSTANT.toEpochMilli();

	private static final String ORIGINAL_NAME = "photo.PNG";

	private static final String CONTENT = "Hello";

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

	@Test
	void shouldSaveFileThroughFacade() throws IOException, SQLException {
		PocketFiles pocketFiles = PocketFiles.create(this.createConfig(), FIXED_CLOCK);

		SaveFileResult result;

		try (InputStream inputStream = this.createInputStream(CONTENT)) {
			SaveFileCommand command = SaveFileCommand.at(
					inputStream,
					ORIGINAL_NAME,
					MIME_TYPE,
					USAGE_TYPE,
					OWNER_TYPE,
					OWNER_ID,
					DISPLAY_NAME,
					METADATA_JSON);

			result = pocketFiles.save(command);
		}

		assertNotNull(result.storedFile());
		assertNotNull(result.physicalFileMetadata());
		assertNotNull(result.fileUsageMetadata());

		assertTrue(Files.exists(result.storedFile().absolutePath()));
		assertEquals(CONTENT, Files.readString(result.storedFile().absolutePath()));

		assertEquals(result.storedFile().uuid(), result.physicalFileMetadata().uuid());
		assertEquals("photo.PNG", result.physicalFileMetadata().originalName());
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
		assertEquals("photo.PNG", result.physicalFileMetadata().originalName());
		assertEquals("png", result.physicalFileMetadata().extension());
		assertEquals(HELLO_SHA256, result.physicalFileMetadata().sha256());
		assertTrue(Files.exists(result.storedFile().absolutePath()));
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
