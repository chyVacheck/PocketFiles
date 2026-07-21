package com.chyvacheck.pocketfiles.storage;

import com.chyvacheck.pocketfiles.config.DirectoryDepth;
import com.chyvacheck.pocketfiles.config.PocketFilesConfig;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.UUID;

public final class LocalPathStrategy {

	// Форматирует год в виде 4 цифр: 2026
	private static final DateTimeFormatter YEAR = DateTimeFormatter.ofPattern("yyyy");

	// Форматирует месяц с ведущим нулем: 01, 02, 12
	private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MM");

	// Форматирует день месяца с ведущим нулем: 01, 02, 31
	private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd");

	// Форматирует час в 24-часовом формате с ведущим нулем: 00, 01, 23
	private static final DateTimeFormatter HOUR = DateTimeFormatter.ofPattern("HH");

	// Конфигурация, которая содержит настройки генерации пути.
	private final PocketFilesConfig config;

	public LocalPathStrategy(PocketFilesConfig config) {
		// Конфиг обязателен, потому что из него мы берем настройки генерации пути.
		this.config = Objects.requireNonNull(config, "config must not be null");
	}

	public String generateRelativePath(UUID physicalFileUuid, String extension, LocalDateTime dateTime) {
		// UUID обязателен, потому что имя файла строится на его основе.
		Objects.requireNonNull(physicalFileUuid, "physicalFileUuid must not be null");

		// Дата/время обязательны, потому что директории строятся по
		// году/месяцу/дню/часу.
		Objects.requireNonNull(dateTime, "dateTime must not be null");

		// Получаем имя файла, например:
		// 550e8400-e29b-41d4-a716-446655440000.png
		String fileName = StoredFileNameGenerator.generate(physicalFileUuid, extension);

		// Генерируем директорию по выбранной глубине: YEAR / MONTH / DAY / HOUR.
		String directoryPath = generateDirectoryPath(dateTime, config.getDirectoryDepth());

		// Собираем final relative_path.
		return directoryPath + "/" + fileName;
	}

	private String generateDirectoryPath(LocalDateTime dateTime, DirectoryDepth directoryDepth) {
		return switch (directoryDepth) {
			// Год
			case YEAR -> String.join(
					"/",
					YEAR.format(dateTime));

			// Месяц
			case MONTH -> String.join(
					"/",
					YEAR.format(dateTime),
					MONTH.format(dateTime));

			// День
			case DAY -> String.join(
					"/",
					YEAR.format(dateTime),
					MONTH.format(dateTime),
					DAY.format(dateTime));

			// Час
			case HOUR -> String.join(
					"/",
					YEAR.format(dateTime),
					MONTH.format(dateTime),
					DAY.format(dateTime),
					HOUR.format(dateTime));
		};
	}
}