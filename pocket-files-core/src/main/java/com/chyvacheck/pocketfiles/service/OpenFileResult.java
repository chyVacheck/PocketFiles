package com.chyvacheck.pocketfiles.service;

import com.chyvacheck.pocketfiles.metadata.model.FileUsageMetadata;
import com.chyvacheck.pocketfiles.metadata.model.PhysicalFileMetadata;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Result of a successful file open operation.
 *
 * <p>
 * This record contains the logical file usage metadata, the related physical
 * file metadata and the resolved absolute path to the stored file.
 */
public record OpenFileResult(
		FileUsageMetadata fileUsageMetadata,
		PhysicalFileMetadata physicalFileMetadata,
		Path absolutePath) {

	/**
	 * Creates a validated open file result.
	 *
	 * @throws NullPointerException if one of the result parts is null
	 */
	public OpenFileResult {
		Objects.requireNonNull(fileUsageMetadata, "fileUsageMetadata must not be null");
		Objects.requireNonNull(physicalFileMetadata, "physicalFileMetadata must not be null");
		Objects.requireNonNull(absolutePath, "absolutePath must not be null");
	}

	/**
	 * Creates an open file result from all resolved file parts.
	 *
	 * @param fileUsageMetadata    metadata record from {@code file_usages}
	 * @param physicalFileMetadata metadata record from {@code physical_files}
	 * @param absolutePath         resolved absolute path to the stored file
	 * @return open file result
	 */
	public static OpenFileResult of(
			FileUsageMetadata fileUsageMetadata,
			PhysicalFileMetadata physicalFileMetadata,
			Path absolutePath) {
		return new OpenFileResult(
				fileUsageMetadata,
				physicalFileMetadata,
				absolutePath);
	}
}
