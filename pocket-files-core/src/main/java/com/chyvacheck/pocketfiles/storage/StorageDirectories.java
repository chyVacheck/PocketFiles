package com.chyvacheck.pocketfiles.storage;

import com.chyvacheck.pocketfiles.config.PocketFilesConfig;

import java.nio.file.Path;
import java.util.Objects;

public final class StorageDirectories {

	// Имя директории, которая содержит файлы.
	private static final String FILES_DIRECTORY = "files";

	// Имя директории, которая содержит временные файлы.
	private static final String TEMP_DIRECTORY = ".tmp";

	// Имя файла, который содержит метаданные.
	private static final String METADATA_FILE = "metadata.db";

	// Конфигурация, которая содержит настройки генерации пути.
	private final PocketFilesConfig config;

	/**
	 * Конструктор, который принимает конфигурацию, которая содержит настройки
	 * генерации пути.
	 * 
	 * @param config конфигурация, которая содержит настройки генерации пути.
	 */
	public StorageDirectories(PocketFilesConfig config) {
		this.config = Objects.requireNonNull(config, "config must not be null");
	}

	/**
	 * Возвращает директорию, которая содержит все другие директории.
	 * 
	 * @return {@link Path} директория, которая содержит все другие директории.
	 */
	public Path getBaseDirectory() {
		return this.config.getBaseDirectory();
	}

	/**
	 * Возвращает директорию, которая содержит файлы.
	 * 
	 * @return {@link Path} директория, которая содержит файлы.
	 */
	public Path getFilesDirectory() {
		return this.getBaseDirectory().resolve(FILES_DIRECTORY);
	}

	/**
	 * Возвращает директорию, которая содержит временные файлы.
	 * 
	 * @return {@link Path} директория, которая содержит временные файлы.
	 */
	public Path getTempDirectory() {
		return this.getBaseDirectory().resolve(TEMP_DIRECTORY);
	}

	/**
	 * Возвращает путь к файлу, который содержит метаданные.
	 * 
	 * @return {@link Path} путь к файлу, который содержит метаданные.
	 */
	public Path getDatabasePath() {
		return this.getBaseDirectory().resolve(METADATA_FILE);
	}

	/**
	 * Resolves a file relative path against the files directory.
	 *
	 * @param relativePath relative path stored in metadata, for example
	 *                     2026/01/02/file.png
	 * @return absolute path inside the files directory
	 */
	public Path resolveFilePath(String relativePath) {
		Objects.requireNonNull(relativePath, "relativePath must not be null");

		Path filesDirectory = this.getFilesDirectory().normalize();
		Path resolvedPath = filesDirectory.resolve(relativePath).normalize();

		// Проверяем, что путь начинается с директории, которая содержит файлы.
		if (!resolvedPath.toString().startsWith(filesDirectory.toString())) {
			throw new IllegalArgumentException("relativePath escapes files directory");
		}

		return resolvedPath;
	}
}
