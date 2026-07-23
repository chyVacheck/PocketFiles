package com.chyvacheck.pocketfiles.service;

import com.chyvacheck.pocketfiles.metadata.DatabaseConnectionFactory;
import com.chyvacheck.pocketfiles.metadata.model.FileUsageMetadata;
import com.chyvacheck.pocketfiles.metadata.repository.FileUsageRepository;
import com.chyvacheck.pocketfiles.metadata.status.FileUsageStatus;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;
import java.util.UUID;

/**
 * Service responsible for restoring soft deleted file usages.
 *
 * <p>
 * This service does not create or move physical files. It only marks the
 * logical file usage as active again.
 */
public final class FileRestoreService {

	private final DatabaseConnectionFactory databaseConnectionFactory;

	private final FileUsageRepository fileUsageRepository;

	/**
	 * Creates a new file restore service.
	 *
	 * @param databaseConnectionFactory database connection factory
	 * @param fileUsageRepository       repository for file usage metadata
	 * @throws NullPointerException if one of the dependencies is null
	 */
	public FileRestoreService(
			DatabaseConnectionFactory databaseConnectionFactory,
			FileUsageRepository fileUsageRepository) {
		this.databaseConnectionFactory = Objects.requireNonNull(
				databaseConnectionFactory,
				"databaseConnectionFactory must not be null");
		this.fileUsageRepository = Objects.requireNonNull(fileUsageRepository, "fileUsageRepository must not be null");
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

		// Find the file usage metadata
		try (Connection connection = this.databaseConnectionFactory.createConnection()) {
			FileUsageMetadata fileUsageMetadata = this.findFileUsage(connection, fileUsageUuid);

			// If the file usage is already active, return the current metadata
			if (fileUsageMetadata.status() == FileUsageStatus.ACTIVE) {
				return fileUsageMetadata;
			}

			// Mark the file usage metadata as active and return the updated metadata
			return this.fileUsageRepository.markActive(
					connection,
					fileUsageMetadata.id());
		}
	}

	// Helpers

	/**
	 * Finds a file usage metadata by UUID.
	 *
	 * @param connection    database connection
	 * @param fileUsageUuid file usage UUID
	 * @return file usage metadata
	 * @throws SQLException if metadata lookup fails
	 */
	private FileUsageMetadata findFileUsage(Connection connection, UUID fileUsageUuid) throws SQLException {
		return this.fileUsageRepository.findByUuid(connection, fileUsageUuid)
				.orElseThrow(() -> new IllegalArgumentException("File usage not found: " + fileUsageUuid));
	}
}
