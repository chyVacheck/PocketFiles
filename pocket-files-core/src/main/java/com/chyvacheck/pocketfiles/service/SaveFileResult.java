package com.chyvacheck.pocketfiles.service;

import com.chyvacheck.pocketfiles.metadata.model.FileUsageMetadata;
import com.chyvacheck.pocketfiles.metadata.model.PhysicalFileMetadata;
import com.chyvacheck.pocketfiles.storage.StoredFile;

import java.util.Objects;

/**
 * Result of a successful file save operation.
 *
 * <p>
 * This record combines the low-level stored file information with the metadata
 * records created in SQLite.
 */
public record SaveFileResult(
		StoredFile storedFile,
		PhysicalFileMetadata physicalFileMetadata,
		FileUsageMetadata fileUsageMetadata) {

	/**
	 * Creates a validated save file result.
	 *
	 * @throws NullPointerException if one of the result parts is null
	 */
	public SaveFileResult {
		Objects.requireNonNull(storedFile, "storedFile must not be null");
		Objects.requireNonNull(physicalFileMetadata, "physicalFileMetadata must not be null");
		Objects.requireNonNull(fileUsageMetadata, "fileUsageMetadata must not be null");
	}

	/**
	 * Creates a save file result from all saved file parts.
	 *
	 * @param storedFile           physical file information from local storage
	 * @param physicalFileMetadata metadata record from {@code physical_files}
	 * @param fileUsageMetadata    metadata record from {@code file_usages}
	 * @return save file result
	 */
	public static SaveFileResult of(
			StoredFile storedFile,
			PhysicalFileMetadata physicalFileMetadata,
			FileUsageMetadata fileUsageMetadata) {
		return new SaveFileResult(
				storedFile,
				physicalFileMetadata,
				fileUsageMetadata);
	}
}
