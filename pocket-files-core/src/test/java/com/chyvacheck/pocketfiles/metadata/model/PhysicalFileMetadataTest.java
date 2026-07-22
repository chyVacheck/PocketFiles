package com.chyvacheck.pocketfiles.metadata.model;

import org.junit.jupiter.api.Test;

import com.chyvacheck.pocketfiles.metadata.status.PhysicalFileStatus;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNull;

class PhysicalFileMetadataTest {

	private static final Long ID = 1L;

	private static final UUID UUID_VALUE = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

	private static final String ORIGINAL_NAME = "photo.png";

	private static final String RELATIVE_PATH = "2026/01/02/550e8400-e29b-41d4-a716-446655440000.png";

	private static final String MIME_TYPE = "image/png";

	private static final String EXTENSION = "png";

	private static final long SIZE_BYTES = 5L;

	private static final String SHA256 = "185f8db32271fe25f561a6fc938b2e264306ec304eda518007d1764826381969";

	private static final PhysicalFileStatus STATUS = PhysicalFileStatus.ACTIVE;

	private static final long CREATED_AT = 1760000000000L;

	private static final long STATUS_CHANGED_AT = 1760000000000L;

	private static final Long DELETED_AT = null;

	@Test
	void shouldCreatePhysicalFileMetadata() {
		// Act
		PhysicalFileMetadata metadata = this.createPhysicalFileMetadata();

		// Assert
		assertEquals(ID, metadata.id());
		assertEquals(UUID_VALUE, metadata.uuid());
		assertEquals(ORIGINAL_NAME, metadata.originalName());
		assertEquals(RELATIVE_PATH, metadata.relativePath());
		assertEquals(MIME_TYPE, metadata.mimeType());
		assertEquals(EXTENSION, metadata.extension());
		assertEquals(SIZE_BYTES, metadata.sizeBytes());
		assertEquals(SHA256, metadata.sha256());
		assertEquals(STATUS, metadata.status());
		assertEquals(CREATED_AT, metadata.createdAt());
		assertEquals(STATUS_CHANGED_AT, metadata.statusChangedAt());
		assertEquals(DELETED_AT, metadata.deletedAt());
	}

	@Test
	void shouldCreateNewFileMetadata() {
		// Act
		PhysicalFileMetadata metadata = PhysicalFileMetadata.newFile(
				UUID_VALUE,
				ORIGINAL_NAME,
				RELATIVE_PATH,
				MIME_TYPE,
				EXTENSION,
				SIZE_BYTES,
				SHA256,
				CREATED_AT);

		// Assert
		assertNull(metadata.id());
		assertEquals(UUID_VALUE, metadata.uuid());
		assertEquals(ORIGINAL_NAME, metadata.originalName());
		assertEquals(RELATIVE_PATH, metadata.relativePath());
		assertEquals(MIME_TYPE, metadata.mimeType());
		assertEquals(EXTENSION, metadata.extension());
		assertEquals(SIZE_BYTES, metadata.sizeBytes());
		assertEquals(SHA256, metadata.sha256());
		assertEquals(PhysicalFileStatus.ACTIVE, metadata.status());
		assertEquals(CREATED_AT, metadata.createdAt());
		assertEquals(CREATED_AT, metadata.statusChangedAt());
		assertNull(metadata.deletedAt());
	}

	@Test
	void shouldTrimStringValues() {
		// Act
		PhysicalFileMetadata metadata = PhysicalFileMetadata.at(
				ID,
				UUID_VALUE,
				" photo.png ",
				" 2026/01/02/file.png ",
				" image/png ",
				" png ",
				SIZE_BYTES,
				" " + SHA256 + " ",
				STATUS,
				CREATED_AT,
				STATUS_CHANGED_AT,
				DELETED_AT);

		// Assert
		assertEquals("photo.png", metadata.originalName());
		assertEquals("2026/01/02/file.png", metadata.relativePath());
		assertEquals("image/png", metadata.mimeType());
		assertEquals("png", metadata.extension());
		assertEquals(SHA256, metadata.sha256());
	}

	@Test
	void shouldAllowNullableFields() {
		// Act
		PhysicalFileMetadata metadata = PhysicalFileMetadata.at(
				null,
				UUID_VALUE,
				"README",
				"2026/01/02/README",
				null,
				null,
				SIZE_BYTES,
				SHA256,
				STATUS,
				CREATED_AT,
				STATUS_CHANGED_AT,
				null);

		// Assert
		assertNull(metadata.id());
		assertNull(metadata.mimeType());
		assertNull(metadata.extension());
		assertNull(metadata.deletedAt());
	}

	@Test
	void shouldThrowExceptionWhenUuidIsNull() {
		// Act + Assert
		NullPointerException exception = assertThrows(
				NullPointerException.class,
				() -> PhysicalFileMetadata.at(
						ID,
						null,
						ORIGINAL_NAME,
						RELATIVE_PATH,
						MIME_TYPE,
						EXTENSION,
						SIZE_BYTES,
						SHA256,
						STATUS,
						CREATED_AT,
						STATUS_CHANGED_AT,
						DELETED_AT));

		// Assert
		assertEquals("uuid must not be null", exception.getMessage());
	}

	@Test
	void shouldThrowExceptionWhenOriginalNameIsNull() {
		NullPointerException exception = assertThrows(
				NullPointerException.class,
				() -> PhysicalFileMetadata.at(
						ID,
						UUID_VALUE,
						null,
						RELATIVE_PATH,
						MIME_TYPE,
						EXTENSION,
						SIZE_BYTES,
						SHA256,
						STATUS,
						CREATED_AT,
						STATUS_CHANGED_AT,
						DELETED_AT));

		assertEquals("originalName must not be null", exception.getMessage());
	}

	@Test
	void shouldThrowExceptionWhenOriginalNameIsBlank() {
		IllegalArgumentException exception = assertThrows(
				IllegalArgumentException.class,
				() -> PhysicalFileMetadata.at(
						ID,
						UUID_VALUE,
						"   ",
						RELATIVE_PATH,
						MIME_TYPE,
						EXTENSION,
						SIZE_BYTES,
						SHA256,
						STATUS,
						CREATED_AT,
						STATUS_CHANGED_AT,
						DELETED_AT));

		assertEquals("originalName must not be blank", exception.getMessage());
	}

	@Test
	void shouldThrowExceptionWhenRelativePathIsNull() {
		NullPointerException exception = assertThrows(
				NullPointerException.class,
				() -> PhysicalFileMetadata.at(
						ID,
						UUID_VALUE,
						ORIGINAL_NAME,
						null,
						MIME_TYPE,
						EXTENSION,
						SIZE_BYTES,
						SHA256,
						STATUS,
						CREATED_AT,
						STATUS_CHANGED_AT,
						DELETED_AT));

		assertEquals("relativePath must not be null", exception.getMessage());
	}

	@Test
	void shouldThrowExceptionWhenRelativePathIsBlank() {
		IllegalArgumentException exception = assertThrows(
				IllegalArgumentException.class,
				() -> PhysicalFileMetadata.at(
						ID,
						UUID_VALUE,
						ORIGINAL_NAME,
						"   ",
						MIME_TYPE,
						EXTENSION,
						SIZE_BYTES,
						SHA256,
						STATUS,
						CREATED_AT,
						STATUS_CHANGED_AT,
						DELETED_AT));

		assertEquals("relativePath must not be blank", exception.getMessage());
	}

	@Test
	void shouldThrowExceptionWhenMimeTypeIsBlank() {
		IllegalArgumentException exception = assertThrows(
				IllegalArgumentException.class,
				() -> PhysicalFileMetadata.at(
						ID,
						UUID_VALUE,
						ORIGINAL_NAME,
						RELATIVE_PATH,
						"   ",
						EXTENSION,
						SIZE_BYTES,
						SHA256,
						STATUS,
						CREATED_AT,
						STATUS_CHANGED_AT,
						DELETED_AT));

		assertEquals("mimeType must not be blank", exception.getMessage());
	}

	@Test
	void shouldThrowExceptionWhenExtensionIsBlank() {
		IllegalArgumentException exception = assertThrows(
				IllegalArgumentException.class,
				() -> PhysicalFileMetadata.at(
						ID,
						UUID_VALUE,
						ORIGINAL_NAME,
						RELATIVE_PATH,
						MIME_TYPE,
						"   ",
						SIZE_BYTES,
						SHA256,
						STATUS,
						CREATED_AT,
						STATUS_CHANGED_AT,
						DELETED_AT));

		assertEquals("extension must not be blank", exception.getMessage());
	}

	@Test
	void shouldThrowExceptionWhenSizeBytesIsNegative() {
		IllegalArgumentException exception = assertThrows(
				IllegalArgumentException.class,
				() -> PhysicalFileMetadata.at(
						ID,
						UUID_VALUE,
						ORIGINAL_NAME,
						RELATIVE_PATH,
						MIME_TYPE,
						EXTENSION,
						-1L,
						SHA256,
						STATUS,
						CREATED_AT,
						STATUS_CHANGED_AT,
						DELETED_AT));

		assertEquals("sizeBytes must not be negative", exception.getMessage());
	}

	@Test
	void shouldThrowExceptionWhenSha256IsNull() {
		NullPointerException exception = assertThrows(
				NullPointerException.class,
				() -> PhysicalFileMetadata.at(
						ID,
						UUID_VALUE,
						ORIGINAL_NAME,
						RELATIVE_PATH,
						MIME_TYPE,
						EXTENSION,
						SIZE_BYTES,
						null,
						STATUS,
						CREATED_AT,
						STATUS_CHANGED_AT,
						DELETED_AT));

		assertEquals("sha256 must not be null", exception.getMessage());
	}

	@Test
	void shouldThrowExceptionWhenSha256IsBlank() {
		IllegalArgumentException exception = assertThrows(
				IllegalArgumentException.class,
				() -> PhysicalFileMetadata.at(
						ID,
						UUID_VALUE,
						ORIGINAL_NAME,
						RELATIVE_PATH,
						MIME_TYPE,
						EXTENSION,
						SIZE_BYTES,
						"   ",
						STATUS,
						CREATED_AT,
						STATUS_CHANGED_AT,
						DELETED_AT));

		assertEquals("sha256 must not be blank", exception.getMessage());
	}

	@Test
	void shouldThrowExceptionWhenStatusIsNull() {
		NullPointerException exception = assertThrows(
				NullPointerException.class,
				() -> PhysicalFileMetadata.at(
						ID,
						UUID_VALUE,
						ORIGINAL_NAME,
						RELATIVE_PATH,
						MIME_TYPE,
						EXTENSION,
						SIZE_BYTES,
						SHA256,
						null,
						CREATED_AT,
						STATUS_CHANGED_AT,
						DELETED_AT));

		assertEquals("status must not be null", exception.getMessage());
	}

	@Test
	void shouldThrowExceptionWhenCreatedAtIsNegative() {
		IllegalArgumentException exception = assertThrows(
				IllegalArgumentException.class,
				() -> PhysicalFileMetadata.at(
						ID,
						UUID_VALUE,
						ORIGINAL_NAME,
						RELATIVE_PATH,
						MIME_TYPE,
						EXTENSION,
						SIZE_BYTES,
						SHA256,
						STATUS,
						-1L,
						STATUS_CHANGED_AT,
						DELETED_AT));

		assertEquals("createdAt must not be negative", exception.getMessage());
	}

	@Test
	void shouldThrowExceptionWhenStatusChangedAtIsNegative() {
		IllegalArgumentException exception = assertThrows(
				IllegalArgumentException.class,
				() -> PhysicalFileMetadata.at(
						ID,
						UUID_VALUE,
						ORIGINAL_NAME,
						RELATIVE_PATH,
						MIME_TYPE,
						EXTENSION,
						SIZE_BYTES,
						SHA256,
						STATUS,
						CREATED_AT,
						-1L,
						DELETED_AT));

		assertEquals("statusChangedAt must not be negative", exception.getMessage());
	}

	@Test
	void shouldThrowExceptionWhenDeletedAtIsNegative() {
		IllegalArgumentException exception = assertThrows(
				IllegalArgumentException.class,
				() -> PhysicalFileMetadata.at(
						ID,
						UUID_VALUE,
						ORIGINAL_NAME,
						RELATIVE_PATH,
						MIME_TYPE,
						EXTENSION,
						SIZE_BYTES,
						SHA256,
						STATUS,
						CREATED_AT,
						STATUS_CHANGED_AT,
						-1L));

		assertEquals("deletedAt must not be negative", exception.getMessage());
	}

	private PhysicalFileMetadata createPhysicalFileMetadata() {
		return PhysicalFileMetadata.at(
				ID,
				UUID_VALUE,
				ORIGINAL_NAME,
				RELATIVE_PATH,
				MIME_TYPE,
				EXTENSION,
				SIZE_BYTES,
				SHA256,
				STATUS,
				CREATED_AT,
				STATUS_CHANGED_AT,
				DELETED_AT);
	}
}
