package com.chyvacheck.pocketfiles.config;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PocketFilesConfigTest {

	private static final Path BASE_DIRECTORY = Path.of("/tmp/pocket-files");

	@Test
	void shouldCreateConfigWithBaseDirectory() {
		// Act
		PocketFilesConfig config = PocketFilesConfig.builder()
				.baseDirectory(BASE_DIRECTORY)
				.build();

		// Assert
		assertEquals(BASE_DIRECTORY, config.getBaseDirectory());
	}

	@Test
	void shouldUseDayDirectoryDepthByDefault() {
		// Act
		PocketFilesConfig config = PocketFilesConfig.builder()
				.baseDirectory(BASE_DIRECTORY)
				.build();

		// Assert
		assertEquals(DirectoryDepth.DAY, config.getDirectoryDepth());
	}

	@Test
	void shouldCreateConfigWithCustomDirectoryDepth() {
		// Act
		PocketFilesConfig config = PocketFilesConfig.builder()
				.baseDirectory(BASE_DIRECTORY)
				.directoryDepth(DirectoryDepth.HOUR)
				.build();

		// Assert
		assertEquals(BASE_DIRECTORY, config.getBaseDirectory());
		assertEquals(DirectoryDepth.HOUR, config.getDirectoryDepth());
	}

	@Test
	void shouldThrowExceptionWhenBaseDirectoryIsNull() {
		// Act + Assert
		NullPointerException exception = assertThrows(
				NullPointerException.class,
				() -> PocketFilesConfig.builder().build());

		assertEquals("baseDirectory must not be null", exception.getMessage());
	}

	@Test
	void shouldThrowExceptionWhenDirectoryDepthIsNull() {
		// Act + Assert
		NullPointerException exception = assertThrows(
				NullPointerException.class,
				() -> PocketFilesConfig.builder()
						.baseDirectory(BASE_DIRECTORY)
						.directoryDepth(null)
						.build());

		assertEquals("directoryDepth must not be null", exception.getMessage());
	}
}