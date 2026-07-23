package com.chyvacheck.pocketfiles.service;

import com.chyvacheck.pocketfiles.config.PocketFilesConfig;
import com.chyvacheck.pocketfiles.metadata.DatabaseConnectionFactory;
import com.chyvacheck.pocketfiles.metadata.MetadataSchemaInitializer;
import com.chyvacheck.pocketfiles.metadata.model.FileUsageMetadata;
import com.chyvacheck.pocketfiles.metadata.model.PhysicalFileMetadata;
import com.chyvacheck.pocketfiles.metadata.repository.FileUsageRepository;
import com.chyvacheck.pocketfiles.metadata.repository.PhysicalFileRepository;
import com.chyvacheck.pocketfiles.metadata.status.PhysicalFileStatus;
import com.chyvacheck.pocketfiles.metadata.transaction.MetadataTransactionManager;
import com.chyvacheck.pocketfiles.storage.LocalFileDeleter;
import com.chyvacheck.pocketfiles.storage.StorageDirectories;
import com.chyvacheck.pocketfiles.storage.StorageInitializer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FilePurgeServiceTest {

	private static final UUID PHYSICAL_FILE_UUID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

	private static final UUID FILE_USAGE_UUID = UUID.fromString("11111111-1111-1111-1111-111111111111");

	private static final UUID SECOND_FILE_USAGE_UUID = UUID.fromString("22222222-2222-2222-2222-222222222222");

	private static final UUID MISSING_FILE_USAGE_UUID = UUID.fromString("33333333-3333-3333-3333-333333333333");

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

	private static final long PURGED_AT = FIXED_INSTANT.toEpochMilli();

	@TempDir
	Path tempDir;

	// ? constructor

	@Test
	void shouldThrowExceptionWhenTransactionManagerIsNull() {
		NullPointerException exception = assertThrows(
				NullPointerException.class,
				() -> new FilePurgeService(
						null,
						new FileUsageRepository(),
						new PhysicalFileRepository(),
						new LocalFileDeleter(),
						this.createStorageDirectories(),
						FIXED_CLOCK));

		assertEquals("transactionManager must not be null", exception.getMessage());
	}

	@Test
	void shouldThrowExceptionWhenFileUsageRepositoryIsNull() {
		NullPointerException exception = assertThrows(
				NullPointerException.class,
				() -> new FilePurgeService(
						this.createTransactionManager(),
						null,
						new PhysicalFileRepository(),
						new LocalFileDeleter(),
						this.createStorageDirectories(),
						FIXED_CLOCK));

		assertEquals("fileUsageRepository must not be null", exception.getMessage());
	}

	@Test
	void shouldThrowExceptionWhenPhysicalFileRepositoryIsNull() {
		NullPointerException exception = assertThrows(
				NullPointerException.class,
				() -> new FilePurgeService(
						this.createTransactionManager(),
						new FileUsageRepository(),
						null,
						new LocalFileDeleter(),
						this.createStorageDirectories(),
						FIXED_CLOCK));

		assertEquals("physicalFileRepository must not be null", exception.getMessage());
	}

	@Test
	void shouldThrowExceptionWhenLocalFileDeleterIsNull() {
		NullPointerException exception = assertThrows(
				NullPointerException.class,
				() -> new FilePurgeService(
						this.createTransactionManager(),
						new FileUsageRepository(),
						new PhysicalFileRepository(),
						null,
						this.createStorageDirectories(),
						FIXED_CLOCK));

		assertEquals("localFileDeleter must not be null", exception.getMessage());
	}

	@Test
	void shouldThrowExceptionWhenStorageDirectoriesIsNull() {
		NullPointerException exception = assertThrows(
				NullPointerException.class,
				() -> new FilePurgeService(
						this.createTransactionManager(),
						new FileUsageRepository(),
						new PhysicalFileRepository(),
						new LocalFileDeleter(),
						null,
						FIXED_CLOCK));

		assertEquals("storageDirectories must not be null", exception.getMessage());
	}

	@Test
	void shouldThrowExceptionWhenClockIsNull() {
		NullPointerException exception = assertThrows(
				NullPointerException.class,
				() -> new FilePurgeService(
						this.createTransactionManager(),
						new FileUsageRepository(),
						new PhysicalFileRepository(),
						new LocalFileDeleter(),
						this.createStorageDirectories(),
						null));

		assertEquals("clock must not be null", exception.getMessage());
	}

	// ? purge

	@Test
	void shouldThrowExceptionWhenFileUsageUuidIsNull() throws IOException, SQLException {
		FilePurgeService filePurgeService = this.createInitializedFilePurgeService();

		NullPointerException exception = assertThrows(
				NullPointerException.class,
				() -> filePurgeService.purge(null));

		assertEquals("fileUsageUuid must not be null", exception.getMessage());
	}

	@Test
	void shouldThrowExceptionWhenFileUsageIsNotFound() throws IOException, SQLException {
		FilePurgeService filePurgeService = this.createInitializedFilePurgeService();

		SQLException exception = assertThrows(
				SQLException.class,
				() -> filePurgeService.purge(MISSING_FILE_USAGE_UUID));

		assertEquals("Transaction failed", exception.getMessage());
		assertEquals(IllegalArgumentException.class, exception.getCause().getClass());
		assertEquals("File usage not found: " + MISSING_FILE_USAGE_UUID, exception.getCause().getMessage());
	}

	@Test
	void shouldThrowExceptionWhenFileUsageIsActive() throws IOException, SQLException {
		TestContext context = this.createTestContext();
		FileUsageMetadata activeFileUsage = this.insertActiveFileUsage(context.databaseConnectionFactory());

		SQLException exception = assertThrows(
				SQLException.class,
				() -> context.filePurgeService().purge(activeFileUsage.uuid()));

		assertEquals("Transaction failed", exception.getMessage());
		assertEquals(IllegalStateException.class, exception.getCause().getClass());
		assertEquals("File usage must be deleted before purge: " + activeFileUsage.uuid(),
				exception.getCause().getMessage());
	}

	@Test
	void shouldThrowExceptionWhenPhysicalFileIsNotOrphaned() throws IOException, SQLException {
		TestContext context = this.createTestContext();
		FileUsageMetadata deletedFileUsage = this.insertDeletedFileUsageWithoutOrphanedPhysicalFile(
				context.databaseConnectionFactory());

		SQLException exception = assertThrows(
				SQLException.class,
				() -> context.filePurgeService().purge(deletedFileUsage.uuid()));

		assertEquals("Transaction failed", exception.getMessage());
		assertEquals(IllegalStateException.class, exception.getCause().getClass());
		assertEquals("Physical file must be orphaned before purge: " + PHYSICAL_FILE_UUID,
				exception.getCause().getMessage());
	}

	@Test
	void shouldThrowExceptionWhenPhysicalFileHasActiveUsages() throws IOException, SQLException {
		TestContext context = this.createTestContext();
		FileUsageMetadata deletedFileUsage = this.insertDeletedFileUsageWithAnotherActiveUsage(
				context.databaseConnectionFactory());

		SQLException exception = assertThrows(
				SQLException.class,
				() -> context.filePurgeService().purge(deletedFileUsage.uuid()));

		assertEquals("Transaction failed", exception.getMessage());
		assertEquals(IllegalStateException.class, exception.getCause().getClass());
		assertEquals("Cannot purge physical file with active usages: " + PHYSICAL_FILE_UUID,
				exception.getCause().getMessage());
	}

	@Test
	void shouldPurgeDeletedFileUsage() throws IOException, SQLException {
		TestContext context = this.createTestContext();
		FileUsageMetadata deletedFileUsage = this.insertDeletedFileUsageWithOrphanedPhysicalFile(
				context.databaseConnectionFactory());

		Path filePath = this.createStoredFile(context.storageDirectories());

		PhysicalFileMetadata purgedPhysicalFile = context.filePurgeService().purge(deletedFileUsage.uuid());

		assertEquals(deletedFileUsage.physicalFileId(), purgedPhysicalFile.id());
		assertEquals(PHYSICAL_FILE_UUID, purgedPhysicalFile.uuid());
		assertEquals(PhysicalFileStatus.DELETED, purgedPhysicalFile.status());
		assertEquals(PURGED_AT, purgedPhysicalFile.statusChangedAt());
		assertEquals(PURGED_AT, purgedPhysicalFile.deletedAt());
		assertFalse(Files.exists(filePath));
	}

	@Test
	void shouldReturnPhysicalFileWhenItIsAlreadyDeleted() throws IOException, SQLException {
		TestContext context = this.createTestContext();
		FileUsageMetadata deletedFileUsage = this.insertDeletedFileUsageWithDeletedPhysicalFile(
				context.databaseConnectionFactory());

		PhysicalFileMetadata purgedPhysicalFile = context.filePurgeService().purge(deletedFileUsage.uuid());

		assertEquals(deletedFileUsage.physicalFileId(), purgedPhysicalFile.id());
		assertEquals(PHYSICAL_FILE_UUID, purgedPhysicalFile.uuid());
		assertEquals(PhysicalFileStatus.DELETED, purgedPhysicalFile.status());
		assertEquals(DELETED_AT, purgedPhysicalFile.statusChangedAt());
		assertEquals(DELETED_AT, purgedPhysicalFile.deletedAt());
	}

	@Test
	void shouldPersistPurgedPhysicalFile() throws IOException, SQLException {
		TestContext context = this.createTestContext();
		FileUsageMetadata deletedFileUsage = this.insertDeletedFileUsageWithOrphanedPhysicalFile(
				context.databaseConnectionFactory());

		this.createStoredFile(context.storageDirectories());

		context.filePurgeService().purge(deletedFileUsage.uuid());

		try (Connection connection = context.databaseConnectionFactory().createConnection()) {
			PhysicalFileRepository physicalFileRepository = new PhysicalFileRepository();

			PhysicalFileMetadata foundPhysicalFile = physicalFileRepository.findById(
					connection,
					deletedFileUsage.physicalFileId())
					.orElseThrow();

			assertEquals(PhysicalFileStatus.DELETED, foundPhysicalFile.status());
			assertEquals(PURGED_AT, foundPhysicalFile.statusChangedAt());
			assertEquals(PURGED_AT, foundPhysicalFile.deletedAt());
		}
	}

	// ? helpers

	private FilePurgeService createInitializedFilePurgeService() throws IOException, SQLException {
		TestContext context = this.createTestContext();

		return context.filePurgeService();
	}

	private TestContext createTestContext() throws IOException, SQLException {
		StorageDirectories storageDirectories = this.createInitializedStorageDirectories();
		DatabaseConnectionFactory databaseConnectionFactory = new DatabaseConnectionFactory(storageDirectories);

		new MetadataSchemaInitializer(databaseConnectionFactory).initialize();

		FilePurgeService filePurgeService = new FilePurgeService(
				new MetadataTransactionManager(databaseConnectionFactory),
				new FileUsageRepository(),
				new PhysicalFileRepository(),
				new LocalFileDeleter(),
				storageDirectories,
				FIXED_CLOCK);

		return new TestContext(
				filePurgeService,
				databaseConnectionFactory,
				storageDirectories);
	}

	private FileUsageMetadata insertActiveFileUsage(DatabaseConnectionFactory databaseConnectionFactory)
			throws SQLException {
		PhysicalFileMetadata physicalFileMetadata = this.insertPhysicalFile(databaseConnectionFactory);

		return this.insertFileUsage(
				databaseConnectionFactory,
				physicalFileMetadata.id(),
				FILE_USAGE_UUID);
	}

	private FileUsageMetadata insertDeletedFileUsageWithoutOrphanedPhysicalFile(
			DatabaseConnectionFactory databaseConnectionFactory) throws SQLException {
		PhysicalFileMetadata physicalFileMetadata = this.insertPhysicalFile(databaseConnectionFactory);
		FileUsageMetadata fileUsageMetadata = this.insertFileUsage(
				databaseConnectionFactory,
				physicalFileMetadata.id(),
				FILE_USAGE_UUID);

		return this.markFileUsageDeleted(
				databaseConnectionFactory,
				fileUsageMetadata.id());
	}

	private FileUsageMetadata insertDeletedFileUsageWithOrphanedPhysicalFile(
			DatabaseConnectionFactory databaseConnectionFactory) throws SQLException {
		PhysicalFileMetadata physicalFileMetadata = this.insertPhysicalFile(databaseConnectionFactory);
		FileUsageMetadata fileUsageMetadata = this.insertFileUsage(
				databaseConnectionFactory,
				physicalFileMetadata.id(),
				FILE_USAGE_UUID);

		FileUsageMetadata deletedFileUsage = this.markFileUsageDeleted(
				databaseConnectionFactory,
				fileUsageMetadata.id());

		this.markPhysicalFileOrphaned(
				databaseConnectionFactory,
				physicalFileMetadata.id());

		return deletedFileUsage;
	}

	private FileUsageMetadata insertDeletedFileUsageWithAnotherActiveUsage(
			DatabaseConnectionFactory databaseConnectionFactory) throws SQLException {
		PhysicalFileMetadata physicalFileMetadata = this.insertPhysicalFile(databaseConnectionFactory);

		FileUsageMetadata fileUsageMetadata = this.insertFileUsage(
				databaseConnectionFactory,
				physicalFileMetadata.id(),
				FILE_USAGE_UUID);

		this.insertFileUsage(
				databaseConnectionFactory,
				physicalFileMetadata.id(),
				SECOND_FILE_USAGE_UUID);

		FileUsageMetadata deletedFileUsage = this.markFileUsageDeleted(
				databaseConnectionFactory,
				fileUsageMetadata.id());

		this.markPhysicalFileOrphaned(
				databaseConnectionFactory,
				physicalFileMetadata.id());

		return deletedFileUsage;
	}

	private FileUsageMetadata insertDeletedFileUsageWithDeletedPhysicalFile(
			DatabaseConnectionFactory databaseConnectionFactory) throws SQLException {
		PhysicalFileMetadata physicalFileMetadata = this.insertPhysicalFile(databaseConnectionFactory);
		FileUsageMetadata fileUsageMetadata = this.insertFileUsage(
				databaseConnectionFactory,
				physicalFileMetadata.id(),
				FILE_USAGE_UUID);

		FileUsageMetadata deletedFileUsage = this.markFileUsageDeleted(
				databaseConnectionFactory,
				fileUsageMetadata.id());

		this.markPhysicalFileDeleted(
				databaseConnectionFactory,
				physicalFileMetadata.id());

		return deletedFileUsage;
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
			long physicalFileId,
			UUID fileUsageUuid) throws SQLException {
		FileUsageRepository fileUsageRepository = new FileUsageRepository();

		try (Connection connection = databaseConnectionFactory.createConnection()) {
			return fileUsageRepository.insert(
					connection,
					this.createNewFileUsageMetadata(physicalFileId, fileUsageUuid));
		}
	}

	private FileUsageMetadata markFileUsageDeleted(
			DatabaseConnectionFactory databaseConnectionFactory,
			long fileUsageId) throws SQLException {
		FileUsageRepository fileUsageRepository = new FileUsageRepository();

		try (Connection connection = databaseConnectionFactory.createConnection()) {
			return fileUsageRepository.markDeleted(
					connection,
					fileUsageId,
					DELETED_AT);
		}
	}

	private PhysicalFileMetadata markPhysicalFileOrphaned(
			DatabaseConnectionFactory databaseConnectionFactory,
			long physicalFileId) throws SQLException {
		PhysicalFileRepository physicalFileRepository = new PhysicalFileRepository();

		try (Connection connection = databaseConnectionFactory.createConnection()) {
			return physicalFileRepository.markOrphaned(
					connection,
					physicalFileId,
					DELETED_AT);
		}
	}

	private PhysicalFileMetadata markPhysicalFileDeleted(
			DatabaseConnectionFactory databaseConnectionFactory,
			long physicalFileId) throws SQLException {
		PhysicalFileRepository physicalFileRepository = new PhysicalFileRepository();

		try (Connection connection = databaseConnectionFactory.createConnection()) {
			return physicalFileRepository.markDeleted(
					connection,
					physicalFileId,
					DELETED_AT);
		}
	}

	private Path createStoredFile(StorageDirectories storageDirectories) throws IOException {
		Path filePath = storageDirectories.resolveFilePath(RELATIVE_PATH);

		Files.createDirectories(filePath.getParent());
		Files.writeString(filePath, "Hello");

		return filePath;
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

	private FileUsageMetadata createNewFileUsageMetadata(long physicalFileId, UUID fileUsageUuid) {
		return FileUsageMetadata.newUsage(
				fileUsageUuid,
				physicalFileId,
				USAGE_TYPE,
				OWNER_TYPE,
				OWNER_ID,
				DISPLAY_NAME,
				METADATA_JSON,
				CREATED_AT);
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
			FilePurgeService filePurgeService,
			DatabaseConnectionFactory databaseConnectionFactory,
			StorageDirectories storageDirectories) {
	}
}
