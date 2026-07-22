package com.chyvacheck.pocketfiles.metadata.model;

import com.chyvacheck.pocketfiles.metadata.status.FileUsageStatus;

import java.util.Objects;
import java.util.UUID;

/**
 * Metadata record that represents a logical usage of a physical file.
 *
 * <p>
 * This object mirrors a row from the {@code file_usages} SQLite table.
 * It connects a physical file with an external owner, usage type and
 * optional display metadata.
 */
public record FileUsageMetadata(
		Long id,
		UUID uuid,

		long physicalFileId,

		String usageType,
		String ownerType,
		String ownerId,

		String displayName,
		String metadataJson,

		FileUsageStatus status,
		long createdAt,
		Long deletedAt) {

	/**
	 * The default usage type.
	 */
	private static final String DEFAULT_USAGE_TYPE = "default";

	/**
	 * Creates a validated file usage metadata instance.
	 *
	 * @throws NullPointerException     if a required reference field is null
	 * @throws IllegalArgumentException if a required string is blank or a numeric
	 *                                  value is invalid
	 */
	public FileUsageMetadata {
		Objects.requireNonNull(uuid, "uuid must not be null");
		Objects.requireNonNull(usageType, "usageType must not be null");
		Objects.requireNonNull(status, "status must not be null");

		usageType = usageType.trim();

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

		if (physicalFileId <= 0) {
			throw new IllegalArgumentException("physicalFileId must be positive");
		}

		if (usageType.isBlank()) {
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

		if (createdAt < 0) {
			throw new IllegalArgumentException("createdAt must not be negative");
		}

		if (deletedAt != null && deletedAt < 0) {
			throw new IllegalArgumentException("deletedAt must not be negative");
		}
	}

	/**
	 * Creates a file usage metadata instance before it is inserted into SQLite.
	 *
	 * <p>
	 * The database {@code id} is unknown before insertion, so it is set to
	 * {@code null}. The usage status is set to {@link FileUsageStatus#ACTIVE}.
	 */
	public static FileUsageMetadata newUsage(
			UUID uuid,
			long physicalFileId,
			String usageType,
			String ownerType,
			String ownerId,
			String displayName,
			String metadataJson,
			long createdAt) {
		return new FileUsageMetadata(
				null,
				uuid,
				physicalFileId,
				normalizeUsageType(usageType),
				ownerType,
				ownerId,
				displayName,
				metadataJson,
				FileUsageStatus.ACTIVE,
				createdAt,
				null);
	}

	/**
	 * Creates a file usage metadata instance from all available fields.
	 */
	public static FileUsageMetadata at(
			Long id,
			UUID uuid,
			long physicalFileId,
			String usageType,
			String ownerType,
			String ownerId,
			String displayName,
			String metadataJson,
			FileUsageStatus status,
			long createdAt,
			Long deletedAt) {
		return new FileUsageMetadata(
				id,
				uuid,
				physicalFileId,
				usageType,
				ownerType,
				ownerId,
				displayName,
				metadataJson,
				status,
				createdAt,
				deletedAt);
	}

	/**
	 * Returns the default usage type when the provided usage type is null or blank.
	 */
	private static String normalizeUsageType(String usageType) {
		if (usageType == null || usageType.isBlank()) {
			return DEFAULT_USAGE_TYPE;
		}

		return usageType;
	}
}
