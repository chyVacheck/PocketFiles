package com.chyvacheck.pocketfiles.storage;

import com.chyvacheck.pocketfiles.config.DirectoryDepth;
import com.chyvacheck.pocketfiles.config.PocketFilesConfig;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LocalPathStrategyTest {

	private static final Path BASE_DIRECTORY = Path.of("/tmp/pocket-files");

	private static final UUID PHYSICAL_FILE_UUID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

	private static final LocalDateTime DATE_TIME = LocalDateTime.of(2026, 1, 2, 3, 15);

	@Test
	void shouldGeneratePathWithYearDirectoryDepth() {
		// Arrange
		LocalPathStrategy strategy = createStrategy(DirectoryDepth.YEAR);

		// Act
		String relativePath = strategy.generateRelativePath(PHYSICAL_FILE_UUID, "png", DATE_TIME);

		// Assert
		assertEquals(
				"2026/" + PHYSICAL_FILE_UUID.toString() + ".png",
				relativePath);
	}

	@Test
	void shouldGeneratePathWithMonthDirectoryDepth() {
		// Arrange
		LocalPathStrategy strategy = createStrategy(DirectoryDepth.MONTH);

		// Act
		String relativePath = strategy.generateRelativePath(PHYSICAL_FILE_UUID, "png", DATE_TIME);

		// Assert
		assertEquals(
				"2026/01/" + PHYSICAL_FILE_UUID.toString() + ".png",
				relativePath);
	}

	@Test
	void shouldGeneratePathWithDayDirectoryDepth() {
		// Arrange
		LocalPathStrategy strategy = createStrategy(DirectoryDepth.DAY);

		// Act
		String relativePath = strategy.generateRelativePath(PHYSICAL_FILE_UUID, "png", DATE_TIME);

		// Assert
		assertEquals(
				"2026/01/02/" + PHYSICAL_FILE_UUID.toString() + ".png",
				relativePath);
	}

	@Test
	void shouldGeneratePathWithHourDirectoryDepth() {
		// Arrange
		LocalPathStrategy strategy = createStrategy(DirectoryDepth.HOUR);

		// Act
		String relativePath = strategy.generateRelativePath(PHYSICAL_FILE_UUID, "png", DATE_TIME);

		// Assert
		assertEquals(
				"2026/01/02/03/" + PHYSICAL_FILE_UUID.toString() + ".png",
				relativePath);
	}

	@Test
	void shouldGeneratePathWithoutExtension() {
		// Arrange
		LocalPathStrategy strategy = createStrategy(DirectoryDepth.DAY);

		// Act
		String relativePath = strategy.generateRelativePath(PHYSICAL_FILE_UUID, null, DATE_TIME);

		// Assert
		assertEquals(
				"2026/01/02/" + PHYSICAL_FILE_UUID.toString(),
				relativePath);
	}

	private LocalPathStrategy createStrategy(DirectoryDepth directoryDepth) {
		// Создаем конфиг с нужной глубиной директорий.
		PocketFilesConfig config = PocketFilesConfig.builder()
				.baseDirectory(BASE_DIRECTORY)
				.directoryDepth(directoryDepth)
				.build();

		return new LocalPathStrategy(config);
	}
}