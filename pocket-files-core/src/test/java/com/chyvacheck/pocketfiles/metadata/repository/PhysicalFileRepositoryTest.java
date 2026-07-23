package com.chyvacheck.pocketfiles.metadata.repository;

import com.chyvacheck.pocketfiles.config.PocketFilesConfig;
import com.chyvacheck.pocketfiles.metadata.DatabaseConnectionFactory;
import com.chyvacheck.pocketfiles.metadata.MetadataSchemaInitializer;
import com.chyvacheck.pocketfiles.metadata.model.PhysicalFileMetadata;
import com.chyvacheck.pocketfiles.metadata.status.PhysicalFileStatus;
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

class PhysicalFileRepositoryTest {

	private static final UUID UUID_VALUE = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

	private static final UUID SECOND_UUID_VALUE = UUID.fromString("11111111-1111-1111-1111-111111111111");

	private static final String ORIGINAL_NAME = "photo.png";

	private static final String RELATIVE_PATH = "2026/01/02/550e8400-e29b-41d4-a716-446655440000.png";

	private static final String MIME_TYPE = "image/png";

	private static final String EXTENSION = "png";

	private static final long SIZE_BYTES = 5L;

	private static final long DIFFERENT_SIZE_BYTES = 10L;

	private static final String SHA256 = "185f8db32271fe25f561a6fc938b2e264306ec304eda518007d1764826381969";

	private static final String DIFFERENT_SHA256 = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";

	private static final long CREATED_AT = 1760000000000L;

	private static final long STATUS_CHANGED_AT = 1760000005000L;

	private static final long ORPHANED_AT = 1760000003000L;

	@TempDir
	Path tempDir;

	// ? insert

	@Test
	void shouldThrowExceptionWhenInsertConnectionIsNull() {
		PhysicalFileRepository repository = new PhysicalFileRepository();

		NullPointerException exception = assertThrows(
				NullPointerException.class,
				() -> repository.insert(null, this.createNewPhysicalFileMetadata()));

		assertEquals("connection must not be null", exception.getMessage());
	}

	@Test
	void shouldThrowExceptionWhenInsertMetadataIsNull() throws IOException, SQLException {
		DatabaseConnectionFactory databaseConnectionFactory = this.createInitializedDatabaseConnectionFactory();
		PhysicalFileRepository repository = new PhysicalFileRepository();

		try (Connection connection = databaseConnectionFactory.createConnection()) {
			NullPointerException exception = assertThrows(
					NullPointerException.class,
					() -> repository.insert(connection, null));

			assertEquals("metadata must not be null", exception.getMessage());
		}
	}

	@Test
	void shouldInsertPhysicalFileMetadata() throws IOException, SQLException {
		DatabaseConnectionFactory databaseConnectionFactory = this.createInitializedDatabaseConnectionFactory();
		PhysicalFileRepository repository = new PhysicalFileRepository();
		PhysicalFileMetadata metadata = this.createNewPhysicalFileMetadata();

		try (Connection connection = databaseConnectionFactory.createConnection()) {
			PhysicalFileMetadata insertedMetadata = repository.insert(connection, metadata);

			assertNotNull(insertedMetadata.id());
			assertEquals(UUID_VALUE, insertedMetadata.uuid());
			assertEquals(ORIGINAL_NAME, insertedMetadata.originalName());
			assertEquals(RELATIVE_PATH, insertedMetadata.relativePath());
			assertEquals(MIME_TYPE, insertedMetadata.mimeType());
			assertEquals(EXTENSION, insertedMetadata.extension());
			assertEquals(SIZE_BYTES, insertedMetadata.sizeBytes());
			assertEquals(SHA256, insertedMetadata.sha256());
			assertEquals(PhysicalFileStatus.ACTIVE, insertedMetadata.status());
			assertEquals(CREATED_AT, insertedMetadata.createdAt());
			assertEquals(CREATED_AT, insertedMetadata.statusChangedAt());
			assertNull(insertedMetadata.deletedAt());
		}
	}

	@Test
	void shouldInsertPhysicalFileMetadataWithNullableFields() throws IOException, SQLException {
		DatabaseConnectionFactory databaseConnectionFactory = this.createInitializedDatabaseConnectionFactory();
		PhysicalFileRepository repository = new PhysicalFileRepository();

		PhysicalFileMetadata metadata = PhysicalFileMetadata.newFile(
				UUID_VALUE,
				"README",
				"2026/01/02/550e8400-e29b-41d4-a716-446655440000",
				null,
				null,
				SIZE_BYTES,
				SHA256,
				CREATED_AT);

		try (Connection connection = databaseConnectionFactory.createConnection()) {
			PhysicalFileMetadata insertedMetadata = repository.insert(connection, metadata);

			assertNotNull(insertedMetadata.id());
			assertNull(insertedMetadata.mimeType());
			assertNull(insertedMetadata.extension());
			assertNull(insertedMetadata.deletedAt());
		}
	}

	// ? findById

	@Test
	void shouldThrowExceptionWhenFindByIdConnectionIsNull() {
		PhysicalFileRepository repository = new PhysicalFileRepository();

		NullPointerException exception = assertThrows(
				NullPointerException.class,
				() -> repository.findById(null, 1L));

		assertEquals("connection must not be null", exception.getMessage());
	}

	@Test
	void shouldThrowExceptionWhenFindByIdIdIsZero() throws IOException, SQLException {
		DatabaseConnectionFactory databaseConnectionFactory = this.createInitializedDatabaseConnectionFactory();
		PhysicalFileRepository repository = new PhysicalFileRepository();

		try (Connection connection = databaseConnectionFactory.createConnection()) {
			IllegalArgumentException exception = assertThrows(
					IllegalArgumentException.class,
					() -> repository.findById(connection, 0L));

			assertEquals("id must be positive", exception.getMessage());
		}
	}

	@Test
	void shouldThrowExceptionWhenFindByIdIdIsNegative() throws IOException, SQLException {
		DatabaseConnectionFactory databaseConnectionFactory = this.createInitializedDatabaseConnectionFactory();
		PhysicalFileRepository repository = new PhysicalFileRepository();

		try (Connection connection = databaseConnectionFactory.createConnection()) {
			IllegalArgumentException exception = assertThrows(
					IllegalArgumentException.class,
					() -> repository.findById(connection, -1L));

			assertEquals("id must be positive", exception.getMessage());
		}
	}

	@Test
	void shouldFindPhysicalFileMetadataById() throws IOException, SQLException {
		DatabaseConnectionFactory databaseConnectionFactory = this.createInitializedDatabaseConnectionFactory();
		PhysicalFileRepository repository = new PhysicalFileRepository();

		try (Connection connection = databaseConnectionFactory.createConnection()) {
			PhysicalFileMetadata insertedMetadata = repository.insert(connection, this.createNewPhysicalFileMetadata());

			Optional<PhysicalFileMetadata> foundMetadata = repository.findById(
					connection,
					insertedMetadata.id());

			assertTrue(foundMetadata.isPresent());
			assertEquals(insertedMetadata, foundMetadata.get());
		}
	}

	@Test
	void shouldReturnEmptyWhenPhysicalFileMetadataByIdDoesNotExist() throws IOException, SQLException {
		DatabaseConnectionFactory databaseConnectionFactory = this.createInitializedDatabaseConnectionFactory();
		PhysicalFileRepository repository = new PhysicalFileRepository();

		try (Connection connection = databaseConnectionFactory.createConnection()) {
			Optional<PhysicalFileMetadata> foundMetadata = repository.findById(connection, 999L);

			assertTrue(foundMetadata.isEmpty());
		}
	}

	// ? findByUuid

	@Test
	void shouldThrowExceptionWhenFindByUuidConnectionIsNull() {
		PhysicalFileRepository repository = new PhysicalFileRepository();

		NullPointerException exception = assertThrows(
				NullPointerException.class,
				() -> repository.findByUuid(null, UUID_VALUE));

		assertEquals("connection must not be null", exception.getMessage());
	}

	@Test
	void shouldThrowExceptionWhenFindByUuidUuidIsNull() throws IOException, SQLException {
		DatabaseConnectionFactory databaseConnectionFactory = this.createInitializedDatabaseConnectionFactory();
		PhysicalFileRepository repository = new PhysicalFileRepository();

		try (Connection connection = databaseConnectionFactory.createConnection()) {
			NullPointerException exception = assertThrows(
					NullPointerException.class,
					() -> repository.findByUuid(connection, null));

			assertEquals("uuid must not be null", exception.getMessage());
		}
	}

	@Test
	void shouldFindPhysicalFileMetadataByUuid() throws IOException, SQLException {
		DatabaseConnectionFactory databaseConnectionFactory = this.createInitializedDatabaseConnectionFactory();
		PhysicalFileRepository repository = new PhysicalFileRepository();

		try (Connection connection = databaseConnectionFactory.createConnection()) {
			PhysicalFileMetadata insertedMetadata = repository.insert(connection, this.createNewPhysicalFileMetadata());

			Optional<PhysicalFileMetadata> foundMetadata = repository.findByUuid(
					connection,
					insertedMetadata.uuid());

			assertTrue(foundMetadata.isPresent());
			assertEquals(insertedMetadata, foundMetadata.get());
		}
	}

	@Test
	void shouldReturnEmptyWhenPhysicalFileMetadataByUuidDoesNotExist() throws IOException, SQLException {
		DatabaseConnectionFactory databaseConnectionFactory = this.createInitializedDatabaseConnectionFactory();
		PhysicalFileRepository repository = new PhysicalFileRepository();

		try (Connection connection = databaseConnectionFactory.createConnection()) {
			Optional<PhysicalFileMetadata> foundMetadata = repository.findByUuid(
					connection,
					SECOND_UUID_VALUE);

			assertTrue(foundMetadata.isEmpty());
		}
	}

	// ? findBySha256AndSizeBytes

	@Test
	void shouldThrowExceptionWhenFindBySha256AndSizeBytesConnectionIsNull() {
		PhysicalFileRepository repository = new PhysicalFileRepository();

		NullPointerException exception = assertThrows(
				NullPointerException.class,
				() -> repository.findBySha256AndSizeBytes(null, SHA256, SIZE_BYTES));

		assertEquals("connection must not be null", exception.getMessage());
	}

	@Test
	void shouldThrowExceptionWhenFindBySha256AndSizeBytesSha256IsNull() throws IOException, SQLException {
		DatabaseConnectionFactory databaseConnectionFactory = this.createInitializedDatabaseConnectionFactory();
		PhysicalFileRepository repository = new PhysicalFileRepository();

		try (Connection connection = databaseConnectionFactory.createConnection()) {
			NullPointerException exception = assertThrows(
					NullPointerException.class,
					() -> repository.findBySha256AndSizeBytes(connection, null, SIZE_BYTES));

			assertEquals("sha256 must not be null", exception.getMessage());
		}
	}

	@Test
	void shouldThrowExceptionWhenFindBySha256AndSizeBytesSha256IsBlank() throws IOException, SQLException {
		DatabaseConnectionFactory databaseConnectionFactory = this.createInitializedDatabaseConnectionFactory();
		PhysicalFileRepository repository = new PhysicalFileRepository();

		try (Connection connection = databaseConnectionFactory.createConnection()) {
			IllegalArgumentException exception = assertThrows(
					IllegalArgumentException.class,
					() -> repository.findBySha256AndSizeBytes(connection, "   ", SIZE_BYTES));

			assertEquals("sha256 must not be blank", exception.getMessage());
		}
	}

	@Test
	void shouldThrowExceptionWhenFindBySha256AndSizeBytesSizeBytesIsNegative() throws IOException, SQLException {
		DatabaseConnectionFactory databaseConnectionFactory = this.createInitializedDatabaseConnectionFactory();
		PhysicalFileRepository repository = new PhysicalFileRepository();

		try (Connection connection = databaseConnectionFactory.createConnection()) {
			IllegalArgumentException exception = assertThrows(
					IllegalArgumentException.class,
					() -> repository.findBySha256AndSizeBytes(connection, SHA256, -1L));

			assertEquals("sizeBytes must not be negative", exception.getMessage());
		}
	}

	@Test
	void shouldFindActivePhysicalFileMetadataBySha256AndSizeBytes() throws IOException, SQLException {
		DatabaseConnectionFactory databaseConnectionFactory = this.createInitializedDatabaseConnectionFactory();
		PhysicalFileRepository repository = new PhysicalFileRepository();

		try (Connection connection = databaseConnectionFactory.createConnection()) {
			PhysicalFileMetadata insertedMetadata = repository.insert(
					connection,
					this.createNewPhysicalFileMetadata());

			Optional<PhysicalFileMetadata> foundMetadata = repository.findBySha256AndSizeBytes(
					connection,
					SHA256,
					SIZE_BYTES);

			assertTrue(foundMetadata.isPresent());
			assertEquals(insertedMetadata, foundMetadata.get());
		}
	}

	@Test
	void shouldFindOrphanedPhysicalFileMetadataBySha256AndSizeBytes() throws IOException, SQLException {
		DatabaseConnectionFactory databaseConnectionFactory = this.createInitializedDatabaseConnectionFactory();
		PhysicalFileRepository repository = new PhysicalFileRepository();

		try (Connection connection = databaseConnectionFactory.createConnection()) {
			PhysicalFileMetadata insertedMetadata = repository.insert(
					connection,
					this.createOrphanedPhysicalFileMetadata());

			Optional<PhysicalFileMetadata> foundMetadata = repository.findBySha256AndSizeBytes(
					connection,
					SHA256,
					SIZE_BYTES);

			assertTrue(foundMetadata.isPresent());
			assertEquals(insertedMetadata, foundMetadata.get());
		}
	}

	@Test
	void shouldNotFindDeletedPhysicalFileMetadataBySha256AndSizeBytes() throws IOException, SQLException {
		DatabaseConnectionFactory databaseConnectionFactory = this.createInitializedDatabaseConnectionFactory();
		PhysicalFileRepository repository = new PhysicalFileRepository();

		try (Connection connection = databaseConnectionFactory.createConnection()) {
			repository.insert(
					connection,
					this.createPhysicalFileMetadataWithStatus(PhysicalFileStatus.DELETED));

			Optional<PhysicalFileMetadata> foundMetadata = repository.findBySha256AndSizeBytes(
					connection,
					SHA256,
					SIZE_BYTES);

			assertTrue(foundMetadata.isEmpty());
		}
	}

	@Test
	void shouldNotFindMissingPhysicalFileMetadataBySha256AndSizeBytes() throws IOException, SQLException {
		DatabaseConnectionFactory databaseConnectionFactory = this.createInitializedDatabaseConnectionFactory();
		PhysicalFileRepository repository = new PhysicalFileRepository();

		try (Connection connection = databaseConnectionFactory.createConnection()) {
			repository.insert(
					connection,
					this.createPhysicalFileMetadataWithStatus(PhysicalFileStatus.MISSING));

			Optional<PhysicalFileMetadata> foundMetadata = repository.findBySha256AndSizeBytes(
					connection,
					SHA256,
					SIZE_BYTES);

			assertTrue(foundMetadata.isEmpty());
		}
	}

	@Test
	void shouldNotFindFailedPhysicalFileMetadataBySha256AndSizeBytes() throws IOException, SQLException {
		DatabaseConnectionFactory databaseConnectionFactory = this.createInitializedDatabaseConnectionFactory();
		PhysicalFileRepository repository = new PhysicalFileRepository();

		try (Connection connection = databaseConnectionFactory.createConnection()) {
			repository.insert(
					connection,
					this.createPhysicalFileMetadataWithStatus(PhysicalFileStatus.FAILED));

			Optional<PhysicalFileMetadata> foundMetadata = repository.findBySha256AndSizeBytes(
					connection,
					SHA256,
					SIZE_BYTES);

			assertTrue(foundMetadata.isEmpty());
		}
	}

	@Test
	void shouldNotFindPendingPhysicalFileMetadataBySha256AndSizeBytes() throws IOException, SQLException {
		DatabaseConnectionFactory databaseConnectionFactory = this.createInitializedDatabaseConnectionFactory();
		PhysicalFileRepository repository = new PhysicalFileRepository();

		try (Connection connection = databaseConnectionFactory.createConnection()) {
			repository.insert(
					connection,
					this.createPhysicalFileMetadataWithStatus(PhysicalFileStatus.PENDING));

			Optional<PhysicalFileMetadata> foundMetadata = repository.findBySha256AndSizeBytes(
					connection,
					SHA256,
					SIZE_BYTES);

			assertTrue(foundMetadata.isEmpty());
		}
	}

	@Test
	void shouldReturnEmptyWhenSha256DoesNotMatch() throws IOException, SQLException {
		DatabaseConnectionFactory databaseConnectionFactory = this.createInitializedDatabaseConnectionFactory();
		PhysicalFileRepository repository = new PhysicalFileRepository();

		try (Connection connection = databaseConnectionFactory.createConnection()) {
			repository.insert(connection, this.createNewPhysicalFileMetadata());

			Optional<PhysicalFileMetadata> foundMetadata = repository.findBySha256AndSizeBytes(
					connection,
					DIFFERENT_SHA256,
					SIZE_BYTES);

			assertTrue(foundMetadata.isEmpty());
		}
	}

	@Test
	void shouldReturnEmptyWhenSizeBytesDoesNotMatch() throws IOException, SQLException {
		DatabaseConnectionFactory databaseConnectionFactory = this.createInitializedDatabaseConnectionFactory();
		PhysicalFileRepository repository = new PhysicalFileRepository();

		try (Connection connection = databaseConnectionFactory.createConnection()) {
			repository.insert(connection, this.createNewPhysicalFileMetadata());

			Optional<PhysicalFileMetadata> foundMetadata = repository.findBySha256AndSizeBytes(
					connection,
					SHA256,
					DIFFERENT_SIZE_BYTES);

			assertTrue(foundMetadata.isEmpty());
		}
	}

	// ? helpers

	private PhysicalFileMetadata createOrphanedPhysicalFileMetadata() {
		return PhysicalFileMetadata.at(
				null,
				UUID_VALUE,
				ORIGINAL_NAME,
				RELATIVE_PATH,
				MIME_TYPE,
				EXTENSION,
				SIZE_BYTES,
				SHA256,
				PhysicalFileStatus.ORPHANED,
				CREATED_AT,
				ORPHANED_AT,
				null);
	}

	private PhysicalFileMetadata createNewPhysicalFileMetadata() {
		return PhysicalFileMetadata.newFile(
				UUID_VALUE,
				ORIGINAL_NAME,
				RELATIVE_PATH,
				MIME_TYPE,
				EXTENSION,
				SIZE_BYTES,
				SHA256,
				CREATED_AT);
	}

	private PhysicalFileMetadata createPhysicalFileMetadataWithStatus(PhysicalFileStatus status) {
		return PhysicalFileMetadata.at(
				null,
				UUID_VALUE,
				ORIGINAL_NAME,
				RELATIVE_PATH,
				MIME_TYPE,
				EXTENSION,
				SIZE_BYTES,
				SHA256,
				status,
				CREATED_AT,
				STATUS_CHANGED_AT,
				status == PhysicalFileStatus.DELETED ? STATUS_CHANGED_AT : null);
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
