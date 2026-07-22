package com.chyvacheck.pocketfiles.service;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SaveFileCommandTest {
	private static final String ORIGINAL_NAME = "photo.png";

	private static final String MIME_TYPE = "image/png";

	private static final String USAGE_TYPE = "invoice_attachment";

	private static final String OWNER_TYPE = "invoice";

	private static final String OWNER_ID = "777";

	private static final String DISPLAY_NAME = "Invoice January.pdf";

	private static final String METADATA_JSON = """
			{"source":"upload-form","category":"invoice"}
			""";

	@Test
	void shouldCreateSaveFileCommand() {
		InputStream inputStream = this.createInputStream();

		SaveFileCommand command = SaveFileCommand.at(
				inputStream,
				ORIGINAL_NAME,
				MIME_TYPE,
				USAGE_TYPE,
				OWNER_TYPE,
				OWNER_ID,
				DISPLAY_NAME,
				METADATA_JSON);

		assertSame(inputStream, command.inputStream());
		assertEquals(ORIGINAL_NAME, command.originalName());
		assertEquals(MIME_TYPE, command.mimeType());
		assertEquals(USAGE_TYPE, command.usageType());
		assertEquals(OWNER_TYPE, command.ownerType());
		assertEquals(OWNER_ID, command.ownerId());
		assertEquals(DISPLAY_NAME, command.displayName());
		assertEquals(METADATA_JSON.trim(), command.metadataJson());
	}

	@Test
	void shouldCreateSaveFileCommandWithRequiredFieldsOnly() {
		InputStream inputStream = this.createInputStream();

		SaveFileCommand command = SaveFileCommand.of(inputStream, ORIGINAL_NAME);

		assertSame(inputStream, command.inputStream());
		assertEquals(ORIGINAL_NAME, command.originalName());
		assertNull(command.mimeType());
		assertNull(command.usageType());
		assertNull(command.ownerType());
		assertNull(command.ownerId());
		assertNull(command.displayName());
		assertNull(command.metadataJson());
	}

	@Test
	void shouldTrimStringValues() {
		InputStream inputStream = this.createInputStream();

		SaveFileCommand command = SaveFileCommand.at(
				inputStream,
				" photo.png ",
				" image/png ",
				" invoice_attachment ",
				" invoice ",
				" 777 ",
				" Invoice January.pdf ",
				" " + METADATA_JSON.trim() + " ");

		assertEquals("photo.png", command.originalName());
		assertEquals("image/png", command.mimeType());
		assertEquals("invoice_attachment", command.usageType());
		assertEquals("invoice", command.ownerType());
		assertEquals("777", command.ownerId());
		assertEquals("Invoice January.pdf", command.displayName());
		assertEquals(METADATA_JSON.trim(), command.metadataJson());
	}

	@Test
	void shouldThrowExceptionWhenInputStreamIsNull() {
		NullPointerException exception = assertThrows(
				NullPointerException.class,
				() -> SaveFileCommand.of(null, ORIGINAL_NAME));

		assertEquals("inputStream must not be null", exception.getMessage());
	}

	@Test
	void shouldThrowExceptionWhenOriginalNameIsNull() {
		NullPointerException exception = assertThrows(
				NullPointerException.class,
				() -> SaveFileCommand.of(this.createInputStream(), null));

		assertEquals("originalName must not be null", exception.getMessage());
	}

	@Test
	void shouldThrowExceptionWhenOriginalNameIsBlank() {
		IllegalArgumentException exception = assertThrows(
				IllegalArgumentException.class,
				() -> SaveFileCommand.of(this.createInputStream(), "   "));

		assertEquals("originalName must not be blank", exception.getMessage());
	}

	@Test
	void shouldThrowExceptionWhenMimeTypeIsBlank() {
		IllegalArgumentException exception = assertThrows(
				IllegalArgumentException.class,
				() -> SaveFileCommand.at(
						this.createInputStream(),
						ORIGINAL_NAME,
						"   ",
						USAGE_TYPE,
						OWNER_TYPE,
						OWNER_ID,
						DISPLAY_NAME,
						METADATA_JSON));

		assertEquals("mimeType must not be blank", exception.getMessage());
	}

	@Test
	void shouldThrowExceptionWhenUsageTypeIsBlank() {
		IllegalArgumentException exception = assertThrows(
				IllegalArgumentException.class,
				() -> SaveFileCommand.at(
						this.createInputStream(),
						ORIGINAL_NAME,
						MIME_TYPE,
						"   ",
						OWNER_TYPE,
						OWNER_ID,
						DISPLAY_NAME,
						METADATA_JSON));

		assertEquals("usageType must not be blank", exception.getMessage());
	}

	@Test
	void shouldThrowExceptionWhenOwnerTypeIsBlank() {
		IllegalArgumentException exception = assertThrows(
				IllegalArgumentException.class,
				() -> SaveFileCommand.at(
						this.createInputStream(),
						ORIGINAL_NAME,
						MIME_TYPE,
						USAGE_TYPE,
						"   ",
						OWNER_ID,
						DISPLAY_NAME,
						METADATA_JSON));

		assertEquals("ownerType must not be blank", exception.getMessage());
	}

	@Test
	void shouldThrowExceptionWhenOwnerIdIsBlank() {
		IllegalArgumentException exception = assertThrows(
				IllegalArgumentException.class,
				() -> SaveFileCommand.at(
						this.createInputStream(),
						ORIGINAL_NAME,
						MIME_TYPE,
						USAGE_TYPE,
						OWNER_TYPE,
						"   ",
						DISPLAY_NAME,
						METADATA_JSON));

		assertEquals("ownerId must not be blank", exception.getMessage());
	}

	@Test
	void shouldThrowExceptionWhenDisplayNameIsBlank() {
		IllegalArgumentException exception = assertThrows(
				IllegalArgumentException.class,
				() -> SaveFileCommand.at(
						this.createInputStream(),
						ORIGINAL_NAME,
						MIME_TYPE,
						USAGE_TYPE,
						OWNER_TYPE,
						OWNER_ID,
						"   ",
						METADATA_JSON));

		assertEquals("displayName must not be blank", exception.getMessage());
	}

	@Test
	void shouldThrowExceptionWhenMetadataJsonIsBlank() {
		IllegalArgumentException exception = assertThrows(
				IllegalArgumentException.class,
				() -> SaveFileCommand.at(
						this.createInputStream(),
						ORIGINAL_NAME,
						MIME_TYPE,
						USAGE_TYPE,
						OWNER_TYPE,
						OWNER_ID,
						DISPLAY_NAME,
						"   "));

		assertEquals("metadataJson must not be blank", exception.getMessage());
	}

	private InputStream createInputStream() {
		return new ByteArrayInputStream("Hello".getBytes(StandardCharsets.UTF_8));
	}
}
