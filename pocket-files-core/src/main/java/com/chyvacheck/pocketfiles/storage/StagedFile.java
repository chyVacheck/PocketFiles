package com.chyvacheck.pocketfiles.storage;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Represents a file that was written to the temporary storage area.
 *
 * <p>
 * A staged file is not a final stored file yet. It is used to calculate
 * metadata such as size and SHA-256 before deciding whether the file should be
 * moved to final storage or discarded as a duplicate.
 */
public record StagedFile(
		String originalName,
		String extension,
		long sizeBytes,
		String sha256,
		Path tempFilePath) {

	public StagedFile {
		Objects.requireNonNull(originalName, "originalName must not be null");
		Objects.requireNonNull(sha256, "sha256 must not be null");
		Objects.requireNonNull(tempFilePath, "tempFilePath must not be null");

		originalName = originalName.trim();
		sha256 = sha256.trim();

		if (extension != null) {
			extension = extension.trim();
		}

		if (originalName.isBlank()) {
			throw new IllegalArgumentException("originalName must not be blank");
		}

		if (extension != null && extension.isBlank()) {
			throw new IllegalArgumentException("extension must not be blank");
		}

		if (sizeBytes < 0) {
			throw new IllegalArgumentException("sizeBytes must not be negative");
		}

		if (sha256.isBlank()) {
			throw new IllegalArgumentException("sha256 must not be blank");
		}
	}

	/**
	 * Creates a new {@link StagedFile} instance with the specified values.
	 *
	 * @param originalName The original name of the file.
	 * @param extension    The file extension.
	 * @param sizeBytes    The size in bytes of the file.
	 * @param sha256       The SHA-256 hash of the file.
	 * @param tempFilePath The path to the temporary file on disk.
	 * @return The created {@link StagedFile} instance.
	 */
	public static StagedFile at(
			String originalName,
			String extension,
			long sizeBytes,
			String sha256,
			Path tempFilePath) {
		return new StagedFile(
				originalName,
				extension,
				sizeBytes,
				sha256,
				tempFilePath);
	}
}
