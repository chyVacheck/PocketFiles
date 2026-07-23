package com.chyvacheck.pocketfiles.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StagedFileTest {

	private static final String ORIGINAL_NAME = "photo.png";

	private static final String EXTENSION = "png";

	private static final long SIZE_BYTES = 5L;

	private static final String SHA256 = "185f8db32271fe25f561a6fc938b2e264306ec304eda518007d1764826381969";

	@TempDir
	Path tempDir;

	// ? constructor

	@Test
	void shouldThrowExceptionWhenOriginalNameIsNull() {
		NullPointerException exception = assertThrows(
				NullPointerException.class,
				() -> new StagedFile(
						null,
						EXTENSION,
						SIZE_BYTES,
						SHA256,
						this.tempDir.resolve("file.tmp")));

		assertEquals("originalName must not be null", exception.getMessage());
	}

	@Test
	void shouldThrowExceptionWhenOriginalNameIsBlank() {
		IllegalArgumentException exception = assertThrows(
				IllegalArgumentException.class,
				() -> new StagedFile(
						"   ",
						EXTENSION,
						SIZE_BYTES,
						SHA256,
						this.tempDir.resolve("file.tmp")));

		assertEquals("originalName must not be blank", exception.getMessage());
	}

	@Test
	void shouldThrowExceptionWhenExtensionIsBlank() {
		IllegalArgumentException exception = assertThrows(
				IllegalArgumentException.class,
				() -> new StagedFile(
						ORIGINAL_NAME,
						"   ",
						SIZE_BYTES,
						SHA256,
						this.tempDir.resolve("file.tmp")));

		assertEquals("extension must not be blank", exception.getMessage());
	}

	@Test
	void shouldThrowExceptionWhenSizeBytesIsNegative() {
		IllegalArgumentException exception = assertThrows(
				IllegalArgumentException.class,
				() -> new StagedFile(
						ORIGINAL_NAME,
						EXTENSION,
						-1L,
						SHA256,
						this.tempDir.resolve("file.tmp")));

		assertEquals("sizeBytes must not be negative", exception.getMessage());
	}

	@Test
	void shouldThrowExceptionWhenSha256IsNull() {
		NullPointerException exception = assertThrows(
				NullPointerException.class,
				() -> new StagedFile(
						ORIGINAL_NAME,
						EXTENSION,
						SIZE_BYTES,
						null,
						this.tempDir.resolve("file.tmp")));

		assertEquals("sha256 must not be null", exception.getMessage());
	}

	@Test
	void shouldThrowExceptionWhenSha256IsBlank() {
		IllegalArgumentException exception = assertThrows(
				IllegalArgumentException.class,
				() -> new StagedFile(
						ORIGINAL_NAME,
						EXTENSION,
						SIZE_BYTES,
						"   ",
						this.tempDir.resolve("file.tmp")));

		assertEquals("sha256 must not be blank", exception.getMessage());
	}

	@Test
	void shouldThrowExceptionWhenTempFilePathIsNull() {
		NullPointerException exception = assertThrows(
				NullPointerException.class,
				() -> new StagedFile(
						ORIGINAL_NAME,
						EXTENSION,
						SIZE_BYTES,
						SHA256,
						null));

		assertEquals("tempFilePath must not be null", exception.getMessage());
	}

	@Test
	void shouldCreateStagedFile() {
		Path tempFilePath = this.tempDir.resolve("file.tmp");

		StagedFile stagedFile = new StagedFile(
				ORIGINAL_NAME,
				EXTENSION,
				SIZE_BYTES,
				SHA256,
				tempFilePath);

		assertEquals(ORIGINAL_NAME, stagedFile.originalName());
		assertEquals(EXTENSION, stagedFile.extension());
		assertEquals(SIZE_BYTES, stagedFile.sizeBytes());
		assertEquals(SHA256, stagedFile.sha256());
		assertEquals(tempFilePath, stagedFile.tempFilePath());
	}

	@Test
	void shouldCreateStagedFileWithoutExtension() {
		Path tempFilePath = this.tempDir.resolve("file.tmp");

		StagedFile stagedFile = new StagedFile(
				"README",
				null,
				SIZE_BYTES,
				SHA256,
				tempFilePath);

		assertEquals("README", stagedFile.originalName());
		assertNull(stagedFile.extension());
		assertEquals(SIZE_BYTES, stagedFile.sizeBytes());
		assertEquals(SHA256, stagedFile.sha256());
		assertEquals(tempFilePath, stagedFile.tempFilePath());
	}

	@Test
	void shouldTrimValues() {
		Path tempFilePath = this.tempDir.resolve("file.tmp");

		StagedFile stagedFile = new StagedFile(
				"  photo.png  ",
				"  png  ",
				SIZE_BYTES,
				"  " + SHA256 + "  ",
				tempFilePath);

		assertEquals(ORIGINAL_NAME, stagedFile.originalName());
		assertEquals(EXTENSION, stagedFile.extension());
		assertEquals(SIZE_BYTES, stagedFile.sizeBytes());
		assertEquals(SHA256, stagedFile.sha256());
		assertEquals(tempFilePath, stagedFile.tempFilePath());
	}

	// ? at

	@Test
	void shouldCreateStagedFileUsingFactoryMethod() {
		Path tempFilePath = this.tempDir.resolve("file.tmp");

		StagedFile stagedFile = StagedFile.at(
				ORIGINAL_NAME,
				EXTENSION,
				SIZE_BYTES,
				SHA256,
				tempFilePath);

		assertEquals(ORIGINAL_NAME, stagedFile.originalName());
		assertEquals(EXTENSION, stagedFile.extension());
		assertEquals(SIZE_BYTES, stagedFile.sizeBytes());
		assertEquals(SHA256, stagedFile.sha256());
		assertEquals(tempFilePath, stagedFile.tempFilePath());
	}
}
