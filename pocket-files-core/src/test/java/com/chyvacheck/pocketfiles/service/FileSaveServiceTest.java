package com.chyvacheck.pocketfiles.service;

import com.chyvacheck.pocketfiles.config.DirectoryDepth;
import com.chyvacheck.pocketfiles.config.PocketFilesConfig;
import com.chyvacheck.pocketfiles.metadata.DatabaseConnectionFactory;
import com.chyvacheck.pocketfiles.metadata.MetadataSchemaInitializer;
import com.chyvacheck.pocketfiles.metadata.model.FileUsageMetadata;
import com.chyvacheck.pocketfiles.metadata.model.PhysicalFileMetadata;
import com.chyvacheck.pocketfiles.metadata.repository.FileUsageRepository;
import com.chyvacheck.pocketfiles.metadata.repository.PhysicalFileRepository;
import com.chyvacheck.pocketfiles.metadata.status.FileUsageStatus;
import com.chyvacheck.pocketfiles.metadata.status.PhysicalFileStatus;
import com.chyvacheck.pocketfiles.metadata.transaction.MetadataTransactionManager;
import com.chyvacheck.pocketfiles.storage.FinalFileMover;
import com.chyvacheck.pocketfiles.storage.LocalFileDeleter;
import com.chyvacheck.pocketfiles.storage.LocalFileStorage;
import com.chyvacheck.pocketfiles.storage.LocalPathStrategy;
import com.chyvacheck.pocketfiles.storage.Sha256Calculator;
import com.chyvacheck.pocketfiles.storage.StorageDirectories;
import com.chyvacheck.pocketfiles.storage.StorageInitializer;
import com.chyvacheck.pocketfiles.storage.TempFileWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;

class FileSaveServiceTest {
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
	void shouldThrowExceptionWhenLocalFileStorageIsNull() {
		NullPointerException exception = assertThrows(
				NullPointerException.class,
				() -> new FileSaveService(
						null,
						new LocalFileDeleter(),
						this.createTransactionManager(),
						new PhysicalFileRepository(),
						new FileUsageRepository(),
						FIXED_CLOCK));

		assertEquals("localFileStorage must not be null", exception.getMessage());
	}

	@Test
	void shouldThrowExceptionWhenLocalFileDeleterIsNull() {
		NullPointerException exception = assertThrows(
				NullPointerException.class,
				() -> new FileSaveService(
						this.createLocalFileStorage(),
						null,
						this.createTransactionManager(),
						new PhysicalFileRepository(),
						new FileUsageRepository(),
						FIXED_CLOCK));

		assertEquals("localFileDeleter must not be null", exception.getMessage());
	}

	@Test
	void shouldThrowExceptionWhenTransactionManagerIsNull() {
		NullPointerException exception = assertThrows(
				NullPointerException.class,
				() -> new FileSaveService(
						this.createLocalFileStorage(),
						new LocalFileDeleter(),
						null,
						new PhysicalFileRepository(),
						new FileUsageRepository(),
						FIXED_CLOCK));

		assertEquals("transactionManager must not be null", exception.getMessage());
	}

	@Test
	void shouldThrowExceptionWhenPhysicalFileRepositoryIsNull() {
		NullPointerException exception = assertThrows(
				NullPointerException.class,
				() -> new FileSaveService(
						this.createLocalFileStorage(),
						new LocalFileDeleter(),
						this.createTransactionManager(),
						null,
						new FileUsageRepository(),
						FIXED_CLOCK));

		assertEquals("physicalFileRepository must not be null", exception.getMessage());
	}

	@Test
	void shouldThrowExceptionWhenFileUsageRepositoryIsNull() {
		NullPointerException exception = assertThrows(
				NullPointerException.class,
				() -> new FileSaveService(
						this.createLocalFileStorage(),
						new LocalFileDeleter(),
						this.createTransactionManager(),
						new PhysicalFileRepository(),
						null,
						FIXED_CLOCK));

		assertEquals("fileUsageRepository must not be null", exception.getMessage());
	}

	@Test
	void shouldThrowExceptionWhenClockIsNull() {
		NullPointerException exception = assertThrows(
				NullPointerException.class,
				() -> new FileSaveService(
						this.createLocalFileStorage(),
						new LocalFileDeleter(),
						this.createTransactionManager(),
						new PhysicalFileRepository(),
						new FileUsageRepository(),
						null));

		assertEquals("clock must not be null", exception.getMessage());
	}

	@Test
	void shouldThrowExceptionWhenCommandIsNull() throws IOException, SQLException {
		FileSaveService fileSaveService = this.createInitializedFileSaveService();

		NullPointerException exception = assertThrows(
				NullPointerException.class,
				() -> fileSaveService.save(null));

		assertEquals("command must not be null", exception.getMessage());
	}

	@Test
	void shouldSaveFile() throws IOException, SQLException {
		DatabaseConnectionFactory databaseConnectionFactory = this.createInitializedDatabaseConnectionFactory();
		FileSaveService fileSaveService = this.createFileSaveService(databaseConnectionFactory);

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

			result = fileSaveService.save(command);
		}

		assertNotNull(result.storedFile());
		assertNotNull(result.physicalFileMetadata());
		assertNotNull(result.fileUsageMetadata());

		assertTrue(Files.exists(result.storedFile().absolutePath()));
		assertEquals(CONTENT, Files.readString(result.storedFile().absolutePath()));

		assertEquals(result.storedFile().uuid(), result.physicalFileMetadata().uuid());
		assertEquals("photo.PNG", result.physicalFileMetadata().originalName());
		assertEquals(result.storedFile().relativePath(), result.physicalFileMetadata().relativePath());
		assertEquals(MIME_TYPE, result.physicalFileMetadata().mimeType());
		assertEquals("png", result.physicalFileMetadata().extension());
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

		try (Connection connection = databaseConnectionFactory.createConnection()) {
			PhysicalFileRepository physicalFileRepository = new PhysicalFileRepository();
			FileUsageRepository fileUsageRepository = new FileUsageRepository();

			Optional<PhysicalFileMetadata> foundPhysicalFile = physicalFileRepository.findByUuid(
					connection,
					result.physicalFileMetadata().uuid());

			Optional<FileUsageMetadata> foundFileUsage = fileUsageRepository.findByUuid(
					connection,
					result.fileUsageMetadata().uuid());

			assertTrue(foundPhysicalFile.isPresent());
			assertTrue(foundFileUsage.isPresent());
			assertEquals(result.physicalFileMetadata(), foundPhysicalFile.get());
			assertEquals(result.fileUsageMetadata(), foundFileUsage.get());
		}
	}

	@Test
	void shouldSaveFileWithRequiredFieldsOnly() throws IOException, SQLException {
		FileSaveService fileSaveService = this.createInitializedFileSaveService();

		SaveFileResult result;

		try (InputStream inputStream = this.createInputStream(CONTENT)) {
			SaveFileCommand command = SaveFileCommand.of(inputStream, ORIGINAL_NAME);

			result = fileSaveService.save(command);
		}

		assertEquals("default", result.fileUsageMetadata().usageType());
		assertNull(result.fileUsageMetadata().ownerType());
		assertNull(result.fileUsageMetadata().ownerId());
		assertNull(result.fileUsageMetadata().displayName());
		assertNull(result.fileUsageMetadata().metadataJson());
		assertEquals("photo.PNG", result.physicalFileMetadata().originalName());
		assertEquals("png", result.physicalFileMetadata().extension());
		assertEquals(HELLO_SHA256, result.physicalFileMetadata().sha256());
		assertTrue(Files.exists(result.storedFile().absolutePath()));
	}

	private FileSaveService createInitializedFileSaveService() throws IOException, SQLException {
		DatabaseConnectionFactory databaseConnectionFactory = this.createInitializedDatabaseConnectionFactory();

		return this.createFileSaveService(databaseConnectionFactory);
	}

	private FileSaveService createFileSaveService(DatabaseConnectionFactory databaseConnectionFactory) {
		return new FileSaveService(
				this.createLocalFileStorage(),
				new LocalFileDeleter(),
				new MetadataTransactionManager(databaseConnectionFactory),
				new PhysicalFileRepository(),
				new FileUsageRepository(),
				FIXED_CLOCK);
	}

	private DatabaseConnectionFactory createInitializedDatabaseConnectionFactory() throws IOException, SQLException {
		StorageDirectories storageDirectories = this.createInitializedStorageDirectories();
		DatabaseConnectionFactory databaseConnectionFactory = new DatabaseConnectionFactory(storageDirectories);
		MetadataSchemaInitializer metadataSchemaInitializer = new MetadataSchemaInitializer(databaseConnectionFactory);

		metadataSchemaInitializer.initialize();

		return databaseConnectionFactory;
	}

	private LocalFileStorage createLocalFileStorage() {
		PocketFilesConfig config = this.createConfig();
		StorageDirectories storageDirectories = new StorageDirectories(config);

		return new LocalFileStorage(
				new TempFileWriter(storageDirectories),
				new Sha256Calculator(),
				new LocalPathStrategy(config),
				new FinalFileMover(storageDirectories));
	}

	private StorageDirectories createInitializedStorageDirectories() throws IOException {
		StorageDirectories storageDirectories = this.createStorageDirectories();

		StorageInitializer storageInitializer = new StorageInitializer(storageDirectories);
		storageInitializer.initialize();

		return storageDirectories;
	}

	private StorageDirectories createStorageDirectories() {
		return new StorageDirectories(this.createConfig());
	}

	private PocketFilesConfig createConfig() {
		return PocketFilesConfig.builder()
				.baseDirectory(this.tempDir.resolve("pocket-files"))
				.directoryDepth(DirectoryDepth.DAY)
				.build();
	}

	private InputStream createInputStream(String content) {
		return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
	}

	private MetadataTransactionManager createTransactionManager() {
		return new MetadataTransactionManager(
				new DatabaseConnectionFactory(this.createStorageDirectories()));
	}

}
