package com.chyvacheck.pocketfiles.service;

import com.chyvacheck.pocketfiles.metadata.model.FileUsageMetadata;
import com.chyvacheck.pocketfiles.metadata.repository.FileUsageRepository;
import com.chyvacheck.pocketfiles.metadata.repository.PhysicalFileRepository;
import com.chyvacheck.pocketfiles.metadata.status.FileUsageStatus;
import com.chyvacheck.pocketfiles.metadata.transaction.MetadataTransactionManager;

import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Service responsible for soft deleting file usages.
 *
 * <p>
 * This service marks logical file usages as deleted. If the deleted usage was
 * the last active usage of a physical file, the physical file is marked as
 * orphaned.
 */
public final class FileDeleteService {

	private final MetadataTransactionManager transactionManager;

	private final FileUsageRepository fileUsageRepository;

	private final PhysicalFileRepository physicalFileRepository;

	private final Clock clock;

	/**
	 * Creates a new file delete service.
	 *
	 * @param transactionManager     metadata transaction manager
	 * @param fileUsageRepository    repository for file usage metadata
	 * @param physicalFileRepository repository for physical file metadata
	 * @param clock                  clock used to generate deletion timestamps
	 * @throws NullPointerException if one of the dependencies is null
	 */
	public FileDeleteService(
			MetadataTransactionManager transactionManager,
			FileUsageRepository fileUsageRepository,
			PhysicalFileRepository physicalFileRepository,
			Clock clock) {
		this.transactionManager = Objects.requireNonNull(transactionManager, "transactionManager must not be null");
		this.fileUsageRepository = Objects.requireNonNull(fileUsageRepository, "fileUsageRepository must not be null");
		this.physicalFileRepository = Objects.requireNonNull(physicalFileRepository,
				"physicalFileRepository must not be null");
		this.clock = Objects.requireNonNull(clock, "clock must not be null");
	}

	// ? methods

	/**
	 * Soft deletes a file usage by UUID.
	 *
	 * <p>
	 * If the file usage is already deleted, the current metadata is returned
	 * without changing {@code deletedAt}.
	 *
	 * @param fileUsageUuid file usage UUID
	 * @return updated or already deleted file usage metadata
	 * @throws SQLException if metadata lookup or update fails
	 */
	public FileUsageMetadata delete(UUID fileUsageUuid) throws SQLException {
		Objects.requireNonNull(fileUsageUuid, "fileUsageUuid must not be null");

		// Generate the current timestamp
		long deletedAt = Instant.now(this.clock).toEpochMilli();

		return this.transactionManager.execute(connection -> {
			FileUsageMetadata fileUsageMetadata = this.fileUsageRepository.findByUuid(connection, fileUsageUuid)
					.orElseThrow(() -> new IllegalArgumentException("File usage not found: " + fileUsageUuid));

			if (fileUsageMetadata.status() == FileUsageStatus.DELETED) {
				return fileUsageMetadata;
			}

			FileUsageMetadata deletedFileUsageMetadata = this.fileUsageRepository.markDeleted(
					connection,
					fileUsageMetadata.id(),
					deletedAt);

			long activeUsagesCount = this.fileUsageRepository.countActiveByPhysicalFileId(
					connection,
					deletedFileUsageMetadata.physicalFileId());

			if (activeUsagesCount == 0L) {
				this.physicalFileRepository.markOrphaned(
						connection,
						deletedFileUsageMetadata.physicalFileId(),
						deletedAt);
			}

			return deletedFileUsageMetadata;
		});

	}

}
