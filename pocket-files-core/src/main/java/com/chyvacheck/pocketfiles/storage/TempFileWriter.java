package com.chyvacheck.pocketfiles.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

public final class TempFileWriter {

	private final StorageDirectories storageDirectories;

	public TempFileWriter(StorageDirectories storageDirectories) {
		this.storageDirectories = Objects.requireNonNull(storageDirectories, "storageDirectories must not be null");
	}

	/**
	 * Write the content of the input stream to a temporary file.
	 * 
	 * @param inputStream The input stream to read from.
	 * @return The path of the temporary file.
	 * @throws IOException If an I/O error occurs.
	 */
	public Path write(InputStream inputStream) throws IOException {
		// проверяем входные данные
		Objects.requireNonNull(inputStream, "inputStream must not be null");

		// получаем директорию для временных файлов
		Path tempFileDir = this.storageDirectories.getTempDirectory();

		// создаем временный файл
		Path tempFilePath = Files.createTempFile(tempFileDir, "pocket-files-", ".tmp");

		// копируем содержимое в временный файл
		Files.copy(inputStream, tempFilePath, StandardCopyOption.REPLACE_EXISTING);

		// возвращаем путь к временному файлу
		return tempFilePath;
	}
}
