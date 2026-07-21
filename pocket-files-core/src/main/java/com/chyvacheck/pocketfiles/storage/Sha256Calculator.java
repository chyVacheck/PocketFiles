package com.chyvacheck.pocketfiles.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

public final class Sha256Calculator {

	private static final int BUFFER_SIZE = 8192;

	public String calculate(Path filePath) throws IOException {
		Objects.requireNonNull(filePath, "filePath must not be null");

		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");

			try (InputStream inputStream = Files.newInputStream(filePath)) {
				byte[] buffer = new byte[Sha256Calculator.BUFFER_SIZE];

				int bytesRead;

				while ((bytesRead = inputStream.read(buffer)) != -1) {
					// передать прочитанные байты в MessageDigest
					digest.update(buffer, 0, bytesRead);
				}
			}

			byte[] hashBytes = digest.digest();

			return Sha256Calculator.toHex(hashBytes);
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 algorithm is not available", exception);
		}
	}

	private static String toHex(byte[] bytes) {
		StringBuilder builder = new StringBuilder(bytes.length * 2);

		for (byte item : bytes) {
			builder.append(String.format("%02x", item));
		}

		return builder.toString();
	}
}
