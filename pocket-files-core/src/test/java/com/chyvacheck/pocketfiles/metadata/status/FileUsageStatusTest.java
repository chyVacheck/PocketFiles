package com.chyvacheck.pocketfiles.metadata.status;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class FileUsageStatusTest {

	@Test
	void shouldReturnCode() {
		assertEquals(1, FileUsageStatus.ACTIVE.getCode());
		assertEquals(2, FileUsageStatus.DELETED.getCode());
	}

	@Test
	void shouldReturnStatusFromCode() {
		assertEquals(FileUsageStatus.ACTIVE, FileUsageStatus.fromCode(1));
		assertEquals(FileUsageStatus.DELETED, FileUsageStatus.fromCode(2));
	}

	@Test
	void shouldThrowExceptionWhenCodeIsUnknown() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> FileUsageStatus.fromCode(3));

		assertEquals("Unknown file usage status code: 3", exception.getMessage());
	}
}
