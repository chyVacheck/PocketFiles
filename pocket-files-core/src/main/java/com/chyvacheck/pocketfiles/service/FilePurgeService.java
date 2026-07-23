package com.chyvacheck.pocketfiles.service;

import com.chyvacheck.pocketfiles.metadata.model.FileUsageMetadata;
import com.chyvacheck.pocketfiles.metadata.model.PhysicalFileMetadata;
import com.chyvacheck.pocketfiles.metadata.repository.FileUsageRepository;
import com.chyvacheck.pocketfiles.metadata.repository.PhysicalFileRepository;
import com.chyvacheck.pocketfiles.metadata.status.FileUsageStatus;
import com.chyvacheck.pocketfiles.metadata.status.PhysicalFileStatus;
import com.chyvacheck.pocketfiles.metadata.transaction.MetadataTransactionManager;
import com.chyvacheck.pocketfiles.storage.LocalFileDeleter;
import com.chyvacheck.pocketfiles.storage.StorageDirectories;
import com.chyvacheck.pocketfiles.storage.StoredFile;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Service responsible for permanently deleting physical files from disk.
 *
 * <p>
 * This service only purges files that are no longer used by any active file
 * usage.
 */
public final class FilePurgeService {

	private final MetadataTransactionManager transactionManager;

	private final FileUsageRepository fileUsageRepository;

	private final PhysicalFileRepository physicalFileRepository;

	private final LocalFileDeleter localFileDeleter;

	private final StorageDirectories storageDirectories;

	private final Clock clock;

	/**
	 * Creates a new file purge service.
	 *
	 * @param transactionManager     metadata transaction manager
	 * @param fileUsageRepository    repository for file usage metadata
	 * @param physicalFileRepository repository for physical file metadata
	 * @param localFileDeleter       local file deleter
	 * @param storageDirectories     storage directories
	 * @param clock                  clock used to generate deletion timestamps
	 * @throws NullPointerException if one of the dependencies is null
	 */
	public FilePurgeService(
			MetadataTransactionManager transactionManager,
			FileUsageRepository fileUsageRepository,
			PhysicalFileRepository physicalFileRepository,
			LocalFileDeleter localFileDeleter,
			StorageDirectories storageDirectories,
			Clock clock) {
		this.transactionManager = Objects.requireNonNull(transactionManager, "transactionManager must not be null");
		this.fileUsageRepository = Objects.requireNonNull(fileUsageRepository, "fileUsageRepository must not be null");
		this.physicalFileRepository = Objects.requireNonNull(
				physicalFileRepository,
				"physicalFileRepository must not be null");
		this.localFileDeleter = Objects.requireNonNull(localFileDeleter, "localFileDeleter must not be null");
		this.storageDirectories = Objects.requireNonNull(storageDirectories, "storageDirectories must not be null");
		this.clock = Objects.requireNonNull(clock, "clock must not be null");
	}

	/**
	 * Permanently deletes a physical file from disk.
	 *
	 * <p>
	 * The related file usage must already be deleted before purge can be called.
	 *
	 * @param fileUsageUuid file usage UUID
	 * @return deleted or already deleted physical file metadata
	 * @throws IOException  if file deletion from disk fails
	 * @throws SQLException if metadata lookup or update fails
	 */
	public PhysicalFileMetadata purge(UUID fileUsageUuid) throws IOException, SQLException {
		Objects.requireNonNull(fileUsageUuid, "fileUsageUuid must not be null");

		long deletedAt = Instant.now(this.clock).toEpochMilli();

		PhysicalFileMetadata physicalFileMetadata = this.transactionManager.execute(connection -> {
			FileUsageMetadata fileUsageMetadata = this.findFileUsage(connection, fileUsageUuid);

			if (fileUsageMetadata.status() == FileUsageStatus.ACTIVE) {
				throw new IllegalStateException("File usage must be deleted before purge: " + fileUsageUuid);
			}

			PhysicalFileMetadata foundPhysicalFileMetadata = this.findPhysicalFile(
					connection,
					fileUsageMetadata.physicalFileId());

			if (foundPhysicalFileMetadata.status() == PhysicalFileStatus.DELETED) {
				return foundPhysicalFileMetadata;
			}

			if (foundPhysicalFileMetadata.status() != PhysicalFileStatus.ORPHANED) {
				throw new IllegalStateException(
						"Physical file must be orphaned before purge: " + foundPhysicalFileMetadata.uuid());
			}

			long activeUsagesCount = this.fileUsageRepository.countActiveByPhysicalFileId(
					connection,
					foundPhysicalFileMetadata.id());

			if (activeUsagesCount > 0L) {
				throw new IllegalStateException(
						"Cannot purge physical file with active usages: " + foundPhysicalFileMetadata.uuid());
			}

			return foundPhysicalFileMetadata;
		});

		if (physicalFileMetadata.status() == PhysicalFileStatus.DELETED) {
			return physicalFileMetadata;
		}

		StoredFile storedFile = this.createStoredFileFromPhysicalFileMetadata(physicalFileMetadata);

		this.localFileDeleter.deleteIfExists(storedFile);

		return this.transactionManager.execute(connection -> this.physicalFileRepository.markDeleted(
				connection,
				physicalFileMetadata.id(),
				deletedAt));
	}

	// ? helpers

	private FileUsageMetadata findFileUsage(Connection connection, UUID fileUsageUuid) throws SQLException {
		return this.fileUsageRepository.findByUuid(connection, fileUsageUuid)
				.orElseThrow(() -> new IllegalArgumentException("File usage not found: " + fileUsageUuid));
	}

	private PhysicalFileMetadata findPhysicalFile(Connection connection, long physicalFileId) throws SQLException {
		return this.physicalFileRepository.findById(connection, physicalFileId)
				.orElseThrow(() -> new IllegalArgumentException("Physical file not found: " + physicalFileId));
	}

	private StoredFile createStoredFileFromPhysicalFileMetadata(PhysicalFileMetadata physicalFileMetadata) {
		return StoredFile.at(
				physicalFileMetadata.uuid(),
				physicalFileMetadata.originalName(),
				physicalFileMetadata.relativePath(),
				physicalFileMetadata.extension(),
				physicalFileMetadata.sizeBytes(),
				physicalFileMetadata.sha256(),
				this.storageDirectories.resolveFilePath(physicalFileMetadata.relativePath()));
	}
}
