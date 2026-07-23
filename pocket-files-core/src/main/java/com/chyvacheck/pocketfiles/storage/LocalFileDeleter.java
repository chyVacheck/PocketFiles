package com.chyvacheck.pocketfiles.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Objects;

/**
 * Deletes files from local storage.
 */
public final class LocalFileDeleter {

	/**
	 * Deletes the stored file from disk if it exists.
	 *
	 * @param storedFile stored file to delete
	 * @throws IOException if the file cannot be deleted
	 */
	public void deleteIfExists(StoredFile storedFile) throws IOException {
		Objects.requireNonNull(storedFile, "storedFile must not be null");

		Files.deleteIfExists(storedFile.absolutePath());
	}

	/**
	 * Deletes the staged file from disk if it exists.
	 *
	 * @param stagedFile {@link StagedFile} staged file to delete
	 * @throws IOException if the file cannot be deleted
	 */
	public void deleteIfExists(StagedFile stagedFile) throws IOException {
		Objects.requireNonNull(stagedFile, "stagedFile must not be null");

		Files.deleteIfExists(stagedFile.tempFilePath());
	}
}
