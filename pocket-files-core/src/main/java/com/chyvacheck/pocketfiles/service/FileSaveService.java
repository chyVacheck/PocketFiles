package com.chyvacheck.pocketfiles.service;

import com.chyvacheck.pocketfiles.metadata.model.FileUsageMetadata;
import com.chyvacheck.pocketfiles.metadata.model.PhysicalFileMetadata;
import com.chyvacheck.pocketfiles.metadata.repository.FileUsageRepository;
import com.chyvacheck.pocketfiles.metadata.repository.PhysicalFileRepository;
import com.chyvacheck.pocketfiles.metadata.transaction.MetadataTransactionManager;
import com.chyvacheck.pocketfiles.storage.LocalFileDeleter;
import com.chyvacheck.pocketfiles.storage.LocalFileStorage;
import com.chyvacheck.pocketfiles.storage.StoredFile;

import java.io.IOException;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.UUID;

/**
 * Service responsible for saving files through PocketFiles.
 *
 * <p>
 * This service coordinates local file storage and SQLite metadata persistence.
 * It saves the file on disk first and then creates the corresponding metadata
 * records in a single database transaction.
 */
public final class FileSaveService {
	private final LocalFileStorage localFileStorage;

	private final LocalFileDeleter localFileDeleter;

	private final MetadataTransactionManager transactionManager;

	private final PhysicalFileRepository physicalFileRepository;

	private final FileUsageRepository fileUsageRepository;

	private final Clock clock;

	/**
	 * Creates a new file save service.
	 *
	 * @param localFileStorage       local storage used to write files to disk
	 * @param localFileDeleter       local storage component used for cleanup
	 * @param transactionManager     metadata transaction manager
	 * @param physicalFileRepository repository for physical file metadata
	 * @param fileUsageRepository    repository for file usage metadata
	 * @param clock                  clock used to generate stable timestamps
	 * @throws NullPointerException if one of the dependencies is null
	 */
	public FileSaveService(
			LocalFileStorage localFileStorage,
			LocalFileDeleter localFileDeleter,
			MetadataTransactionManager transactionManager,
			PhysicalFileRepository physicalFileRepository,
			FileUsageRepository fileUsageRepository,
			Clock clock) {
		this.localFileStorage = Objects.requireNonNull(localFileStorage, "localFileStorage must not be null");
		this.localFileDeleter = Objects.requireNonNull(localFileDeleter, "localFileDeleter must not be null");
		this.transactionManager = Objects.requireNonNull(transactionManager, "transactionManager must not be null");
		this.physicalFileRepository = Objects.requireNonNull(
				physicalFileRepository,
				"physicalFileRepository must not be null");
		this.fileUsageRepository = Objects.requireNonNull(fileUsageRepository, "fileUsageRepository must not be null");
		this.clock = Objects.requireNonNull(clock, "clock must not be null");
	}

	/**
	 * Saves a file and creates metadata records for it.
	 *
	 * <p>
	 * If metadata persistence fails after the file was written to disk, the saved
	 * file is deleted as cleanup.
	 *
	 * @param command save file command
	 * @return result containing stored file and created metadata records
	 * @throws IOException  if local file storage or cleanup fails
	 * @throws SQLException if metadata persistence fails
	 */
	public SaveFileResult save(SaveFileCommand command) throws IOException, SQLException {
		Objects.requireNonNull(command, "command must not be null");

		Instant now = Instant.now(this.clock);
		long createdAt = now.toEpochMilli();
		LocalDateTime dateTime = LocalDateTime.ofInstant(now, ZoneOffset.UTC);

		StoredFile storedFile = this.localFileStorage.save(
				command.inputStream(),
				command.originalName(),
				dateTime);

		try {
			return this.saveMetadata(command, storedFile, createdAt);
		} catch (SQLException exception) {
			this.localFileDeleter.deleteIfExists(storedFile);

			throw exception;
		}
	}

	/**
	 * Saves metadata records for the stored file inside a database transaction.
	 *
	 * @param command    original save command
	 * @param storedFile stored file from local storage
	 * @param createdAt  creation timestamp in epoch milliseconds
	 * @return full save result
	 * @throws SQLException if metadata persistence fails
	 */
	private SaveFileResult saveMetadata(
			SaveFileCommand command,
			StoredFile storedFile,
			long createdAt) throws SQLException {
		return this.transactionManager.execute(connection -> {
			// Create physical file metadata
			PhysicalFileMetadata physicalFileMetadata = PhysicalFileMetadata.newFile(
					storedFile.uuid(),
					storedFile.originalName(),
					storedFile.relativePath(),
					command.mimeType(),
					storedFile.extension(),
					storedFile.sizeBytes(),
					storedFile.sha256(),
					createdAt);

			// Insert physical file metadata into the database
			PhysicalFileMetadata insertedPhysicalFileMetadata = this.physicalFileRepository.insert(connection,
					physicalFileMetadata);

			// Create file usage metadata
			FileUsageMetadata fileUsageMetadata = FileUsageMetadata.newUsage(
					UUID.randomUUID(),
					insertedPhysicalFileMetadata.id(),
					command.usageType(),
					command.ownerType(),
					command.ownerId(),
					command.displayName(),
					command.metadataJson(),
					createdAt);

			// Insert file usage metadata into the database
			FileUsageMetadata insertedFileUsageMetadata = this.fileUsageRepository.insert(connection,
					fileUsageMetadata);

			// Return the save result
			return SaveFileResult.of(
					storedFile,
					insertedPhysicalFileMetadata,
					insertedFileUsageMetadata);
		});
	}
}
