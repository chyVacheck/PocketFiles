package com.chyvacheck.pocketfiles.metadata.repository;

import com.chyvacheck.pocketfiles.config.PocketFilesConfig;
import com.chyvacheck.pocketfiles.metadata.DatabaseConnectionFactory;
import com.chyvacheck.pocketfiles.metadata.MetadataSchemaInitializer;
import com.chyvacheck.pocketfiles.metadata.model.FileUsageMetadata;
import com.chyvacheck.pocketfiles.metadata.model.PhysicalFileMetadata;
import com.chyvacheck.pocketfiles.metadata.status.FileUsageStatus;
import com.chyvacheck.pocketfiles.storage.StorageDirectories;
import com.chyvacheck.pocketfiles.storage.StorageInitializer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileUsageRepositoryTest {
	private static final UUID PHYSICAL_FILE_UUID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

	private static final UUID FILE_USAGE_UUID = UUID.fromString("11111111-1111-1111-1111-111111111111");

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

	@Test
	void shouldInsertFileUsageMetadata() throws IOException, SQLException {
		DatabaseConnectionFactory databaseConnectionFactory = this.createInitializedDatabaseConnectionFactory();
		PhysicalFileRepository physicalFileRepository = new PhysicalFileRepository();
		FileUsageRepository fileUsageRepository = new FileUsageRepository();

		try (Connection connection = databaseConnectionFactory.createConnection()) {
			PhysicalFileMetadata physicalFileMetadata = physicalFileRepository.insert(
					connection,
					this.createNewPhysicalFileMetadata());

			FileUsageMetadata fileUsageMetadata = this.createNewFileUsageMetadata(physicalFileMetadata.id());

			FileUsageMetadata insertedMetadata = fileUsageRepository.insert(connection, fileUsageMetadata);

			assertNotNull(insertedMetadata.id());
			assertEquals(FILE_USAGE_UUID, insertedMetadata.uuid());
			assertEquals(physicalFileMetadata.id(), insertedMetadata.physicalFileId());
			assertEquals(USAGE_TYPE, insertedMetadata.usageType());
			assertEquals(OWNER_TYPE, insertedMetadata.ownerType());
			assertEquals(OWNER_ID, insertedMetadata.ownerId());
			assertEquals(DISPLAY_NAME, insertedMetadata.displayName());
			assertEquals(METADATA_JSON.trim(), insertedMetadata.metadataJson());
			assertEquals(FileUsageStatus.ACTIVE, insertedMetadata.status());
			assertEquals(CREATED_AT, insertedMetadata.createdAt());
			assertNull(insertedMetadata.deletedAt());
		}
	}

	@Test
	void shouldInsertFileUsageMetadataWithNullableFields() throws IOException, SQLException {
		DatabaseConnectionFactory databaseConnectionFactory = this.createInitializedDatabaseConnectionFactory();
		PhysicalFileRepository physicalFileRepository = new PhysicalFileRepository();
		FileUsageRepository fileUsageRepository = new FileUsageRepository();

		try (Connection connection = databaseConnectionFactory.createConnection()) {
			PhysicalFileMetadata physicalFileMetadata = physicalFileRepository.insert(
					connection,
					this.createNewPhysicalFileMetadata());

			FileUsageMetadata fileUsageMetadata = FileUsageMetadata.newUsage(
					FILE_USAGE_UUID,
					physicalFileMetadata.id(),
					null,
					null,
					null,
					null,
					null,
					CREATED_AT);

			FileUsageMetadata insertedMetadata = fileUsageRepository.insert(connection, fileUsageMetadata);

			assertNotNull(insertedMetadata.id());
			assertEquals("default", insertedMetadata.usageType());
			assertNull(insertedMetadata.ownerType());
			assertNull(insertedMetadata.ownerId());
			assertNull(insertedMetadata.displayName());
			assertNull(insertedMetadata.metadataJson());
			assertNull(insertedMetadata.deletedAt());
		}
	}

	@Test
	void shouldFindFileUsageMetadataById() throws IOException, SQLException {
		DatabaseConnectionFactory databaseConnectionFactory = this.createInitializedDatabaseConnectionFactory();
		PhysicalFileRepository physicalFileRepository = new PhysicalFileRepository();
		FileUsageRepository fileUsageRepository = new FileUsageRepository();

		try (Connection connection = databaseConnectionFactory.createConnection()) {
			PhysicalFileMetadata physicalFileMetadata = physicalFileRepository.insert(
					connection,
					this.createNewPhysicalFileMetadata());

			FileUsageMetadata insertedMetadata = fileUsageRepository.insert(
					connection,
					this.createNewFileUsageMetadata(physicalFileMetadata.id()));

			Optional<FileUsageMetadata> foundMetadata = fileUsageRepository.findById(
					connection,
					insertedMetadata.id());

			assertTrue(foundMetadata.isPresent());
			assertEquals(insertedMetadata, foundMetadata.get());
		}
	}

	@Test
	void shouldReturnEmptyWhenFileUsageMetadataByIdDoesNotExist() throws IOException, SQLException {
		DatabaseConnectionFactory databaseConnectionFactory = this.createInitializedDatabaseConnectionFactory();
		FileUsageRepository fileUsageRepository = new FileUsageRepository();

		try (Connection connection = databaseConnectionFactory.createConnection()) {
			Optional<FileUsageMetadata> foundMetadata = fileUsageRepository.findById(connection, 999L);

			assertTrue(foundMetadata.isEmpty());
		}
	}

	@Test
	void shouldFindFileUsageMetadataByUuid() throws IOException, SQLException {
		DatabaseConnectionFactory databaseConnectionFactory = this.createInitializedDatabaseConnectionFactory();
		PhysicalFileRepository physicalFileRepository = new PhysicalFileRepository();
		FileUsageRepository fileUsageRepository = new FileUsageRepository();

		try (Connection connection = databaseConnectionFactory.createConnection()) {
			PhysicalFileMetadata physicalFileMetadata = physicalFileRepository.insert(
					connection,
					this.createNewPhysicalFileMetadata());

			FileUsageMetadata insertedMetadata = fileUsageRepository.insert(
					connection,
					this.createNewFileUsageMetadata(physicalFileMetadata.id()));

			Optional<FileUsageMetadata> foundMetadata = fileUsageRepository.findByUuid(
					connection,
					insertedMetadata.uuid());

			assertTrue(foundMetadata.isPresent());
			assertEquals(insertedMetadata, foundMetadata.get());
		}
	}

	@Test
	void shouldReturnEmptyWhenFileUsageMetadataByUuidDoesNotExist() throws IOException, SQLException {
		DatabaseConnectionFactory databaseConnectionFactory = this.createInitializedDatabaseConnectionFactory();
		FileUsageRepository fileUsageRepository = new FileUsageRepository();

		try (Connection connection = databaseConnectionFactory.createConnection()) {
			Optional<FileUsageMetadata> foundMetadata = fileUsageRepository.findByUuid(
					connection,
					UUID.fromString("22222222-2222-2222-2222-222222222222"));

			assertTrue(foundMetadata.isEmpty());
		}
	}

	@Test
	void shouldThrowExceptionWhenPhysicalFileDoesNotExist() throws IOException, SQLException {
		DatabaseConnectionFactory databaseConnectionFactory = this.createInitializedDatabaseConnectionFactory();
		FileUsageRepository fileUsageRepository = new FileUsageRepository();

		FileUsageMetadata fileUsageMetadata = this.createNewFileUsageMetadata(999L);

		try (Connection connection = databaseConnectionFactory.createConnection()) {
			assertThrows(
					SQLException.class,
					() -> fileUsageRepository.insert(connection, fileUsageMetadata));
		}
	}

	@Test
	void shouldThrowExceptionWhenInsertConnectionIsNull() {
		FileUsageRepository fileUsageRepository = new FileUsageRepository();

		NullPointerException exception = assertThrows(
				NullPointerException.class,
				() -> fileUsageRepository.insert(null, this.createNewFileUsageMetadata(1L)));

		assertEquals("connection must not be null", exception.getMessage());
	}

	@Test
	void shouldThrowExceptionWhenInsertMetadataIsNull() throws IOException, SQLException {
		DatabaseConnectionFactory databaseConnectionFactory = this.createInitializedDatabaseConnectionFactory();
		FileUsageRepository fileUsageRepository = new FileUsageRepository();

		try (Connection connection = databaseConnectionFactory.createConnection()) {
			NullPointerException exception = assertThrows(
					NullPointerException.class,
					() -> fileUsageRepository.insert(connection, null));

			assertEquals("metadata must not be null", exception.getMessage());
		}
	}

	@Test
	void shouldThrowExceptionWhenFindByIdConnectionIsNull() {
		FileUsageRepository fileUsageRepository = new FileUsageRepository();

		NullPointerException exception = assertThrows(
				NullPointerException.class,
				() -> fileUsageRepository.findById(null, 1L));

		assertEquals("connection must not be null", exception.getMessage());
	}

	@Test
	void shouldThrowExceptionWhenFindByIdIdIsZero() throws IOException, SQLException {
		DatabaseConnectionFactory databaseConnectionFactory = this.createInitializedDatabaseConnectionFactory();
		FileUsageRepository fileUsageRepository = new FileUsageRepository();

		try (Connection connection = databaseConnectionFactory.createConnection()) {
			IllegalArgumentException exception = assertThrows(
					IllegalArgumentException.class,
					() -> fileUsageRepository.findById(connection, 0L));

			assertEquals("id must be positive", exception.getMessage());
		}
	}

	@Test
	void shouldThrowExceptionWhenFindByIdIdIsNegative() throws IOException, SQLException {
		DatabaseConnectionFactory databaseConnectionFactory = this.createInitializedDatabaseConnectionFactory();
		FileUsageRepository fileUsageRepository = new FileUsageRepository();

		try (Connection connection = databaseConnectionFactory.createConnection()) {
			IllegalArgumentException exception = assertThrows(
					IllegalArgumentException.class,
					() -> fileUsageRepository.findById(connection, -1L));

			assertEquals("id must be positive", exception.getMessage());
		}
	}

	@Test
	void shouldThrowExceptionWhenFindByUuidConnectionIsNull() {
		FileUsageRepository fileUsageRepository = new FileUsageRepository();

		NullPointerException exception = assertThrows(
				NullPointerException.class,
				() -> fileUsageRepository.findByUuid(null, FILE_USAGE_UUID));

		assertEquals("connection must not be null", exception.getMessage());
	}

	@Test
	void shouldThrowExceptionWhenFindByUuidUuidIsNull() throws IOException, SQLException {
		DatabaseConnectionFactory databaseConnectionFactory = this.createInitializedDatabaseConnectionFactory();
		FileUsageRepository fileUsageRepository = new FileUsageRepository();

		try (Connection connection = databaseConnectionFactory.createConnection()) {
			NullPointerException exception = assertThrows(
					NullPointerException.class,
					() -> fileUsageRepository.findByUuid(connection, null));

			assertEquals("uuid must not be null", exception.getMessage());
		}
	}

	@Test
	void shouldThrowExceptionWhenMarkDeletedConnectionIsNull() {
		FileUsageRepository fileUsageRepository = new FileUsageRepository();

		NullPointerException exception = assertThrows(
				NullPointerException.class,
				() -> fileUsageRepository.markDeleted(null, 1L, DELETED_AT));

		assertEquals("connection must not be null", exception.getMessage());
	}

	@Test
	void shouldThrowExceptionWhenMarkDeletedIdIsZero() throws IOException, SQLException {
		DatabaseConnectionFactory databaseConnectionFactory = this.createInitializedDatabaseConnectionFactory();
		FileUsageRepository fileUsageRepository = new FileUsageRepository();

		try (Connection connection = databaseConnectionFactory.createConnection()) {
			IllegalArgumentException exception = assertThrows(
					IllegalArgumentException.class,
					() -> fileUsageRepository.markDeleted(connection, 0L, DELETED_AT));

			assertEquals("id must be positive", exception.getMessage());
		}
	}

	@Test
	void shouldThrowExceptionWhenMarkDeletedIdIsNegative() throws IOException, SQLException {
		DatabaseConnectionFactory databaseConnectionFactory = this.createInitializedDatabaseConnectionFactory();
		FileUsageRepository fileUsageRepository = new FileUsageRepository();

		try (Connection connection = databaseConnectionFactory.createConnection()) {
			IllegalArgumentException exception = assertThrows(
					IllegalArgumentException.class,
					() -> fileUsageRepository.markDeleted(connection, -1L, DELETED_AT));

			assertEquals("id must be positive", exception.getMessage());
		}
	}

	@Test
	void shouldThrowExceptionWhenDeletedAtIsNegative() throws IOException, SQLException {
		DatabaseConnectionFactory databaseConnectionFactory = this.createInitializedDatabaseConnectionFactory();
		FileUsageRepository fileUsageRepository = new FileUsageRepository();

		try (Connection connection = databaseConnectionFactory.createConnection()) {
			IllegalArgumentException exception = assertThrows(
					IllegalArgumentException.class,
					() -> fileUsageRepository.markDeleted(connection, 1L, -1L));

			assertEquals("deletedAt must not be negative", exception.getMessage());
		}
	}

	@Test
	void shouldThrowExceptionWhenMarkDeletedFileUsageDoesNotExist() throws IOException, SQLException {
		DatabaseConnectionFactory databaseConnectionFactory = this.createInitializedDatabaseConnectionFactory();
		FileUsageRepository fileUsageRepository = new FileUsageRepository();

		try (Connection connection = databaseConnectionFactory.createConnection()) {
			SQLException exception = assertThrows(
					SQLException.class,
					() -> fileUsageRepository.markDeleted(connection, 999L, DELETED_AT));

			assertEquals("Failed to find deleted file usage: 999", exception.getMessage());
		}
	}

	@Test
	void shouldMarkFileUsageMetadataAsDeleted() throws IOException, SQLException {
		DatabaseConnectionFactory databaseConnectionFactory = this.createInitializedDatabaseConnectionFactory();
		PhysicalFileRepository physicalFileRepository = new PhysicalFileRepository();
		FileUsageRepository fileUsageRepository = new FileUsageRepository();

		try (Connection connection = databaseConnectionFactory.createConnection()) {
			PhysicalFileMetadata physicalFileMetadata = physicalFileRepository.insert(
					connection,
					this.createNewPhysicalFileMetadata());

			FileUsageMetadata insertedMetadata = fileUsageRepository.insert(
					connection,
					this.createNewFileUsageMetadata(physicalFileMetadata.id()));

			FileUsageMetadata deletedMetadata = fileUsageRepository.markDeleted(
					connection,
					insertedMetadata.id(),
					DELETED_AT);

			assertEquals(insertedMetadata.id(), deletedMetadata.id());
			assertEquals(insertedMetadata.uuid(), deletedMetadata.uuid());
			assertEquals(insertedMetadata.physicalFileId(), deletedMetadata.physicalFileId());
			assertEquals(insertedMetadata.usageType(), deletedMetadata.usageType());
			assertEquals(insertedMetadata.ownerType(), deletedMetadata.ownerType());
			assertEquals(insertedMetadata.ownerId(), deletedMetadata.ownerId());
			assertEquals(insertedMetadata.displayName(), deletedMetadata.displayName());
			assertEquals(insertedMetadata.metadataJson(), deletedMetadata.metadataJson());
			assertEquals(FileUsageStatus.DELETED, deletedMetadata.status());
			assertEquals(insertedMetadata.createdAt(), deletedMetadata.createdAt());
			assertEquals(DELETED_AT, deletedMetadata.deletedAt());
		}
	}

	@Test
	void shouldFindDeletedFileUsageMetadataAfterMarkDeleted() throws IOException, SQLException {
		DatabaseConnectionFactory databaseConnectionFactory = this.createInitializedDatabaseConnectionFactory();
		PhysicalFileRepository physicalFileRepository = new PhysicalFileRepository();
		FileUsageRepository fileUsageRepository = new FileUsageRepository();

		try (Connection connection = databaseConnectionFactory.createConnection()) {
			PhysicalFileMetadata physicalFileMetadata = physicalFileRepository.insert(
					connection,
					this.createNewPhysicalFileMetadata());

			FileUsageMetadata insertedMetadata = fileUsageRepository.insert(
					connection,
					this.createNewFileUsageMetadata(physicalFileMetadata.id()));

			fileUsageRepository.markDeleted(
					connection,
					insertedMetadata.id(),
					DELETED_AT);

			FileUsageMetadata foundMetadata = fileUsageRepository.findById(
					connection,
					insertedMetadata.id())
					.orElseThrow();

			assertEquals(FileUsageStatus.DELETED, foundMetadata.status());
			assertEquals(DELETED_AT, foundMetadata.deletedAt());
		}
	}

	@Test
	void shouldThrowExceptionWhenMarkActiveConnectionIsNull() {
		FileUsageRepository fileUsageRepository = new FileUsageRepository();

		NullPointerException exception = assertThrows(
				NullPointerException.class,
				() -> fileUsageRepository.markActive(null, 1L));

		assertEquals("connection must not be null", exception.getMessage());
	}

	@Test
	void shouldThrowExceptionWhenMarkActiveIdIsZero() throws IOException, SQLException {
		DatabaseConnectionFactory databaseConnectionFactory = this.createInitializedDatabaseConnectionFactory();
		FileUsageRepository fileUsageRepository = new FileUsageRepository();

		try (Connection connection = databaseConnectionFactory.createConnection()) {
			IllegalArgumentException exception = assertThrows(
					IllegalArgumentException.class,
					() -> fileUsageRepository.markActive(connection, 0L));

			assertEquals("id must be positive", exception.getMessage());
		}
	}

	@Test
	void shouldThrowExceptionWhenMarkActiveIdIsNegative() throws IOException, SQLException {
		DatabaseConnectionFactory databaseConnectionFactory = this.createInitializedDatabaseConnectionFactory();
		FileUsageRepository fileUsageRepository = new FileUsageRepository();

		try (Connection connection = databaseConnectionFactory.createConnection()) {
			IllegalArgumentException exception = assertThrows(
					IllegalArgumentException.class,
					() -> fileUsageRepository.markActive(connection, -1L));

			assertEquals("id must be positive", exception.getMessage());
		}
	}

	@Test
	void shouldThrowExceptionWhenMarkActiveFileUsageDoesNotExist() throws IOException, SQLException {
		DatabaseConnectionFactory databaseConnectionFactory = this.createInitializedDatabaseConnectionFactory();
		FileUsageRepository fileUsageRepository = new FileUsageRepository();

		try (Connection connection = databaseConnectionFactory.createConnection()) {
			SQLException exception = assertThrows(
					SQLException.class,
					() -> fileUsageRepository.markActive(connection, 999L));

			assertEquals("Failed to find active file usage: 999", exception.getMessage());
		}
	}

	@Test
	void shouldMarkFileUsageMetadataAsActive() throws IOException, SQLException {
		DatabaseConnectionFactory databaseConnectionFactory = this.createInitializedDatabaseConnectionFactory();
		PhysicalFileRepository physicalFileRepository = new PhysicalFileRepository();
		FileUsageRepository fileUsageRepository = new FileUsageRepository();

		try (Connection connection = databaseConnectionFactory.createConnection()) {
			PhysicalFileMetadata physicalFileMetadata = physicalFileRepository.insert(
					connection,
					this.createNewPhysicalFileMetadata());

			FileUsageMetadata insertedMetadata = fileUsageRepository.insert(
					connection,
					this.createNewFileUsageMetadata(physicalFileMetadata.id()));

			FileUsageMetadata deletedMetadata = fileUsageRepository.markDeleted(
					connection,
					insertedMetadata.id(),
					DELETED_AT);

			FileUsageMetadata activeMetadata = fileUsageRepository.markActive(
					connection,
					deletedMetadata.id());

			assertEquals(insertedMetadata.id(), activeMetadata.id());
			assertEquals(insertedMetadata.uuid(), activeMetadata.uuid());
			assertEquals(insertedMetadata.physicalFileId(), activeMetadata.physicalFileId());
			assertEquals(insertedMetadata.usageType(), activeMetadata.usageType());
			assertEquals(insertedMetadata.ownerType(), activeMetadata.ownerType());
			assertEquals(insertedMetadata.ownerId(), activeMetadata.ownerId());
			assertEquals(insertedMetadata.displayName(), activeMetadata.displayName());
			assertEquals(insertedMetadata.metadataJson(), activeMetadata.metadataJson());
			assertEquals(FileUsageStatus.ACTIVE, activeMetadata.status());
			assertEquals(insertedMetadata.createdAt(), activeMetadata.createdAt());
			assertNull(activeMetadata.deletedAt());
		}
	}

	@Test
	void shouldFindActiveFileUsageMetadataAfterMarkActive() throws IOException, SQLException {
		DatabaseConnectionFactory databaseConnectionFactory = this.createInitializedDatabaseConnectionFactory();
		PhysicalFileRepository physicalFileRepository = new PhysicalFileRepository();
		FileUsageRepository fileUsageRepository = new FileUsageRepository();

		try (Connection connection = databaseConnectionFactory.createConnection()) {
			PhysicalFileMetadata physicalFileMetadata = physicalFileRepository.insert(
					connection,
					this.createNewPhysicalFileMetadata());

			FileUsageMetadata insertedMetadata = fileUsageRepository.insert(
					connection,
					this.createNewFileUsageMetadata(physicalFileMetadata.id()));

			FileUsageMetadata deletedMetadata = fileUsageRepository.markDeleted(
					connection,
					insertedMetadata.id(),
					DELETED_AT);

			fileUsageRepository.markActive(connection, deletedMetadata.id());

			FileUsageMetadata foundMetadata = fileUsageRepository.findById(
					connection,
					insertedMetadata.id())
					.orElseThrow();

			assertEquals(FileUsageStatus.ACTIVE, foundMetadata.status());
			assertNull(foundMetadata.deletedAt());
		}
	}

	// ? countActiveByPhysicalFileId

	@Test
	void shouldThrowExceptionWhenCountActiveByPhysicalFileIdConnectionIsNull() {
		FileUsageRepository fileUsageRepository = new FileUsageRepository();

		NullPointerException exception = assertThrows(
				NullPointerException.class,
				() -> fileUsageRepository.countActiveByPhysicalFileId(null, 1L));

		assertEquals("connection must not be null", exception.getMessage());
	}

	@Test
	void shouldThrowExceptionWhenCountActiveByPhysicalFileIdPhysicalFileIdIsZero() throws IOException, SQLException {
		DatabaseConnectionFactory databaseConnectionFactory = this.createInitializedDatabaseConnectionFactory();
		FileUsageRepository fileUsageRepository = new FileUsageRepository();

		try (Connection connection = databaseConnectionFactory.createConnection()) {
			IllegalArgumentException exception = assertThrows(
					IllegalArgumentException.class,
					() -> fileUsageRepository.countActiveByPhysicalFileId(connection, 0L));

			assertEquals("physicalFileId must be positive", exception.getMessage());
		}
	}

	@Test
	void shouldThrowExceptionWhenCountActiveByPhysicalFileIdPhysicalFileIdIsNegative()
			throws IOException, SQLException {
		DatabaseConnectionFactory databaseConnectionFactory = this.createInitializedDatabaseConnectionFactory();
		FileUsageRepository fileUsageRepository = new FileUsageRepository();

		try (Connection connection = databaseConnectionFactory.createConnection()) {
			IllegalArgumentException exception = assertThrows(
					IllegalArgumentException.class,
					() -> fileUsageRepository.countActiveByPhysicalFileId(connection, -1L));

			assertEquals("physicalFileId must be positive", exception.getMessage());
		}
	}

	@Test
	void shouldReturnZeroWhenPhysicalFileHasNoActiveFileUsages() throws IOException, SQLException {
		DatabaseConnectionFactory databaseConnectionFactory = this.createInitializedDatabaseConnectionFactory();
		PhysicalFileRepository physicalFileRepository = new PhysicalFileRepository();
		FileUsageRepository fileUsageRepository = new FileUsageRepository();

		try (Connection connection = databaseConnectionFactory.createConnection()) {
			PhysicalFileMetadata physicalFileMetadata = physicalFileRepository.insert(
					connection,
					this.createNewPhysicalFileMetadata());

			long activeUsagesCount = fileUsageRepository.countActiveByPhysicalFileId(
					connection,
					physicalFileMetadata.id());

			assertEquals(0L, activeUsagesCount);
		}
	}

	@Test
	void shouldCountActiveFileUsagesByPhysicalFileId() throws IOException, SQLException {
		DatabaseConnectionFactory databaseConnectionFactory = this.createInitializedDatabaseConnectionFactory();
		PhysicalFileRepository physicalFileRepository = new PhysicalFileRepository();
		FileUsageRepository fileUsageRepository = new FileUsageRepository();

		try (Connection connection = databaseConnectionFactory.createConnection()) {
			PhysicalFileMetadata physicalFileMetadata = physicalFileRepository.insert(
					connection,
					this.createNewPhysicalFileMetadata());

			fileUsageRepository.insert(
					connection,
					this.createNewFileUsageMetadata(physicalFileMetadata.id()));

			FileUsageMetadata secondFileUsageMetadata = FileUsageMetadata.newUsage(
					UUID.fromString("22222222-2222-2222-2222-222222222222"),
					physicalFileMetadata.id(),
					USAGE_TYPE,
					OWNER_TYPE,
					OWNER_ID,
					DISPLAY_NAME,
					METADATA_JSON,
					CREATED_AT);

			fileUsageRepository.insert(connection, secondFileUsageMetadata);

			long activeUsagesCount = fileUsageRepository.countActiveByPhysicalFileId(
					connection,
					physicalFileMetadata.id());

			assertEquals(2L, activeUsagesCount);
		}
	}

	@Test
	void shouldNotCountDeletedFileUsagesByPhysicalFileId() throws IOException, SQLException {
		DatabaseConnectionFactory databaseConnectionFactory = this.createInitializedDatabaseConnectionFactory();
		PhysicalFileRepository physicalFileRepository = new PhysicalFileRepository();
		FileUsageRepository fileUsageRepository = new FileUsageRepository();

		try (Connection connection = databaseConnectionFactory.createConnection()) {
			PhysicalFileMetadata physicalFileMetadata = physicalFileRepository.insert(
					connection,
					this.createNewPhysicalFileMetadata());

			FileUsageMetadata insertedMetadata = fileUsageRepository.insert(
					connection,
					this.createNewFileUsageMetadata(physicalFileMetadata.id()));

			fileUsageRepository.markDeleted(
					connection,
					insertedMetadata.id(),
					DELETED_AT);

			long activeUsagesCount = fileUsageRepository.countActiveByPhysicalFileId(
					connection,
					physicalFileMetadata.id());

			assertEquals(0L, activeUsagesCount);
		}
	}

	// ? helpers

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
		MetadataSchemaInitializer metadataSchemaInitializer = new MetadataSchemaInitializer(databaseConnectionFactory);

		metadataSchemaInitializer.initialize();

		return databaseConnectionFactory;
	}

	private StorageDirectories createInitializedStorageDirectories() throws IOException {
		PocketFilesConfig config = PocketFilesConfig.builder()
				.baseDirectory(this.tempDir.resolve("pocket-files"))
				.build();

		StorageDirectories storageDirectories = new StorageDirectories(config);

		StorageInitializer storageInitializer = new StorageInitializer(storageDirectories);
		storageInitializer.initialize();

		return storageDirectories;
	}
}
