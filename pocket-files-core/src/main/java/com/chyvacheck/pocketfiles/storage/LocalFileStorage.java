package com.chyvacheck.pocketfiles.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public final class LocalFileStorage {

	private final TempFileWriter tempFileWriter;

	private final Sha256Calculator sha256Calculator;

	private final LocalPathStrategy localPathStrategy;

	private final FinalFileMover finalFileMover;

	/**
	 * Constructor, which creates a new instance of LocalFileStorage.
	 * 
	 * @param tempFileWriter    TempFileWriter instance, which will be used to
	 *                          create temporary files.
	 * @param sha256Calculator  Sha256Calculator instance, which will be used to
	 *                          calculate SHA-256 hash.
	 * @param localPathStrategy LocalPathStrategy instance, which will be used to
	 *                          generate
	 *                          relative path to the file.
	 * @param finalFileMover    FinalFileMover instance, which will be used to
	 *                          move file to final directory.
	 */
	public LocalFileStorage(
			TempFileWriter tempFileWriter,
			Sha256Calculator sha256Calculator,
			LocalPathStrategy localPathStrategy,
			FinalFileMover finalFileMover) {
		this.tempFileWriter = Objects.requireNonNull(tempFileWriter, "tempFileWriter must not be null");
		this.sha256Calculator = Objects.requireNonNull(sha256Calculator, "sha256Calculator must not be null");
		this.localPathStrategy = Objects.requireNonNull(localPathStrategy, "localPathStrategy must not be null");
		this.finalFileMover = Objects.requireNonNull(finalFileMover, "finalFileMover must not be null");
	}

	/**
	 * Saves file to local storage.
	 * 
	 * @param inputStream  Input stream, which will be used to read file content.
	 * @param originalName Original name of the file.
	 * @param dateTime     Date and time of the file.
	 * @return StoredFile instance, which contains information about the saved file.
	 * @throws IOException If an I/O error occurs.
	 */
	public StoredFile save(InputStream inputStream, String originalName, LocalDateTime dateTime) throws IOException {
		// validation
		Objects.requireNonNull(inputStream, "inputStream must not be null");
		Objects.requireNonNull(originalName, "originalName must not be null");
		Objects.requireNonNull(dateTime, "dateTime must not be null");

		String trimmedOriginalName = originalName.trim();

		if (trimmedOriginalName.isBlank()) {
			throw new IllegalArgumentException("originalName must not be blank");
		}

		// generate file info
		String extension = FileExtensionExtractor.extract(trimmedOriginalName);
		UUID physicalFileUuid = UUID.randomUUID();
		Path tempFilePath = this.tempFileWriter.write(inputStream);
		long sizeBytes = Files.size(tempFilePath);
		String sha256 = this.sha256Calculator.calculate(tempFilePath);

		String relativePath = this.localPathStrategy.generateRelativePath(
				physicalFileUuid,
				extension,
				dateTime);

		// move file to final path
		Path absolutePath = this.finalFileMover.move(tempFilePath, relativePath);

		// return stored file
		return StoredFile.at(
				physicalFileUuid,
				trimmedOriginalName,
				relativePath,
				extension,
				sizeBytes,
				sha256,
				absolutePath);
	}
}
