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
import com.chyvacheck.pocketfiles.storage.StorageDirectories;
import com.chyvacheck.pocketfiles.storage.StorageInitializer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileOpenServiceTest {
	private static final UUID PHYSICAL_FILE_UUID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

	private static final UUID FILE_USAGE_UUID = UUID.fromString("11111111-1111-1111-1111-111111111111");

	private static final UUID MISSING_FILE_USAGE_UUID = UUID.fromString("22222222-2222-2222-2222-222222222222");

	private static final String ORIGINAL_NAME = "photo.png";

	private static final String RELATIVE_PATH = "2026/01/02/550e8400-e29b-41d4-a716-446655440000.png";

	private static final String MIME_TYPE = "image/png";

	private static final String EXTENSION = "png";

	private static final String CONTENT = "Hello";

	private static final long SIZE_BYTES = 5L;

	private static final String SHA256 = "185f8db32271fe25f561a6fc938b2e264306ec304eda518007d1764826381969";

	private static final long CREATED_AT = 1760000000000L;

	@TempDir
	Path tempDir;

	@Test
	void shouldThrowExceptionWhenDatabaseConnectionFactoryIsNull() {
		NullPointerException exception = assertThrows(
				NullPointerException.class,
				() -> new FileOpenService(
						null,
						new FileUsageRepository(),
						new PhysicalFileRepository(),
						this.createStorageDirectories()));

		assertEquals("databaseConnectionFactory must not be null", exception.getMessage());
	}

	@Test
	void shouldThrowExceptionWhenFileUsageRepositoryIsNull() {
		NullPointerException exception = assertThrows(
				NullPointerException.class,
				() -> new FileOpenService(
						this.createDatabaseConnectionFactory(),
						null,
						new PhysicalFileRepository(),
						this.createStorageDirectories()));

		assertEquals("fileUsageRepository must not be null", exception.getMessage());
	}

	@Test
	void shouldThrowExceptionWhenPhysicalFileRepositoryIsNull() {
		NullPointerException exception = assertThrows(
				NullPointerException.class,
				() -> new FileOpenService(
						this.createDatabaseConnectionFactory(),
						new FileUsageRepository(),
						null,
						this.createStorageDirectories()));

		assertEquals("physicalFileRepository must not be null", exception.getMessage());
	}

	@Test
	void shouldThrowExceptionWhenStorageDirectoriesIsNull() {
		NullPointerException exception = assertThrows(
				NullPointerException.class,
				() -> new FileOpenService(
						this.createDatabaseConnectionFactory(),
						new FileUsageRepository(),
						new PhysicalFileRepository(),
						null));

		assertEquals("storageDirectories must not be null", exception.getMessage());
	}

	@Test
	void shouldThrowExceptionWhenFileUsageUuidIsNull() throws IOException, SQLException {
		TestContext context = this.createTestContext();

		NullPointerException exception = assertThrows(
				NullPointerException.class,
				() -> context.fileOpenService().open(null));

		assertEquals("fileUsageUuid must not be null", exception.getMessage());
	}

	@Test
	void shouldOpenExistingFile() throws IOException, SQLException {
		TestContext context = this.createTestContext();

		this.createStoredFileOnDisk(context.storageDirectories());
		InsertedMetadata insertedMetadata = this.insertMetadata(
				context.databaseConnectionFactory(),
				PhysicalFileStatus.ACTIVE,
				FileUsageStatus.ACTIVE);

		OpenFileResult result = context.fileOpenService().open(insertedMetadata.fileUsageMetadata().uuid());

		assertEquals(insertedMetadata.fileUsageMetadata(), result.fileUsageMetadata());
		assertEquals(insertedMetadata.physicalFileMetadata(), result.physicalFileMetadata());
		assertEquals(
				context.storageDirectories().resolveFilePath(RELATIVE_PATH),
				result.absolutePath());

		assertTrue(Files.isRegularFile(result.absolutePath()));
		assertEquals(CONTENT, Files.readString(result.absolutePath()));
	}

	@Test
	void shouldThrowExceptionWhenFileUsageIsNotFound() throws IOException, SQLException {
		TestContext context = this.createTestContext();

		IllegalArgumentException exception = assertThrows(
				IllegalArgumentException.class,
				() -> context.fileOpenService().open(MISSING_FILE_USAGE_UUID));

		assertEquals("File usage not found: " + MISSING_FILE_USAGE_UUID, exception.getMessage());
	}

	@Test
	void shouldThrowExceptionWhenFileUsageIsNotActive() throws IOException, SQLException {
		TestContext context = this.createTestContext();

		this.createStoredFileOnDisk(context.storageDirectories());
		InsertedMetadata insertedMetadata = this.insertMetadata(
				context.databaseConnectionFactory(),
				PhysicalFileStatus.ACTIVE,
				FileUsageStatus.DELETED);

		IllegalArgumentException exception = assertThrows(
				IllegalArgumentException.class,
				() -> context.fileOpenService().open(insertedMetadata.fileUsageMetadata().uuid()));

		assertEquals(
				"File usage is not active: " + insertedMetadata.fileUsageMetadata().uuid(),
				exception.getMessage());
	}

	@Test
	void shouldThrowExceptionWhenPhysicalFileIsNotActive() throws IOException, SQLException {
		TestContext context = this.createTestContext();

		this.createStoredFileOnDisk(context.storageDirectories());
		InsertedMetadata insertedMetadata = this.insertMetadata(
				context.databaseConnectionFactory(),
				PhysicalFileStatus.DELETED,
				FileUsageStatus.ACTIVE);

		IllegalArgumentException exception = assertThrows(
				IllegalArgumentException.class,
				() -> context.fileOpenService().open(insertedMetadata.fileUsageMetadata().uuid()));

		assertEquals(
				"Physical file is not active: " + insertedMetadata.physicalFileMetadata().uuid(),
				exception.getMessage());
	}

	@Test
	void shouldThrowExceptionWhenPhysicalFileIsMissingOnDisk() throws IOException, SQLException {
		TestContext context = this.createTestContext();

		InsertedMetadata insertedMetadata = this.insertMetadata(
				context.databaseConnectionFactory(),
				PhysicalFileStatus.ACTIVE,
				FileUsageStatus.ACTIVE);

		NoSuchFileException exception = assertThrows(
				NoSuchFileException.class,
				() -> context.fileOpenService().open(insertedMetadata.fileUsageMetadata().uuid()));

		assertEquals(
				context.storageDirectories().resolveFilePath(RELATIVE_PATH).toString(),
				exception.getMessage());
	}

	private TestContext createTestContext() throws IOException, SQLException {
		StorageDirectories storageDirectories = this.createInitializedStorageDirectories();
		DatabaseConnectionFactory databaseConnectionFactory = new DatabaseConnectionFactory(storageDirectories);

		new MetadataSchemaInitializer(databaseConnectionFactory).initialize();

		FileOpenService fileOpenService = new FileOpenService(
				databaseConnectionFactory,
				new FileUsageRepository(),
				new PhysicalFileRepository(),
				storageDirectories);

		return new TestContext(
				fileOpenService,
				databaseConnectionFactory,
				storageDirectories);
	}

	private InsertedMetadata insertMetadata(
			DatabaseConnectionFactory databaseConnectionFactory,
			PhysicalFileStatus physicalFileStatus,
			FileUsageStatus fileUsageStatus) throws SQLException {
		PhysicalFileRepository physicalFileRepository = new PhysicalFileRepository();
		FileUsageRepository fileUsageRepository = new FileUsageRepository();

		try (Connection connection = databaseConnectionFactory.createConnection()) {
			PhysicalFileMetadata physicalFileMetadata = PhysicalFileMetadata.at(
					null,
					PHYSICAL_FILE_UUID,
					ORIGINAL_NAME,
					RELATIVE_PATH,
					MIME_TYPE,
					EXTENSION,
					SIZE_BYTES,
					SHA256,
					physicalFileStatus,
					CREATED_AT,
					CREATED_AT,
					this.getDeletedAt(physicalFileStatus));

			PhysicalFileMetadata insertedPhysicalFileMetadata = physicalFileRepository.insert(
					connection,
					physicalFileMetadata);

			FileUsageMetadata fileUsageMetadata = FileUsageMetadata.at(
					null,
					FILE_USAGE_UUID,
					insertedPhysicalFileMetadata.id(),
					"invoice_attachment",
					"invoice",
					"777",
					"Invoice January.pdf",
					"""
							{"source":"upload-form","category":"invoice"}
							""",
					fileUsageStatus,
					CREATED_AT,
					this.getDeletedAt(fileUsageStatus));

			FileUsageMetadata insertedFileUsageMetadata = fileUsageRepository.insert(
					connection,
					fileUsageMetadata);

			return new InsertedMetadata(
					insertedPhysicalFileMetadata,
					insertedFileUsageMetadata);
		}
	}

	private Long getDeletedAt(PhysicalFileStatus status) {
		if (status == PhysicalFileStatus.DELETED) {
			return CREATED_AT;
		}

		return null;
	}

	private Long getDeletedAt(FileUsageStatus status) {
		if (status == FileUsageStatus.DELETED) {
			return CREATED_AT;
		}

		return null;
	}

	private void createStoredFileOnDisk(StorageDirectories storageDirectories) throws IOException {
		Path absolutePath = storageDirectories.resolveFilePath(RELATIVE_PATH);

		Files.createDirectories(absolutePath.getParent());
		Files.writeString(absolutePath, CONTENT, StandardCharsets.UTF_8);
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
				.directoryDepth(DirectoryDepth.DAY)
				.build();
	}

	private record TestContext(
			FileOpenService fileOpenService,
			DatabaseConnectionFactory databaseConnectionFactory,
			StorageDirectories storageDirectories) {
	}

	private record InsertedMetadata(
			PhysicalFileMetadata physicalFileMetadata,
			FileUsageMetadata fileUsageMetadata) {
	}
}
