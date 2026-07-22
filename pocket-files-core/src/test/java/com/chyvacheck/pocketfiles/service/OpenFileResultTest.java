package com.chyvacheck.pocketfiles.service;

import com.chyvacheck.pocketfiles.metadata.model.FileUsageMetadata;
import com.chyvacheck.pocketfiles.metadata.model.PhysicalFileMetadata;
import com.chyvacheck.pocketfiles.metadata.status.FileUsageStatus;
import com.chyvacheck.pocketfiles.metadata.status.PhysicalFileStatus;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OpenFileResultTest {
	private static final UUID PHYSICAL_FILE_UUID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

	private static final UUID FILE_USAGE_UUID = UUID.fromString("11111111-1111-1111-1111-111111111111");

	private static final long CREATED_AT = 1760000000000L;

	private static final Path ABSOLUTE_PATH = Path
			.of("/tmp/pocket-files/files/2026/01/02/550e8400-e29b-41d4-a716-446655440000.png");

	@Test
	void shouldCreateOpenFileResult() {
		FileUsageMetadata fileUsageMetadata = this.createFileUsageMetadata();
		PhysicalFileMetadata physicalFileMetadata = this.createPhysicalFileMetadata();

		OpenFileResult result = OpenFileResult.of(
				fileUsageMetadata,
				physicalFileMetadata,
				ABSOLUTE_PATH);

		assertSame(fileUsageMetadata, result.fileUsageMetadata());
		assertSame(physicalFileMetadata, result.physicalFileMetadata());
		assertEquals(ABSOLUTE_PATH, result.absolutePath());
	}

	@Test
	void shouldThrowExceptionWhenFileUsageMetadataIsNull() {
		NullPointerException exception = assertThrows(
				NullPointerException.class,
				() -> OpenFileResult.of(
						null,
						this.createPhysicalFileMetadata(),
						ABSOLUTE_PATH));

		assertEquals("fileUsageMetadata must not be null", exception.getMessage());
	}

	@Test
	void shouldThrowExceptionWhenPhysicalFileMetadataIsNull() {
		NullPointerException exception = assertThrows(
				NullPointerException.class,
				() -> OpenFileResult.of(
						this.createFileUsageMetadata(),
						null,
						ABSOLUTE_PATH));

		assertEquals("physicalFileMetadata must not be null", exception.getMessage());
	}

	@Test
	void shouldThrowExceptionWhenAbsolutePathIsNull() {
		NullPointerException exception = assertThrows(
				NullPointerException.class,
				() -> OpenFileResult.of(
						this.createFileUsageMetadata(),
						this.createPhysicalFileMetadata(),
						null));

		assertEquals("absolutePath must not be null", exception.getMessage());
	}

	private FileUsageMetadata createFileUsageMetadata() {
		return FileUsageMetadata.at(
				1L,
				FILE_USAGE_UUID,
				1L,
				"invoice_attachment",
				"invoice",
				"777",
				"Invoice January.pdf",
				"""
						{"source":"upload-form","category":"invoice"}
						""",
				FileUsageStatus.ACTIVE,
				CREATED_AT,
				null);
	}

	private PhysicalFileMetadata createPhysicalFileMetadata() {
		return PhysicalFileMetadata.at(
				1L,
				PHYSICAL_FILE_UUID,
				"photo.png",
				"2026/01/02/550e8400-e29b-41d4-a716-446655440000.png",
				"image/png",
				"png",
				5L,
				"185f8db32271fe25f561a6fc938b2e264306ec304eda518007d1764826381969",
				PhysicalFileStatus.ACTIVE,
				CREATED_AT,
				CREATED_AT,
				null);
	}
}
