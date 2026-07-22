package com.chyvacheck.pocketfiles.metadata.model;

import com.chyvacheck.pocketfiles.metadata.status.FileUsageStatus;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FileUsageMetadataTest {
	private static final Long ID = 1L;

	private static final UUID UUID_VALUE = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

	private static final long PHYSICAL_FILE_ID = 10L;

	private static final String USAGE_TYPE = "invoice_attachment";

	private static final String OWNER_TYPE = "invoice";

	private static final String OWNER_ID = "777";

	private static final String DISPLAY_NAME = "Invoice January.pdf";

	private static final String METADATA_JSON = """
			{"source":"upload-form","category":"invoice"}
			""";

	private static final FileUsageStatus STATUS = FileUsageStatus.ACTIVE;

	private static final long CREATED_AT = 1760000000000L;

	private static final Long DELETED_AT = null;

	@Test
	void shouldCreateFileUsageMetadata() {
		FileUsageMetadata metadata = this.createFileUsageMetadata();

		assertEquals(ID, metadata.id());
		assertEquals(UUID_VALUE, metadata.uuid());
		assertEquals(PHYSICAL_FILE_ID, metadata.physicalFileId());
		assertEquals(USAGE_TYPE, metadata.usageType());
		assertEquals(OWNER_TYPE, metadata.ownerType());
		assertEquals(OWNER_ID, metadata.ownerId());
		assertEquals(DISPLAY_NAME, metadata.displayName());
		assertEquals(METADATA_JSON.trim(), metadata.metadataJson());
		assertEquals(STATUS, metadata.status());
		assertEquals(CREATED_AT, metadata.createdAt());
		assertNull(metadata.deletedAt());
	}

	@Test
	void shouldCreateNewUsageMetadata() {
		FileUsageMetadata metadata = FileUsageMetadata.newUsage(
				UUID_VALUE,
				PHYSICAL_FILE_ID,
				USAGE_TYPE,
				OWNER_TYPE,
				OWNER_ID,
				DISPLAY_NAME,
				METADATA_JSON,
				CREATED_AT);

		assertNull(metadata.id());
		assertEquals(UUID_VALUE, metadata.uuid());
		assertEquals(PHYSICAL_FILE_ID, metadata.physicalFileId());
		assertEquals(USAGE_TYPE, metadata.usageType());
		assertEquals(OWNER_TYPE, metadata.ownerType());
		assertEquals(OWNER_ID, metadata.ownerId());
		assertEquals(DISPLAY_NAME, metadata.displayName());
		assertEquals(METADATA_JSON.trim(), metadata.metadataJson());
		assertEquals(FileUsageStatus.ACTIVE, metadata.status());
		assertEquals(CREATED_AT, metadata.createdAt());
		assertNull(metadata.deletedAt());
	}

	@Test
	void shouldUseDefaultUsageTypeWhenNewUsageTypeIsNull() {
		FileUsageMetadata metadata = FileUsageMetadata.newUsage(
				UUID_VALUE,
				PHYSICAL_FILE_ID,
				null,
				OWNER_TYPE,
				OWNER_ID,
				DISPLAY_NAME,
				METADATA_JSON,
				CREATED_AT);

		assertEquals("default", metadata.usageType());
	}

	@Test
	void shouldUseDefaultUsageTypeWhenNewUsageTypeIsBlank() {
		FileUsageMetadata metadata = FileUsageMetadata.newUsage(
				UUID_VALUE,
				PHYSICAL_FILE_ID,
				"   ",
				OWNER_TYPE,
				OWNER_ID,
				DISPLAY_NAME,
				METADATA_JSON,
				CREATED_AT);

		assertEquals("default", metadata.usageType());
	}

	@Test
	void shouldTrimStringValues() {
		FileUsageMetadata metadata = FileUsageMetadata.at(
				ID,
				UUID_VALUE,
				PHYSICAL_FILE_ID,
				" invoice_attachment ",
				" invoice ",
				" 777 ",
				" Invoice January.pdf ",
				" " + METADATA_JSON.trim() + " ",
				STATUS,
				CREATED_AT,
				DELETED_AT);

		assertEquals("invoice_attachment", metadata.usageType());
		assertEquals("invoice", metadata.ownerType());
		assertEquals("777", metadata.ownerId());
		assertEquals("Invoice January.pdf", metadata.displayName());
		assertEquals(METADATA_JSON.trim(), metadata.metadataJson());
	}

	@Test
	void shouldAllowNullableOptionalFields() {
		FileUsageMetadata metadata = FileUsageMetadata.at(
				null,
				UUID_VALUE,
				PHYSICAL_FILE_ID,
				USAGE_TYPE,
				null,
				null,
				null,
				null,
				STATUS,
				CREATED_AT,
				null);

		assertNull(metadata.id());
		assertNull(metadata.ownerType());
		assertNull(metadata.ownerId());
		assertNull(metadata.displayName());
		assertNull(metadata.metadataJson());
		assertNull(metadata.deletedAt());
	}

	@Test
	void shouldThrowExceptionWhenUuidIsNull() {
		NullPointerException exception = assertThrows(
				NullPointerException.class,
				() -> FileUsageMetadata.at(
						ID,
						null,
						PHYSICAL_FILE_ID,
						USAGE_TYPE,
						OWNER_TYPE,
						OWNER_ID,
						DISPLAY_NAME,
						METADATA_JSON,
						STATUS,
						CREATED_AT,
						DELETED_AT));

		assertEquals("uuid must not be null", exception.getMessage());
	}

	@Test
	void shouldThrowExceptionWhenPhysicalFileIdIsZero() {
		IllegalArgumentException exception = assertThrows(
				IllegalArgumentException.class,
				() -> FileUsageMetadata.at(
						ID,
						UUID_VALUE,
						0L,
						USAGE_TYPE,
						OWNER_TYPE,
						OWNER_ID,
						DISPLAY_NAME,
						METADATA_JSON,
						STATUS,
						CREATED_AT,
						DELETED_AT));

		assertEquals("physicalFileId must be positive", exception.getMessage());
	}

	@Test
	void shouldThrowExceptionWhenPhysicalFileIdIsNegative() {
		IllegalArgumentException exception = assertThrows(
				IllegalArgumentException.class,
				() -> FileUsageMetadata.at(
						ID,
						UUID_VALUE,
						-1L,
						USAGE_TYPE,
						OWNER_TYPE,
						OWNER_ID,
						DISPLAY_NAME,
						METADATA_JSON,
						STATUS,
						CREATED_AT,
						DELETED_AT));

		assertEquals("physicalFileId must be positive", exception.getMessage());
	}

	@Test
	void shouldThrowExceptionWhenUsageTypeIsNull() {
		NullPointerException exception = assertThrows(
				NullPointerException.class,
				() -> FileUsageMetadata.at(
						ID,
						UUID_VALUE,
						PHYSICAL_FILE_ID,
						null,
						OWNER_TYPE,
						OWNER_ID,
						DISPLAY_NAME,
						METADATA_JSON,
						STATUS,
						CREATED_AT,
						DELETED_AT));

		assertEquals("usageType must not be null", exception.getMessage());
	}

	@Test
	void shouldThrowExceptionWhenUsageTypeIsBlank() {
		IllegalArgumentException exception = assertThrows(
				IllegalArgumentException.class,
				() -> FileUsageMetadata.at(
						ID,
						UUID_VALUE,
						PHYSICAL_FILE_ID,
						"   ",
						OWNER_TYPE,
						OWNER_ID,
						DISPLAY_NAME,
						METADATA_JSON,
						STATUS,
						CREATED_AT,
						DELETED_AT));

		assertEquals("usageType must not be blank", exception.getMessage());
	}

	@Test
	void shouldThrowExceptionWhenOwnerTypeIsBlank() {
		IllegalArgumentException exception = assertThrows(
				IllegalArgumentException.class,
				() -> FileUsageMetadata.at(
						ID,
						UUID_VALUE,
						PHYSICAL_FILE_ID,
						USAGE_TYPE,
						"   ",
						OWNER_ID,
						DISPLAY_NAME,
						METADATA_JSON,
						STATUS,
						CREATED_AT,
						DELETED_AT));

		assertEquals("ownerType must not be blank", exception.getMessage());
	}

	@Test
	void shouldThrowExceptionWhenOwnerIdIsBlank() {
		IllegalArgumentException exception = assertThrows(
				IllegalArgumentException.class,
				() -> FileUsageMetadata.at(
						ID,
						UUID_VALUE,
						PHYSICAL_FILE_ID,
						USAGE_TYPE,
						OWNER_TYPE,
						"   ",
						DISPLAY_NAME,
						METADATA_JSON,
						STATUS,
						CREATED_AT,
						DELETED_AT));

		assertEquals("ownerId must not be blank", exception.getMessage());
	}

	@Test
	void shouldThrowExceptionWhenDisplayNameIsBlank() {
		IllegalArgumentException exception = assertThrows(
				IllegalArgumentException.class,
				() -> FileUsageMetadata.at(
						ID,
						UUID_VALUE,
						PHYSICAL_FILE_ID,
						USAGE_TYPE,
						OWNER_TYPE,
						OWNER_ID,
						"   ",
						METADATA_JSON,
						STATUS,
						CREATED_AT,
						DELETED_AT));

		assertEquals("displayName must not be blank", exception.getMessage());
	}

	@Test
	void shouldThrowExceptionWhenMetadataJsonIsBlank() {
		IllegalArgumentException exception = assertThrows(
				IllegalArgumentException.class,
				() -> FileUsageMetadata.at(
						ID,
						UUID_VALUE,
						PHYSICAL_FILE_ID,
						USAGE_TYPE,
						OWNER_TYPE,
						OWNER_ID,
						DISPLAY_NAME,
						"   ",
						STATUS,
						CREATED_AT,
						DELETED_AT));

		assertEquals("metadataJson must not be blank", exception.getMessage());
	}

	@Test
	void shouldThrowExceptionWhenStatusIsNull() {
		NullPointerException exception = assertThrows(
				NullPointerException.class,
				() -> FileUsageMetadata.at(
						ID,
						UUID_VALUE,
						PHYSICAL_FILE_ID,
						USAGE_TYPE,
						OWNER_TYPE,
						OWNER_ID,
						DISPLAY_NAME,
						METADATA_JSON,
						null,
						CREATED_AT,
						DELETED_AT));

		assertEquals("status must not be null", exception.getMessage());
	}

	@Test
	void shouldThrowExceptionWhenCreatedAtIsNegative() {
		IllegalArgumentException exception = assertThrows(
				IllegalArgumentException.class,
				() -> FileUsageMetadata.at(
						ID,
						UUID_VALUE,
						PHYSICAL_FILE_ID,
						USAGE_TYPE,
						OWNER_TYPE,
						OWNER_ID,
						DISPLAY_NAME,
						METADATA_JSON,
						STATUS,
						-1L,
						DELETED_AT));

		assertEquals("createdAt must not be negative", exception.getMessage());
	}

	@Test
	void shouldThrowExceptionWhenDeletedAtIsNegative() {
		IllegalArgumentException exception = assertThrows(
				IllegalArgumentException.class,
				() -> FileUsageMetadata.at(
						ID,
						UUID_VALUE,
						PHYSICAL_FILE_ID,
						USAGE_TYPE,
						OWNER_TYPE,
						OWNER_ID,
						DISPLAY_NAME,
						METADATA_JSON,
						STATUS,
						CREATED_AT,
						-1L));

		assertEquals("deletedAt must not be negative", exception.getMessage());
	}

	private FileUsageMetadata createFileUsageMetadata() {
		return FileUsageMetadata.at(
				ID,
				UUID_VALUE,
				PHYSICAL_FILE_ID,
				USAGE_TYPE,
				OWNER_TYPE,
				OWNER_ID,
				DISPLAY_NAME,
				METADATA_JSON,
				STATUS,
				CREATED_AT,
				DELETED_AT);
	}
}
