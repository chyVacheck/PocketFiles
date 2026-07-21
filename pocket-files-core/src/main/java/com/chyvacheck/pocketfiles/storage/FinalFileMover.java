package com.chyvacheck.pocketfiles.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public final class FinalFileMover {

	private final StorageDirectories storageDirectories;

	public FinalFileMover(StorageDirectories storageDirectories) {
		this.storageDirectories = Objects.requireNonNull(storageDirectories, "storageDirectories must not be null");
	}

	public Path move(Path tempFilePath, String relativePath) throws IOException {
		Objects.requireNonNull(tempFilePath, "tempFilePath must not be null");
		Objects.requireNonNull(relativePath, "relativePath must not be null");

		// Resolve the final path inside the files directory
		Path finalPath = this.storageDirectories.resolveFilePath(relativePath);

		// Create date-based parent directories if they do not exist yet
		Files.createDirectories(finalPath.getParent());

		// Move the temporary file to its final location
		Files.move(tempFilePath, finalPath);

		return finalPath;
	}
}