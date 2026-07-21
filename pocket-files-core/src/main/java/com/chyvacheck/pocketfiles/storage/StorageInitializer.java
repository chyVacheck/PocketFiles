package com.chyvacheck.pocketfiles.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Objects;

public final class StorageInitializer {

	/**
	 * The storage directories to initialize.
	 */
	private final StorageDirectories directories;

	/**
	 * Create a new instance of StorageInitializer.
	 * 
	 * @param directories The storage directories to initialize.
	 */
	public StorageInitializer(StorageDirectories directories) {
		this.directories = Objects.requireNonNull(directories, "directories must not be null");
	}

	/**
	 * Initialize the storage directories.
	 */
	public void initialize() throws IOException {
		this.createBaseDirectory();
		this.createFilesDirectory();
		this.createTempDirectory();
	}

	/**
	 * Create the base directory.
	 */
	private void createBaseDirectory() throws IOException {
		Files.createDirectories(this.directories.getBaseDirectory());
	}

	/**
	 * Create the files directory.
	 */
	private void createFilesDirectory() throws IOException {
		Files.createDirectories(this.directories.getFilesDirectory());
	}

	/**
	 * Create the temporary directory.
	 */
	private void createTempDirectory() throws IOException {
		Files.createDirectories(this.directories.getTempDirectory());
	}
}
