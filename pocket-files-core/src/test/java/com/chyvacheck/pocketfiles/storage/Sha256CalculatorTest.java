package com.chyvacheck.pocketfiles.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class Sha256CalculatorTest {

	@TempDir
	Path tempDir;

	private static final String HELLO_CONTENT = "Hello";

	private static final String HELLO_SHA256 = "185f8db32271fe25f561a6fc938b2e264306ec304eda518007d1764826381969";

	@Test
	void shouldCalculateSha256() throws IOException {
		// Arrange
		Sha256Calculator sha256Calculator = new Sha256Calculator();
		Path filePath = createFile("hello.txt", HELLO_CONTENT);

		// Act
		String sha256 = sha256Calculator.calculate(filePath);

		assertEquals(HELLO_SHA256, sha256);
	}

	@Test
	void shouldThrowExceptionWhenFilePathIsNull() {
		// Arrange
		Sha256Calculator calculator = new Sha256Calculator();

		// Act + Assert
		NullPointerException exception = assertThrows(
				NullPointerException.class,
				() -> calculator.calculate(null));

		assertEquals("filePath must not be null", exception.getMessage());
	}

	@Test
	void shouldCalculateTheSameHashForSameContent() throws IOException {
		// Arrange
		Sha256Calculator calculator = new Sha256Calculator();

		Path firstFilePath = createFile("first.txt", HELLO_CONTENT);
		Path secondFilePath = createFile("second.txt", HELLO_CONTENT);

		// Act
		String firstHash = calculator.calculate(firstFilePath);
		String secondHash = calculator.calculate(secondFilePath);

		// Assert
		assertEquals(firstHash, secondHash);
	}

	@Test
	void shouldCalculateDifferentHashesForDifferentFiles() throws IOException {
		// Arrange
		Sha256Calculator calculator = new Sha256Calculator();

		Path firstFilePath = createFile("first.txt", HELLO_CONTENT);
		Path secondFilePath = createFile("second.txt", "World");

		// Act
		String firstHash = calculator.calculate(firstFilePath);
		String secondHash = calculator.calculate(secondFilePath);

		// Assert
		assertNotEquals(firstHash, secondHash);
	}

	private Path createFile(String fileName, String content) throws IOException {
		Path filePath = this.tempDir.resolve(fileName);

		Files.writeString(filePath, content, StandardCharsets.UTF_8);

		return filePath;
	}
}
