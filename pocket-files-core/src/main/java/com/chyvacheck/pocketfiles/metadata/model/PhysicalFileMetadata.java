package com.chyvacheck.pocketfiles.metadata.model;

import java.util.Objects;
import java.util.UUID;

import com.chyvacheck.pocketfiles.metadata.status.PhysicalFileStatus;

/**
 * Metadata record that represents a physical file stored on disk.
 *
 * <p>
 * This object mirrors a row from the {@code physical_files} SQLite table.
 * It contains file identity, storage path, content information and lifecycle
 * status.
 */
public record PhysicalFileMetadata(
		Long id,
		UUID uuid,

		String originalName,
		String relativePath,

		String mimeType,
		String extension,

		long sizeBytes,
		String sha256,

		PhysicalFileStatus status,
		long createdAt,
		long statusChangedAt,
		Long deletedAt) {

	/**
	 * Creates a validated physical file metadata instance.
	 *
	 * @throws NullPointerException     if a required reference field is null
	 * @throws IllegalArgumentException if a required string is blank or a numeric
	 *                                  value is invalid
	 */
	public PhysicalFileMetadata {
		Objects.requireNonNull(uuid, "uuid must not be null");
		Objects.requireNonNull(originalName, "originalName must not be null");
		Objects.requireNonNull(relativePath, "relativePath must not be null");
		Objects.requireNonNull(sha256, "sha256 must not be null");
		Objects.requireNonNull(status, "status must not be null");

		originalName = originalName.trim();
		relativePath = relativePath.trim();
		sha256 = sha256.trim();

		if (mimeType != null) {
			mimeType = mimeType.trim();
		}

		if (extension != null) {
			extension = extension.trim();
		}

		if (originalName.isBlank()) {
			throw new IllegalArgumentException("originalName must not be blank");
		}

		if (relativePath.isBlank()) {
			throw new IllegalArgumentException("relativePath must not be blank");
		}

		if (mimeType != null && mimeType.isBlank()) {
			throw new IllegalArgumentException("mimeType must not be blank");
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

		if (createdAt < 0) {
			throw new IllegalArgumentException("createdAt must not be negative");
		}

		if (statusChangedAt < 0) {
			throw new IllegalArgumentException("statusChangedAt must not be negative");
		}

		if (deletedAt != null && deletedAt < 0) {
			throw new IllegalArgumentException("deletedAt must not be negative");
		}
	}

	/**
	 * Creates a physical file metadata instance before it is inserted into SQLite.
	 *
	 * <p>
	 * The database {@code id} is unknown before insertion, so it is set to
	 * {@code null}.
	 */
	public static PhysicalFileMetadata newFile(
			UUID uuid,
			String originalName,
			String relativePath,
			String mimeType,
			String extension,
			long sizeBytes,
			String sha256,
			long createdAt) {
		return new PhysicalFileMetadata(
				null,
				uuid,
				originalName,
				relativePath,
				mimeType,
				extension,
				sizeBytes,
				sha256,
				PhysicalFileStatus.ACTIVE,
				createdAt,
				createdAt,
				null);
	}

	/**
	 * Creates a physical file metadata instance from all available fields.
	 */
	public static PhysicalFileMetadata at(
			Long id,
			UUID uuid,
			String originalName,
			String relativePath,
			String mimeType,
			String extension,
			long sizeBytes,
			String sha256,
			PhysicalFileStatus status,
			long createdAt,
			long statusChangedAt,
			Long deletedAt) {
		return new PhysicalFileMetadata(
				id,
				uuid,
				originalName,
				relativePath,
				mimeType,
				extension,
				sizeBytes,
				sha256,
				status,
				createdAt,
				statusChangedAt,
				deletedAt);
	}
}
