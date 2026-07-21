package com.chyvacheck.pocketfiles.storage;

import java.nio.file.Path;
import java.util.Objects;
import java.util.UUID;

public record StoredFile(
		UUID uuid,
		String originalName,
		String relativePath,
		String extension,
		long sizeBytes,
		String sha256,
		Path absolutePath) {

	public StoredFile {
		Objects.requireNonNull(uuid, "uuid must not be null");
		Objects.requireNonNull(originalName, "originalName must not be null");
		Objects.requireNonNull(relativePath, "relativePath must not be null");
		Objects.requireNonNull(sha256, "sha256 must not be null");
		Objects.requireNonNull(absolutePath, "absolutePath must not be null");

		originalName = originalName.trim();
		relativePath = relativePath.trim();
		sha256 = sha256.trim();

		if (extension != null) {
			extension = extension.trim();
		}

		if (originalName.isBlank()) {
			throw new IllegalArgumentException("originalName must not be blank");
		}

		if (relativePath.isBlank()) {
			throw new IllegalArgumentException("relativePath must not be blank");
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

	public static StoredFile at(UUID uuid,
			String originalName,
			String relativePath,
			String extension,
			long sizeBytes,
			String sha256,
			Path absolutePath) {

		return new StoredFile(
				uuid,
				originalName,
				relativePath,
				extension,
				sizeBytes,
				sha256,
				absolutePath);

	}

}
