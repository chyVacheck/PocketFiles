package com.chyvacheck.pocketfiles.service;

import java.io.InputStream;
import java.util.Objects;

/**
 * Command object used to request saving a file through PocketFiles.
 *
 * <p>
 * This record contains the input stream, original file name and optional usage
 * metadata that will be used to create both physical file metadata and file
 * usage metadata.
 */
public record SaveFileCommand(
		InputStream inputStream,
		String originalName,
		String mimeType,

		String usageType,
		String ownerType,
		String ownerId,

		String displayName,
		String metadataJson) {

	/**
	 * Creates a validated save file command.
	 *
	 * @throws NullPointerException     if a required reference field is null
	 * @throws IllegalArgumentException if a required or optional string field is
	 *                                  blank
	 */
	public SaveFileCommand {
		Objects.requireNonNull(inputStream, "inputStream must not be null");
		Objects.requireNonNull(originalName, "originalName must not be null");

		originalName = originalName.trim();

		if (mimeType != null) {
			mimeType = mimeType.trim();
		}

		if (usageType != null) {
			usageType = usageType.trim();
		}

		if (ownerType != null) {
			ownerType = ownerType.trim();
		}

		if (ownerId != null) {
			ownerId = ownerId.trim();
		}

		if (displayName != null) {
			displayName = displayName.trim();
		}

		if (metadataJson != null) {
			metadataJson = metadataJson.trim();
		}

		if (originalName.isBlank()) {
			throw new IllegalArgumentException("originalName must not be blank");
		}

		if (mimeType != null && mimeType.isBlank()) {
			throw new IllegalArgumentException("mimeType must not be blank");
		}

		if (usageType != null && usageType.isBlank()) {
			throw new IllegalArgumentException("usageType must not be blank");
		}

		if (ownerType != null && ownerType.isBlank()) {
			throw new IllegalArgumentException("ownerType must not be blank");
		}

		if (ownerId != null && ownerId.isBlank()) {
			throw new IllegalArgumentException("ownerId must not be blank");
		}

		if (displayName != null && displayName.isBlank()) {
			throw new IllegalArgumentException("displayName must not be blank");
		}

		if (metadataJson != null && metadataJson.isBlank()) {
			throw new IllegalArgumentException("metadataJson must not be blank");
		}
	}

	/**
	 * Creates a save file command with only the required file data.
	 *
	 * @param inputStream  file content input stream
	 * @param originalName original file name
	 * @return save file command with no custom usage metadata
	 */
	public static SaveFileCommand of(InputStream inputStream, String originalName) {
		return new SaveFileCommand(
				inputStream,
				originalName,
				null,
				null,
				null,
				null,
				null,
				null);
	}

	/**
	 * Creates a save file command from all available fields.
	 */
	public static SaveFileCommand at(
			InputStream inputStream,
			String originalName,
			String mimeType,
			String usageType,
			String ownerType,
			String ownerId,
			String displayName,
			String metadataJson) {
		return new SaveFileCommand(
				inputStream,
				originalName,
				mimeType,
				usageType,
				ownerType,
				ownerId,
				displayName,
				metadataJson);
	}
}
