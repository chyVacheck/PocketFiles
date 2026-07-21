package com.chyvacheck.pocketfiles.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.chyvacheck.pocketfiles.config.PocketFilesConfig;

class FinalFileMoverTest {

	@TempDir
	Path tempDir;

	private static final String RELATIVE_PATH = "2026/01/02/file.txt";

	@Test
	void shouldThrowExceptionWhenStorageDirectoriesIsNull() {
		// Act + Assert
		NullPointerException exception = assertThrows(
				NullPointerException.class,
				() -> new FinalFileMover(null));

		// Assert
		assertEquals("storageDirectories must not be null", exception.getMessage());
	}

	@Test
	void shouldThrowExceptionWhenTempFilePathIsNull() throws IOException {
		// Arrange
		StorageDirectories storageDirectories = createStorageDirectories();
		FinalFileMover finalFileMover = createInitializedFinalFileMover(storageDirectories);

		// Act + Assert
		NullPointerException exception = assertThrows(
				NullPointerException.class,
				() -> finalFileMover.move(null,
						RELATIVE_PATH));

		// Assert
		assertEquals("tempFilePath must not be null", exception.getMessage());
	}

	@Test
	void shouldThrowExceptionWhenRelativePathIsNull() throws IOException {
		// Arrange
		StorageDirectories storageDirectories = createStorageDirectories();
		FinalFileMover finalFileMover = createInitializedFinalFileMover(storageDirectories);

		Path tempFilePath = createTempFile(storageDirectories);

		NullPointerException exception = assertThrows(
				NullPointerException.class,
				() -> finalFileMover.move(tempFilePath, null));

		// Assert
		assertEquals("relativePath must not be null", exception.getMessage());
	}

	@Test
	void shouldCreateParentDirectories() throws IOException {
		// Arrange
		StorageDirectories storageDirectories = createStorageDirectories();
		FinalFileMover finalFileMover = createInitializedFinalFileMover(storageDirectories);

		Path tempFilePath = createTempFile(storageDirectories);

		// Act
		Path finalPath = finalFileMover.move(tempFilePath, RELATIVE_PATH);

		// Assert
		assertTrue(Files.isDirectory(finalPath.getParent()));
	}

	@Test
	void shouldMoveTempFileToFinalLocation() throws IOException {
		// Arrange
		StorageDirectories storageDirectories = createStorageDirectories();
		FinalFileMover finalFileMover = createInitializedFinalFileMover(storageDirectories);

		Path tempFilePath = createTempFile(storageDirectories);

		Files.writeString(tempFilePath, "Hello", StandardCharsets.UTF_8);

		// Act
		Path finalPath = finalFileMover.move(tempFilePath, RELATIVE_PATH);

		// Assert
		assertEquals(storageDirectories.resolveFilePath(RELATIVE_PATH), finalPath);
		assertTrue(Files.exists(finalPath));
		assertTrue(Files.isRegularFile(finalPath));
		assertTrue(finalPath.startsWith(storageDirectories.getFilesDirectory()));
		assertEquals("Hello", Files.readString(finalPath));
	}

	@Test
	void shouldRemoveTempFileAfterMove() throws IOException {
		// Arrange
		StorageDirectories storageDirectories = createStorageDirectories();
		FinalFileMover finalFileMover = createInitializedFinalFileMover(storageDirectories);

		Path tempFilePath = createTempFile(storageDirectories);

		// Act
		finalFileMover.move(tempFilePath, RELATIVE_PATH);

		// Assert
		assertFalse(Files.exists(tempFilePath));
	}

	@Test
	void shouldThrowExceptionWhenRelativePathEscapesFilesDirectory() throws IOException {
		// Arrange
		StorageDirectories storageDirectories = createStorageDirectories();
		FinalFileMover finalFileMover = createInitializedFinalFileMover(storageDirectories);

		Path tempFilePath = createTempFile(storageDirectories);

		// Act + Assert
		IllegalArgumentException exception = assertThrows(
				IllegalArgumentException.class,
				() -> finalFileMover.move(tempFilePath, "../../etc/passwd"));

		assertEquals("relativePath escapes files directory", exception.getMessage());
	}

	/**
	 * Creates a configuration object that contains storage settings.
	 * 
	 * @return the configuration object
	 */
	private StorageDirectories createStorageDirectories() {
		// Create a configuration object that contains storage settings.
		PocketFilesConfig config = PocketFilesConfig.builder()
				.baseDirectory(tempDir.resolve("pocket-files"))
				.build();

		return new StorageDirectories(config);
	}

	/**
	 * Creates a temporary file in the temporary directory.
	 * 
	 * @param storageDirectories the storage directories
	 * @return the path of the created temporary file
	 * @throws IOException if the temporary file cannot be created
	 */
	private Path createTempFile(StorageDirectories storageDirectories) throws IOException {
		return Files.createTempFile(
				storageDirectories.getTempDirectory(),
				"test-",
				".tmp");
	}

	/**
	 * Creates an initialized FinalFileMover object.
	 * 
	 * @param storageDirectories the storage directories
	 * @return the initialized FinalFileMover object
	 * @throws IOException if the storage directories cannot be initialized
	 */
	private FinalFileMover createInitializedFinalFileMover(StorageDirectories storageDirectories) throws IOException {
		StorageInitializer storageInitializer = new StorageInitializer(storageDirectories);
		storageInitializer.initialize();

		return new FinalFileMover(storageDirectories);
	}
}
