package com.chyvacheck.pocketfiles.service;

import com.chyvacheck.pocketfiles.metadata.DatabaseConnectionFactory;
import com.chyvacheck.pocketfiles.metadata.model.FileUsageMetadata;
import com.chyvacheck.pocketfiles.metadata.model.PhysicalFileMetadata;
import com.chyvacheck.pocketfiles.metadata.repository.FileUsageRepository;
import com.chyvacheck.pocketfiles.metadata.repository.PhysicalFileRepository;
import com.chyvacheck.pocketfiles.metadata.status.FileUsageStatus;
import com.chyvacheck.pocketfiles.metadata.status.PhysicalFileStatus;
import com.chyvacheck.pocketfiles.storage.StorageDirectories;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;
import java.util.UUID;

/**
 * Service responsible for resolving existing stored files by file usage UUID.
 */
public final class FileOpenService {
	private final DatabaseConnectionFactory databaseConnectionFactory;

	private final FileUsageRepository fileUsageRepository;

	private final PhysicalFileRepository physicalFileRepository;

	private final StorageDirectories storageDirectories;

	/**
	 * Creates a new file open service.
	 *
	 * @param databaseConnectionFactory database connection factory
	 * @param fileUsageRepository       repository for file usage metadata
	 * @param physicalFileRepository    repository for physical file metadata
	 * @param storageDirectories        storage directory resolver
	 * @throws NullPointerException if one of the dependencies is null
	 */
	public FileOpenService(
			DatabaseConnectionFactory databaseConnectionFactory,
			FileUsageRepository fileUsageRepository,
			PhysicalFileRepository physicalFileRepository,
			StorageDirectories storageDirectories) {
		this.databaseConnectionFactory = Objects.requireNonNull(
				databaseConnectionFactory,
				"databaseConnectionFactory must not be null");
		this.fileUsageRepository = Objects.requireNonNull(fileUsageRepository, "fileUsageRepository must not be null");
		this.physicalFileRepository = Objects.requireNonNull(
				physicalFileRepository,
				"physicalFileRepository must not be null");
		this.storageDirectories = Objects.requireNonNull(storageDirectories, "storageDirectories must not be null");
	}

	/**
	 * Resolves a stored file by file usage UUID.
	 *
	 * @param fileUsageUuid file usage UUID
	 * @return open file result with metadata and absolute file path
	 * @throws SQLException if metadata lookup fails
	 * @throws IOException  if the physical file is missing on disk
	 */
	public OpenFileResult open(UUID fileUsageUuid) throws SQLException, IOException {
		Objects.requireNonNull(fileUsageUuid, "fileUsageUuid must not be null");

		try (Connection connection = this.databaseConnectionFactory.createConnection()) {
			FileUsageMetadata fileUsageMetadata = this.findFileUsage(connection, fileUsageUuid);
			this.requireActiveFileUsage(fileUsageMetadata);

			PhysicalFileMetadata physicalFileMetadata = this.findPhysicalFile(
					connection,
					fileUsageMetadata.physicalFileId());
			this.requireActivePhysicalFile(physicalFileMetadata);

			Path absolutePath = this.storageDirectories.resolveFilePath(physicalFileMetadata.relativePath());
			this.requireExistingFile(absolutePath);

			return OpenFileResult.of(
					fileUsageMetadata,
					physicalFileMetadata,
					absolutePath);
		}
	}

	private FileUsageMetadata findFileUsage(Connection connection, UUID fileUsageUuid) throws SQLException {
		return this.fileUsageRepository.findByUuid(connection, fileUsageUuid)
				.orElseThrow(() -> new IllegalArgumentException("File usage not found: " + fileUsageUuid));
	}

	private PhysicalFileMetadata findPhysicalFile(Connection connection, long physicalFileId) throws SQLException {
		return this.physicalFileRepository.findById(connection, physicalFileId)
				.orElseThrow(() -> new IllegalArgumentException("Physical file not found: " + physicalFileId));
	}

	private void requireActiveFileUsage(FileUsageMetadata fileUsageMetadata) {
		if (fileUsageMetadata.status() != FileUsageStatus.ACTIVE) {
			throw new IllegalArgumentException("File usage is not active: " + fileUsageMetadata.uuid());
		}
	}

	private void requireActivePhysicalFile(PhysicalFileMetadata physicalFileMetadata) {
		if (physicalFileMetadata.status() != PhysicalFileStatus.ACTIVE) {
			throw new IllegalArgumentException("Physical file is not active: " + physicalFileMetadata.uuid());
		}
	}

	private void requireExistingFile(Path absolutePath) throws IOException {
		if (!Files.isRegularFile(absolutePath)) {
			throw new NoSuchFileException(absolutePath.toString());
		}
	}
}
