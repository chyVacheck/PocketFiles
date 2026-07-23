package com.chyvacheck.pocketfiles.service;

import com.chyvacheck.pocketfiles.metadata.model.FileUsageMetadata;
import com.chyvacheck.pocketfiles.metadata.model.PhysicalFileMetadata;
import com.chyvacheck.pocketfiles.metadata.repository.FileUsageRepository;
import com.chyvacheck.pocketfiles.metadata.repository.PhysicalFileRepository;
import com.chyvacheck.pocketfiles.metadata.status.FileUsageStatus;
import com.chyvacheck.pocketfiles.metadata.status.PhysicalFileStatus;
import com.chyvacheck.pocketfiles.metadata.transaction.MetadataTransactionManager;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Service responsible for restoring soft deleted file usages.
 *
 * <p>
 * This service marks logical file usages as active. If the related physical
 * file
 * is orphaned, it is marked as active too.
 */
public final class FileRestoreService {

	private final MetadataTransactionManager transactionManager;

	private final FileUsageRepository fileUsageRepository;

	private final PhysicalFileRepository physicalFileRepository;

	private final Clock clock;

	/**
	 * Creates a new file restore service.
	 *
	 * @param transactionManager     metadata transaction manager
	 * @param fileUsageRepository    repository for file usage metadata
	 * @param physicalFileRepository repository for physical file metadata
	 * @param clock                  clock used to generate status change timestamps
	 * @throws NullPointerException if one of the dependencies is null
	 */
	public FileRestoreService(
			MetadataTransactionManager transactionManager,
			FileUsageRepository fileUsageRepository,
			PhysicalFileRepository physicalFileRepository,
			Clock clock) {
		this.transactionManager = Objects.requireNonNull(transactionManager, "transactionManager must not be null");
		this.fileUsageRepository = Objects.requireNonNull(fileUsageRepository, "fileUsageRepository must not be null");
		this.physicalFileRepository = Objects.requireNonNull(
				physicalFileRepository,
				"physicalFileRepository must not be null");
		this.clock = Objects.requireNonNull(clock, "clock must not be null");
	}

	/**
	 * Restores a soft deleted file usage by UUID.
	 *
	 * <p>
	 * If the file usage is already active, the current metadata is returned
	 * without changes.
	 *
	 * @param fileUsageUuid file usage UUID
	 * @return restored or already active file usage metadata
	 * @throws SQLException if metadata lookup or update fails
	 */
	public FileUsageMetadata restore(UUID fileUsageUuid) throws SQLException {
		Objects.requireNonNull(fileUsageUuid, "fileUsageUuid must not be null");

		long statusChangedAt = Instant.now(this.clock).toEpochMilli();

		return this.transactionManager.execute(connection -> {
			FileUsageMetadata fileUsageMetadata = this.findFileUsage(connection, fileUsageUuid);

			if (fileUsageMetadata.status() == FileUsageStatus.ACTIVE) {
				return fileUsageMetadata;
			}

			PhysicalFileMetadata physicalFileMetadata = this.findPhysicalFile(
					connection,
					fileUsageMetadata.physicalFileId());

			this.requireRestorablePhysicalFile(physicalFileMetadata);

			if (physicalFileMetadata.status() == PhysicalFileStatus.ORPHANED) {
				this.physicalFileRepository.markActive(
						connection,
						physicalFileMetadata.id(),
						statusChangedAt);
			}

			return this.fileUsageRepository.markActive(
					connection,
					fileUsageMetadata.id());
		});
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

	private void requireRestorablePhysicalFile(PhysicalFileMetadata physicalFileMetadata) {
		if (physicalFileMetadata.status() == PhysicalFileStatus.DELETED) {
			throw new IllegalStateException("Physical file is deleted: " + physicalFileMetadata.uuid());
		}

		if (physicalFileMetadata.status() == PhysicalFileStatus.MISSING) {
			throw new IllegalStateException("Physical file is missing: " + physicalFileMetadata.uuid());
		}

		if (physicalFileMetadata.status() == PhysicalFileStatus.FAILED) {
			throw new IllegalStateException("Physical file is failed: " + physicalFileMetadata.uuid());
		}
	}
}
