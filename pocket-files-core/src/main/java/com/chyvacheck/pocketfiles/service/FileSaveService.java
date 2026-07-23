package com.chyvacheck.pocketfiles.service;

import com.chyvacheck.pocketfiles.metadata.model.FileUsageMetadata;
import com.chyvacheck.pocketfiles.metadata.model.PhysicalFileMetadata;
import com.chyvacheck.pocketfiles.metadata.repository.FileUsageRepository;
import com.chyvacheck.pocketfiles.metadata.repository.PhysicalFileRepository;
import com.chyvacheck.pocketfiles.metadata.status.PhysicalFileStatus;
import com.chyvacheck.pocketfiles.metadata.transaction.MetadataTransactionManager;
import com.chyvacheck.pocketfiles.storage.LocalFileDeleter;
import com.chyvacheck.pocketfiles.storage.LocalFileStorage;
import com.chyvacheck.pocketfiles.storage.StagedFile;
import com.chyvacheck.pocketfiles.storage.StorageDirectories;
import com.chyvacheck.pocketfiles.storage.StoredFile;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Service responsible for saving files through PocketFiles.
 *
 * <p>
 * This service coordinates local file staging, deduplication, final file
 * storage
 * and SQLite metadata persistence.
 */
public final class FileSaveService {

	private final LocalFileStorage localFileStorage;

	private final LocalFileDeleter localFileDeleter;

	private final MetadataTransactionManager transactionManager;

	private final PhysicalFileRepository physicalFileRepository;

	private final FileUsageRepository fileUsageRepository;

	private final StorageDirectories storageDirectories;

	private final Clock clock;

	/**
	 * Creates a new file save service.
	 *
	 * @param localFileStorage       local storage used to write files to disk
	 * @param localFileDeleter       local storage component used for cleanup
	 * @param transactionManager     metadata transaction manager
	 * @param physicalFileRepository repository for physical file metadata
	 * @param fileUsageRepository    repository for file usage metadata
	 * @param storageDirectories     storage directories used to resolve stored file
	 *                               paths
	 * @param clock                  clock used to generate stable timestamps
	 * @throws NullPointerException if one of the dependencies is null
	 */
	public FileSaveService(
			LocalFileStorage localFileStorage,
			LocalFileDeleter localFileDeleter,
			MetadataTransactionManager transactionManager,
			PhysicalFileRepository physicalFileRepository,
			FileUsageRepository fileUsageRepository,
			StorageDirectories storageDirectories,
			Clock clock) {
		this.localFileStorage = Objects.requireNonNull(localFileStorage, "localFileStorage must not be null");
		this.localFileDeleter = Objects.requireNonNull(localFileDeleter, "localFileDeleter must not be null");
		this.transactionManager = Objects.requireNonNull(transactionManager, "transactionManager must not be null");
		this.physicalFileRepository = Objects.requireNonNull(
				physicalFileRepository,
				"physicalFileRepository must not be null");
		this.fileUsageRepository = Objects.requireNonNull(fileUsageRepository, "fileUsageRepository must not be null");
		this.storageDirectories = Objects.requireNonNull(storageDirectories, "storageDirectories must not be null");
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

		StagedFile stagedFile = this.localFileStorage.stage(
				command.inputStream(),
				command.originalName());

		AtomicReference<StoredFile> storedFileForCleanup = new AtomicReference<>();

		try {
			SaveFileResult result = this.transactionManager.execute(connection -> {
				Optional<PhysicalFileMetadata> existingPhysicalFile = this.physicalFileRepository
						.findBySha256AndSizeBytes(
								connection,
								stagedFile.sha256(),
								stagedFile.sizeBytes());

				// If the file already exists, update the usage metadata
				if (existingPhysicalFile.isPresent()) {
					return this.saveUsageForExistingPhysicalFile(
							connection,
							command,
							existingPhysicalFile.get(),
							createdAt);
				}

				// If the file does not exist, create a new physical file and usage metadata

				StoredFile storedFile = this.localFileStorage.store(stagedFile, dateTime);
				storedFileForCleanup.set(storedFile);

				return this.saveNewPhysicalFileAndUsage(
						connection,
						command,
						storedFile,
						createdAt);
			});

			this.localFileDeleter.deleteIfExists(stagedFile);

			return result;
		} catch (SQLException exception) {
			this.cleanupAfterFailedSave(stagedFile, storedFileForCleanup.get(), exception);

			throw exception;
		}
	}

	/**
	 * Saves a new file usage metadata record for an existing physical file.
	 *
	 * @param connection           database connection
	 * @param command              save file command
	 * @param physicalFileMetadata existing physical file metadata
	 * @param createdAt            timestamp of the record
	 * @return result containing stored file and created metadata records
	 * @throws SQLException if metadata persistence fails
	 */
	private SaveFileResult saveUsageForExistingPhysicalFile(
			Connection connection,
			SaveFileCommand command,
			PhysicalFileMetadata physicalFileMetadata,
			long createdAt) throws SQLException {

		// If the file is orphaned, mark it as active
		if (physicalFileMetadata.status() == PhysicalFileStatus.ORPHANED) {
			physicalFileMetadata = this.physicalFileRepository.markActive(
					connection,
					physicalFileMetadata.id(),
					createdAt);
		}

		FileUsageMetadata fileUsageMetadata = this.createFileUsageMetadata(
				command,
				physicalFileMetadata.id(),
				createdAt);

		FileUsageMetadata insertedFileUsageMetadata = this.fileUsageRepository.insert(connection, fileUsageMetadata);

		StoredFile storedFile = this.createStoredFileFromPhysicalFileMetadata(physicalFileMetadata);

		return SaveFileResult.of(
				storedFile,
				physicalFileMetadata,
				insertedFileUsageMetadata);
	}

	/**
	 * Saves a new physical file and creates metadata records for it.
	 *
	 * @param connection database connection
	 * @param command    save file command
	 * @param storedFile stored file record
	 * @param createdAt  timestamp of the record
	 * @return result containing stored file and created metadata records
	 * @throws SQLException if metadata persistence fails
	 */
	private SaveFileResult saveNewPhysicalFileAndUsage(
			Connection connection,
			SaveFileCommand command,
			StoredFile storedFile,
			long createdAt) throws SQLException {
		PhysicalFileMetadata physicalFileMetadata = PhysicalFileMetadata.newFile(
				storedFile.uuid(),
				storedFile.originalName(),
				storedFile.relativePath(),
				command.mimeType(),
				storedFile.extension(),
				storedFile.sizeBytes(),
				storedFile.sha256(),
				createdAt);

		PhysicalFileMetadata insertedPhysicalFileMetadata = this.physicalFileRepository.insert(connection,
				physicalFileMetadata);

		FileUsageMetadata fileUsageMetadata = this.createFileUsageMetadata(
				command,
				insertedPhysicalFileMetadata.id(),
				createdAt);

		FileUsageMetadata insertedFileUsageMetadata = this.fileUsageRepository.insert(connection, fileUsageMetadata);

		return SaveFileResult.of(
				storedFile,
				insertedPhysicalFileMetadata,
				insertedFileUsageMetadata);
	}

	/**
	 * Creates a new stored file record from physical file metadata.
	 *
	 * @param physicalFileMetadata physical file metadata
	 * @return new stored file record
	 */
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

	/**
	 * Creates a new file usage metadata record.
	 *
	 * @param command        save file command
	 * @param physicalFileId ID of the physical file
	 * @param createdAt      timestamp of the record
	 * @return new file usage metadata record
	 */
	private FileUsageMetadata createFileUsageMetadata(
			SaveFileCommand command,
			long physicalFileId,
			long createdAt) {
		return FileUsageMetadata.newUsage(
				UUID.randomUUID(),
				physicalFileId,
				command.usageType(),
				command.ownerType(),
				command.ownerId(),
				command.displayName(),
				command.metadataJson(),
				createdAt);
	}

	/**
	 * Cleans up after a failed save operation by deleting the staged file and
	 * stored file.
	 *
	 * @param stagedFile staged file record
	 * @param storedFile stored file record
	 * @param exception  SQLException that caused the failure
	 */
	private void cleanupAfterFailedSave(
			StagedFile stagedFile,
			StoredFile storedFile,
			SQLException exception) {
		try {
			if (storedFile != null) {
				this.localFileDeleter.deleteIfExists(storedFile);
			}

			this.localFileDeleter.deleteIfExists(stagedFile);
		} catch (IOException cleanupException) {
			exception.addSuppressed(cleanupException);
		}
	}
}
