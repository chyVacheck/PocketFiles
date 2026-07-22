package com.chyvacheck.pocketfiles;

import com.chyvacheck.pocketfiles.config.PocketFilesConfig;
import com.chyvacheck.pocketfiles.metadata.DatabaseConnectionFactory;
import com.chyvacheck.pocketfiles.metadata.MetadataSchemaInitializer;
import com.chyvacheck.pocketfiles.metadata.repository.FileUsageRepository;
import com.chyvacheck.pocketfiles.metadata.repository.PhysicalFileRepository;
import com.chyvacheck.pocketfiles.metadata.transaction.MetadataTransactionManager;
import com.chyvacheck.pocketfiles.service.FileSaveService;
import com.chyvacheck.pocketfiles.service.SaveFileCommand;
import com.chyvacheck.pocketfiles.service.SaveFileResult;
import com.chyvacheck.pocketfiles.storage.FinalFileMover;
import com.chyvacheck.pocketfiles.storage.LocalFileDeleter;
import com.chyvacheck.pocketfiles.storage.LocalFileStorage;
import com.chyvacheck.pocketfiles.storage.LocalPathStrategy;
import com.chyvacheck.pocketfiles.storage.Sha256Calculator;
import com.chyvacheck.pocketfiles.storage.StorageDirectories;
import com.chyvacheck.pocketfiles.storage.StorageInitializer;
import com.chyvacheck.pocketfiles.storage.TempFileWriter;

import java.io.IOException;
import java.sql.SQLException;
import java.time.Clock;
import java.util.Objects;

/**
 * Main public entry point for PocketFiles.
 *
 * <p>
 * This facade hides internal storage, metadata and repository wiring from the
 * library user.
 */
public final class PocketFiles {

	private final FileSaveService fileSaveService;

	/**
	 * Creates a new PocketFiles instance.
	 *
	 * @param fileSaveService file save service instance
	 */
	private PocketFiles(FileSaveService fileSaveService) {
		this.fileSaveService = Objects.requireNonNull(fileSaveService, "fileSaveService must not be null");
	}

	/**
	 * Creates and initializes a PocketFiles instance.
	 *
	 * <p>
	 * This method creates required storage directories and initializes the SQLite
	 * metadata schema.
	 *
	 * @param config PocketFiles configuration
	 * @return initialized PocketFiles facade
	 * @throws IOException  if storage directories cannot be initialized
	 * @throws SQLException if metadata schema cannot be initialized
	 */
	public static PocketFiles create(PocketFilesConfig config) throws IOException, SQLException {
		return PocketFiles.create(config, Clock.systemUTC());
	}

	/**
	 * Creates and initializes a PocketFiles instance with a custom clock.
	 *
	 * <p>
	 * This overload is mainly useful for tests.
	 *
	 * @param config PocketFiles configuration
	 * @param clock  clock used for generated timestamps
	 * @return initialized PocketFiles facade
	 * @throws IOException  if storage directories cannot be initialized
	 * @throws SQLException if metadata schema cannot be initialized
	 */
	static PocketFiles create(PocketFilesConfig config, Clock clock) throws IOException, SQLException {
		Objects.requireNonNull(config, "config must not be null");
		Objects.requireNonNull(clock, "clock must not be null");

		StorageDirectories storageDirectories = new StorageDirectories(config);

		StorageInitializer storageInitializer = new StorageInitializer(storageDirectories);
		storageInitializer.initialize();

		DatabaseConnectionFactory databaseConnectionFactory = new DatabaseConnectionFactory(storageDirectories);

		MetadataSchemaInitializer metadataSchemaInitializer = new MetadataSchemaInitializer(databaseConnectionFactory);
		metadataSchemaInitializer.initialize();

		LocalFileStorage localFileStorage = new LocalFileStorage(
				new TempFileWriter(storageDirectories),
				new Sha256Calculator(),
				new LocalPathStrategy(config),
				new FinalFileMover(storageDirectories));

		FileSaveService fileSaveService = new FileSaveService(
				localFileStorage,
				new LocalFileDeleter(),
				new MetadataTransactionManager(databaseConnectionFactory),
				new PhysicalFileRepository(),
				new FileUsageRepository(),
				clock);

		return new PocketFiles(fileSaveService);
	}

	/**
	 * Saves a file and creates metadata records for it.
	 *
	 * @param command save file command
	 * @return save file result
	 * @throws IOException  if file storage fails
	 * @throws SQLException if metadata persistence fails
	 */
	public SaveFileResult save(SaveFileCommand command) throws IOException, SQLException {
		return this.fileSaveService.save(command);
	}
}
