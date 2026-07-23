package com.chyvacheck.pocketfiles.service;

import com.chyvacheck.pocketfiles.config.PocketFilesConfig;
import com.chyvacheck.pocketfiles.metadata.DatabaseConnectionFactory;
import com.chyvacheck.pocketfiles.metadata.MetadataSchemaInitializer;
import com.chyvacheck.pocketfiles.metadata.model.FileUsageMetadata;
import com.chyvacheck.pocketfiles.metadata.model.PhysicalFileMetadata;
import com.chyvacheck.pocketfiles.metadata.repository.FileUsageRepository;
import com.chyvacheck.pocketfiles.metadata.repository.PhysicalFileRepository;
import com.chyvacheck.pocketfiles.metadata.status.FileUsageStatus;
import com.chyvacheck.pocketfiles.storage.StorageDirectories;
import com.chyvacheck.pocketfiles.storage.StorageInitializer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests the behavior of the file restore service.
 */
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

	@TempDir
	Path tempDir;

	/**
	 * Tests the behavior of the file restore service when the database connection
	 * factory is null.
	 *
	 * @throws NullPointerException if the database connection factory is null
	 */
	@Test
	void shouldThrowExceptionWhenDatabaseConnectionFactoryIsNull() {
		NullPointerException exception = assertThrows(
				NullPointerException.class,
				() -> new FileRestoreService(
						null,
						new FileUsageRepository()));

		assertEquals("databaseConnectionFactory must not be null", exception.getMessage());
	}

	/**
	 * Tests the behavior of the file restore service when the file usage repository
	 * is null.
	 *
	 * @throws NullPointerException if the file usage repository is null
	 */
	@Test
	void shouldThrowExceptionWhenFileUsageRepositoryIsNull() {
		NullPointerException exception = assertThrows(
				NullPointerException.class,
				() -> new FileRestoreService(
						this.createDatabaseConnectionFactory(),
						null));

		assertEquals("fileUsageRepository must not be null", exception.getMessage());
	}

	/**
	 * Tests the behavior of the file restore service when the file usage UUID is
	 * null.
	 *
	 * @throws NullPointerException if the file usage UUID is null
	 */
	@Test
	void shouldThrowExceptionWhenFileUsageUuidIsNull() throws IOException, SQLException {
		FileRestoreService fileRestoreService = this.createInitializedFileRestoreService();

		NullPointerException exception = assertThrows(
				NullPointerException.class,
				() -> fileRestoreService.restore(null));

		assertEquals("fileUsageUuid must not be null", exception.getMessage());
	}

	/**
	 * Tests the behavior of the file restore service when a deleted file usage is
	 * restored.
	 *
	 * @throws IOException  if storage initialization fails
	 * @throws SQLException if metadata schema initialization fails
	 */
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

	/**
	 * Tests the behavior of the file restore service when a file usage is already
	 * active.
	 *
	 * @throws IOException  if storage initialization fails
	 * @throws SQLException if metadata schema initialization fails
	 */
	@Test
	void shouldReturnFileUsageWhenItIsAlreadyActive() throws IOException, SQLException {
		TestContext context = this.createTestContext();
		FileUsageMetadata activeMetadata = this.insertActiveFileUsage(context.databaseConnectionFactory());

		FileUsageMetadata restoredMetadata = context.fileRestoreService().restore(activeMetadata.uuid());

		assertEquals(activeMetadata, restoredMetadata);
		assertEquals(FileUsageStatus.ACTIVE, restoredMetadata.status());
		assertNull(restoredMetadata.deletedAt());
	}

	/**
	 * Tests the behavior of the file restore service when a file usage is not
	 * found.
	 *
	 * @throws IOException  if storage initialization fails
	 * @throws SQLException if metadata schema initialization fails
	 */
	@Test
	void shouldThrowExceptionWhenFileUsageIsNotFound() throws IOException, SQLException {
		FileRestoreService fileRestoreService = this.createInitializedFileRestoreService();

		IllegalArgumentException exception = assertThrows(
				IllegalArgumentException.class,
				() -> fileRestoreService.restore(MISSING_FILE_USAGE_UUID));

		assertEquals("File usage not found: " + MISSING_FILE_USAGE_UUID, exception.getMessage());
	}

	/**
	 * Tests the behavior of the file restore service when a deleted file usage is
	 * restored.
	 *
	 * @throws IOException  if storage initialization fails
	 * @throws SQLException if metadata schema initialization fails
	 */
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

	/**
	 * Creates an initialized file restore service for the test.
	 *
	 * @return initialized file restore service
	 * @throws IOException  if storage initialization fails
	 * @throws SQLException if metadata schema initialization fails
	 */
	private FileRestoreService createInitializedFileRestoreService() throws IOException, SQLException {
		TestContext context = this.createTestContext();

		return context.fileRestoreService();
	}

	/**
	 * Creates a test context for the file restore service.
	 *
	 * @return test context
	 * @throws IOException  if storage initialization fails
	 * @throws SQLException if metadata schema initialization fails
	 */
	private TestContext createTestContext() throws IOException, SQLException {
		DatabaseConnectionFactory databaseConnectionFactory = this.createInitializedDatabaseConnectionFactory();

		FileRestoreService fileRestoreService = new FileRestoreService(
				databaseConnectionFactory,
				new FileUsageRepository());

		return new TestContext(fileRestoreService, databaseConnectionFactory);
	}

	/**
	 * Inserts an active file usage for the test.
	 *
	 * @param databaseConnectionFactory database connection factory
	 * @return active file usage metadata
	 * @throws SQLException if database operation fails
	 */
	private FileUsageMetadata insertActiveFileUsage(DatabaseConnectionFactory databaseConnectionFactory)
			throws SQLException {
		PhysicalFileRepository physicalFileRepository = new PhysicalFileRepository();
		FileUsageRepository fileUsageRepository = new FileUsageRepository();

		try (Connection connection = databaseConnectionFactory.createConnection()) {
			PhysicalFileMetadata physicalFileMetadata = physicalFileRepository.insert(
					connection,
					this.createNewPhysicalFileMetadata());

			return fileUsageRepository.insert(
					connection,
					this.createNewFileUsageMetadata(physicalFileMetadata.id()));
		}
	}

	/**
	 * Inserts a deleted file usage for the test.
	 *
	 * @param databaseConnectionFactory database connection factory
	 * @return deleted file usage metadata
	 * @throws SQLException if database operation fails
	 */
	private FileUsageMetadata insertDeletedFileUsage(DatabaseConnectionFactory databaseConnectionFactory)
			throws SQLException {
		PhysicalFileRepository physicalFileRepository = new PhysicalFileRepository();
		FileUsageRepository fileUsageRepository = new FileUsageRepository();

		try (Connection connection = databaseConnectionFactory.createConnection()) {
			PhysicalFileMetadata physicalFileMetadata = physicalFileRepository.insert(
					connection,
					this.createNewPhysicalFileMetadata());

			FileUsageMetadata insertedMetadata = fileUsageRepository.insert(
					connection,
					this.createNewFileUsageMetadata(physicalFileMetadata.id()));

			return fileUsageRepository.markDeleted(
					connection,
					insertedMetadata.id(),
					DELETED_AT);
		}
	}

	/**
	 * Creates a new physical file metadata for the test.
	 *
	 * @return new physical file metadata
	 */
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

	/**
	 * Creates a new file usage metadata for the test.
	 *
	 * @param physicalFileId ID of the physical file
	 * @return new file usage metadata
	 */
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

	/**
	 * Creates and initializes the database connection factory for the test.
	 *
	 * @return initialized database connection factory
	 * @throws IOException  if storage initialization fails
	 * @throws SQLException if metadata schema initialization fails
	 */
	private DatabaseConnectionFactory createInitializedDatabaseConnectionFactory() throws IOException, SQLException {
		StorageDirectories storageDirectories = this.createInitializedStorageDirectories();
		DatabaseConnectionFactory databaseConnectionFactory = new DatabaseConnectionFactory(storageDirectories);

		new MetadataSchemaInitializer(databaseConnectionFactory).initialize();

		return databaseConnectionFactory;
	}

	/**
	 * Creates the database connection factory for the test.
	 *
	 * @return database connection factory
	 */
	private DatabaseConnectionFactory createDatabaseConnectionFactory() {
		return new DatabaseConnectionFactory(this.createStorageDirectories());
	}

	/**
	 * Creates and initializes the storage directories for the test.
	 *
	 * @return initialized storage directories
	 * @throws IOException if storage initialization fails
	 */
	private StorageDirectories createInitializedStorageDirectories() throws IOException {
		StorageDirectories storageDirectories = this.createStorageDirectories();

		new StorageInitializer(storageDirectories).initialize();

		return storageDirectories;
	}

	/**
	 * Creates the storage directories for the test.
	 *
	 * @return storage directories
	 */
	private StorageDirectories createStorageDirectories() {
		return new StorageDirectories(this.createConfig());
	}

	/**
	 * Creates the configuration for the test.
	 *
	 * @return configuration
	 */
	private PocketFilesConfig createConfig() {
		return PocketFilesConfig.builder()
				.baseDirectory(this.tempDir.resolve("pocket-files"))
				.build();
	}

	/**
	 * Test context for the file restore service.
	 *
	 * @param fileRestoreService        file restore service
	 * @param databaseConnectionFactory database connection factory
	 */
	private record TestContext(
			FileRestoreService fileRestoreService,
			DatabaseConnectionFactory databaseConnectionFactory) {
	}
}
