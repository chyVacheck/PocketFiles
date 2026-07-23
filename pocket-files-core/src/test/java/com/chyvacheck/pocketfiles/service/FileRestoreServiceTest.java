package com.chyvacheck.pocketfiles.service;

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
import com.chyvacheck.pocketfiles.storage.StorageDirectories;
import com.chyvacheck.pocketfiles.storage.StorageInitializer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FileRestoreServiceTest {

	private static final UUID PHYSICAL_FILE_UUID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

	private static final UUID FILE_USAGE_UUID = UUID.fromString("11111111-1111-1111-1111-111111111111");

	private static final UUID MISSING_FILE_USAGE_UUID = UUID.fromString("22222222-2222-2222-2222-222222222222");

	private static final String ORIGINAL_NAME = "photo.png";

	private static final String RELATIVE_PATH = "2026/01/02/550e8400-e29b-41d4-a716-446655440000.png";

	private static final String MIME_TYPE = "image/png";

	private static final String EXTENSION = "png";

	private static final long SIZE_BYTES = 5L;

	private static final String SHA256 = "185f8db32271fe25f561a6fc938b2e264306ec304eda518007d1764826381969";

	private static final String USAGE_TYPE = "invoice_attachment";

	private static final String OWNER_TYPE = "invoice";

	private static final String OWNER_ID = "777";

	private static final String DISPLAY_NAME = "Invoice January.pdf";

	private static final String METADATA_JSON = """
			{"source":"upload-form","category":"invoice"}
			""";

	private static final long CREATED_AT = 1760000000000L;

	private static final long DELETED_AT = 1760000005000L;

	private static final Instant FIXED_INSTANT = Instant.parse("2026-01-02T03:15:05Z");

	private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);

	private static final long STATUS_CHANGED_AT = FIXED_INSTANT.toEpochMilli();

	@TempDir
	Path tempDir;

	// ? constructor

	@Test
	void shouldThrowExceptionWhenTransactionManagerIsNull() {
		NullPointerException exception = assertThrows(
				NullPointerException.class,
				() -> new FileRestoreService(
						null,
						new FileUsageRepository(),
						new PhysicalFileRepository(),
						FIXED_CLOCK));

		assertEquals("transactionManager must not be null", exception.getMessage());
	}

	@Test
	void shouldThrowExceptionWhenFileUsageRepositoryIsNull() {
		NullPointerException exception = assertThrows(
				NullPointerException.class,
				() -> new FileRestoreService(
						this.createTransactionManager(),
						null,
						new PhysicalFileRepository(),
						FIXED_CLOCK));

		assertEquals("fileUsageRepository must not be null", exception.getMessage());
	}

	@Test
	void shouldThrowExceptionWhenPhysicalFileRepositoryIsNull() {
		NullPointerException exception = assertThrows(
				NullPointerException.class,
				() -> new FileRestoreService(
						this.createTransactionManager(),
						new FileUsageRepository(),
						null,
						FIXED_CLOCK));

		assertEquals("physicalFileRepository must not be null", exception.getMessage());
	}

	@Test
	void shouldThrowExceptionWhenClockIsNull() {
		NullPointerException exception = assertThrows(
				NullPointerException.class,
				() -> new FileRestoreService(
						this.createTransactionManager(),
						new FileUsageRepository(),
						new PhysicalFileRepository(),
						null));

		assertEquals("clock must not be null", exception.getMessage());
	}

	// ? restore

	@Test
	void shouldThrowExceptionWhenFileUsageUuidIsNull() throws IOException, SQLException {
		FileRestoreService fileRestoreService = this.createInitializedFileRestoreService();

		NullPointerException exception = assertThrows(
				NullPointerException.class,
				() -> fileRestoreService.restore(null));

		assertEquals("fileUsageUuid must not be null", exception.getMessage());
	}

	@Test
	void shouldThrowExceptionWhenFileUsageIsNotFound() throws IOException, SQLException {
		FileRestoreService fileRestoreService = this.createInitializedFileRestoreService();

		SQLException exception = assertThrows(
				SQLException.class,
				() -> fileRestoreService.restore(MISSING_FILE_USAGE_UUID));

		assertEquals("Transaction failed", exception.getMessage());
		assertEquals(IllegalArgumentException.class, exception.getCause().getClass());
		assertEquals("File usage not found: " + MISSING_FILE_USAGE_UUID, exception.getCause().getMessage());
	}

	@Test
	void shouldRestoreDeletedFileUsage() throws IOException, SQLException {
		TestContext context = this.createTestContext();
		FileUsageMetadata deletedMetadata = this.insertDeletedFileUsage(context.databaseConnectionFactory());

		FileUsageMetadata restoredMetadata = context.fileRestoreService().restore(deletedMetadata.uuid());

		assertEquals(deletedMetadata.id(), restoredMetadata.id());
		assertEquals(deletedMetadata.uuid(), restoredMetadata.uuid());
		assertEquals(deletedMetadata.physicalFileId(), restoredMetadata.physicalFileId());
		assertEquals(deletedMetadata.usageType(), restoredMetadata.usageType());
		assertEquals(deletedMetadata.ownerType(), restoredMetadata.ownerType());
		assertEquals(deletedMetadata.ownerId(), restoredMetadata.ownerId());
		assertEquals(deletedMetadata.displayName(), restoredMetadata.displayName());
		assertEquals(deletedMetadata.metadataJson(), restoredMetadata.metadataJson());
		assertEquals(FileUsageStatus.ACTIVE, restoredMetadata.status());
		assertEquals(deletedMetadata.createdAt(), restoredMetadata.createdAt());
		assertNull(restoredMetadata.deletedAt());
	}

	@Test
	void shouldReturnFileUsageWhenItIsAlreadyActive() throws IOException, SQLException {
		TestContext context = this.createTestContext();
		FileUsageMetadata activeMetadata = this.insertActiveFileUsage(context.databaseConnectionFactory());

		FileUsageMetadata restoredMetadata = context.fileRestoreService().restore(activeMetadata.uuid());

		assertEquals(activeMetadata, restoredMetadata);
		assertEquals(FileUsageStatus.ACTIVE, restoredMetadata.status());
		assertNull(restoredMetadata.deletedAt());
	}

	@Test
	void shouldPersistRestoredFileUsage() throws IOException, SQLException {
		TestContext context = this.createTestContext();
		FileUsageMetadata deletedMetadata = this.insertDeletedFileUsage(context.databaseConnectionFactory());

		context.fileRestoreService().restore(deletedMetadata.uuid());

		try (Connection connection = context.databaseConnectionFactory().createConnection()) {
			FileUsageRepository fileUsageRepository = new FileUsageRepository();

			FileUsageMetadata foundMetadata = fileUsageRepository.findByUuid(
					connection,
					deletedMetadata.uuid())
					.orElseThrow();

			assertEquals(FileUsageStatus.ACTIVE, foundMetadata.status());
			assertNull(foundMetadata.deletedAt());
		}
	}

	@Test
	void shouldMarkOrphanedPhysicalFileAsActiveWhenRestoringDeletedFileUsage() throws IOException, SQLException {
		TestContext context = this.createTestContext();
		FileUsageMetadata deletedMetadata = this.insertDeletedFileUsage(context.databaseConnectionFactory());

		context.fileRestoreService().restore(deletedMetadata.uuid());

		try (Connection connection = context.databaseConnectionFactory().createConnection()) {
			PhysicalFileRepository physicalFileRepository = new PhysicalFileRepository();

			PhysicalFileMetadata foundPhysicalFile = physicalFileRepository.findById(
					connection,
					deletedMetadata.physicalFileId())
					.orElseThrow();

			assertEquals(PhysicalFileStatus.ACTIVE, foundPhysicalFile.status());
			assertEquals(STATUS_CHANGED_AT, foundPhysicalFile.statusChangedAt());
			assertNull(foundPhysicalFile.deletedAt());
		}
	}

	// ? helpers

	private FileRestoreService createInitializedFileRestoreService() throws IOException, SQLException {
		TestContext context = this.createTestContext();

		return context.fileRestoreService();
	}

	private TestContext createTestContext() throws IOException, SQLException {
		DatabaseConnectionFactory databaseConnectionFactory = this.createInitializedDatabaseConnectionFactory();

		FileRestoreService fileRestoreService = new FileRestoreService(
				new MetadataTransactionManager(databaseConnectionFactory),
				new FileUsageRepository(),
				new PhysicalFileRepository(),
				FIXED_CLOCK);

		return new TestContext(fileRestoreService, databaseConnectionFactory);
	}

	private FileUsageMetadata insertActiveFileUsage(DatabaseConnectionFactory databaseConnectionFactory)
			throws SQLException {
		PhysicalFileMetadata physicalFileMetadata = this.insertPhysicalFile(databaseConnectionFactory);

		return this.insertFileUsage(
				databaseConnectionFactory,
				physicalFileMetadata.id());
	}

	private FileUsageMetadata insertDeletedFileUsage(DatabaseConnectionFactory databaseConnectionFactory)
			throws SQLException {
		PhysicalFileMetadata physicalFileMetadata = this.insertPhysicalFile(databaseConnectionFactory);

		try (Connection connection = databaseConnectionFactory.createConnection()) {
			FileUsageRepository fileUsageRepository = new FileUsageRepository();
			PhysicalFileRepository physicalFileRepository = new PhysicalFileRepository();

			FileUsageMetadata insertedMetadata = fileUsageRepository.insert(
					connection,
					this.createNewFileUsageMetadata(physicalFileMetadata.id()));

			FileUsageMetadata deletedMetadata = fileUsageRepository.markDeleted(
					connection,
					insertedMetadata.id(),
					DELETED_AT);

			physicalFileRepository.markOrphaned(
					connection,
					physicalFileMetadata.id(),
					DELETED_AT);

			return deletedMetadata;
		}
	}

	private PhysicalFileMetadata insertPhysicalFile(DatabaseConnectionFactory databaseConnectionFactory)
			throws SQLException {
		PhysicalFileRepository physicalFileRepository = new PhysicalFileRepository();

		try (Connection connection = databaseConnectionFactory.createConnection()) {
			return physicalFileRepository.insert(
					connection,
					this.createNewPhysicalFileMetadata());
		}
	}

	private FileUsageMetadata insertFileUsage(
			DatabaseConnectionFactory databaseConnectionFactory,
			long physicalFileId) throws SQLException {
		FileUsageRepository fileUsageRepository = new FileUsageRepository();

		try (Connection connection = databaseConnectionFactory.createConnection()) {
			return fileUsageRepository.insert(
					connection,
					this.createNewFileUsageMetadata(physicalFileId));
		}
	}

	private PhysicalFileMetadata createNewPhysicalFileMetadata() {
		return PhysicalFileMetadata.newFile(
				PHYSICAL_FILE_UUID,
				ORIGINAL_NAME,
				RELATIVE_PATH,
				MIME_TYPE,
				EXTENSION,
				SIZE_BYTES,
				SHA256,
				CREATED_AT);
	}

	private FileUsageMetadata createNewFileUsageMetadata(long physicalFileId) {
		return FileUsageMetadata.newUsage(
				FILE_USAGE_UUID,
				physicalFileId,
				USAGE_TYPE,
				OWNER_TYPE,
				OWNER_ID,
				DISPLAY_NAME,
				METADATA_JSON,
				CREATED_AT);
	}

	private DatabaseConnectionFactory createInitializedDatabaseConnectionFactory() throws IOException, SQLException {
		StorageDirectories storageDirectories = this.createInitializedStorageDirectories();
		DatabaseConnectionFactory databaseConnectionFactory = new DatabaseConnectionFactory(storageDirectories);

		new MetadataSchemaInitializer(databaseConnectionFactory).initialize();

		return databaseConnectionFactory;
	}

	private MetadataTransactionManager createTransactionManager() {
		return new MetadataTransactionManager(this.createDatabaseConnectionFactory());
	}

	private DatabaseConnectionFactory createDatabaseConnectionFactory() {
		return new DatabaseConnectionFactory(this.createStorageDirectories());
	}

	private StorageDirectories createInitializedStorageDirectories() throws IOException {
		StorageDirectories storageDirectories = this.createStorageDirectories();

		new StorageInitializer(storageDirectories).initialize();

		return storageDirectories;
	}

	private StorageDirectories createStorageDirectories() {
		return new StorageDirectories(this.createConfig());
	}

	private PocketFilesConfig createConfig() {
		return PocketFilesConfig.builder()
				.baseDirectory(this.tempDir.resolve("pocket-files"))
				.build();
	}

	private record TestContext(
			FileRestoreService fileRestoreService,
			DatabaseConnectionFactory databaseConnectionFactory) {
	}
}
