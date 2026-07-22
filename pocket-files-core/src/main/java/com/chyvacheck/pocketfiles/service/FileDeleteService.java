package com.chyvacheck.pocketfiles.service;

import com.chyvacheck.pocketfiles.metadata.DatabaseConnectionFactory;
import com.chyvacheck.pocketfiles.metadata.model.FileUsageMetadata;
import com.chyvacheck.pocketfiles.metadata.repository.FileUsageRepository;
import com.chyvacheck.pocketfiles.metadata.status.FileUsageStatus;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Service responsible for soft deleting file usages.
 *
 * <p>
 * This service does not delete the physical file from disk. It only marks the
 * logical file usage as deleted.
 */
public final class FileDeleteService {

	private final DatabaseConnectionFactory databaseConnectionFactory;

	private final FileUsageRepository fileUsageRepository;

	private final Clock clock;

	/**
	 * Creates a new file delete service.
	 *
	 * @param databaseConnectionFactory database connection factory
	 * @param fileUsageRepository       repository for file usage metadata
	 * @param clock                     clock used to generate deletion timestamps
	 * @throws NullPointerException if one of the dependencies is null
	 */
	public FileDeleteService(
			DatabaseConnectionFactory databaseConnectionFactory,
			FileUsageRepository fileUsageRepository,
			Clock clock) {
		this.databaseConnectionFactory = Objects.requireNonNull(
				databaseConnectionFactory,
				"databaseConnectionFactory must not be null");
		this.fileUsageRepository = Objects.requireNonNull(fileUsageRepository, "fileUsageRepository must not be null");
		this.clock = Objects.requireNonNull(clock, "clock must not be null");
	}

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

		try (Connection connection = this.databaseConnectionFactory.createConnection()) {
			// Look up the file usage metadata
			FileUsageMetadata fileUsageMetadata = this.findFileUsage(connection, fileUsageUuid);

			// If the file usage is already deleted, return the current metadata
			if (fileUsageMetadata.status() == FileUsageStatus.DELETED) {
				return fileUsageMetadata;
			}

			// Generate the current timestamp
			long deletedAt = Instant.now(this.clock).toEpochMilli();

			// Mark the file usage as deleted
			return this.fileUsageRepository.markDeleted(
					connection,
					fileUsageMetadata.id(),
					deletedAt);
		}
	}

	/**
	 * Looks up the file usage metadata by UUID.
	 *
	 * @param connection    database connection to use
	 * @param fileUsageUuid file usage UUID
	 * @return file usage metadata
	 * @throws IllegalArgumentException if file usage not found
	 * @throws SQLException             if database lookup fails
	 */
	private FileUsageMetadata findFileUsage(Connection connection, UUID fileUsageUuid) throws SQLException {
		return this.fileUsageRepository.findByUuid(connection, fileUsageUuid)
				.orElseThrow(() -> new IllegalArgumentException("File usage not found: " + fileUsageUuid));
	}
}
