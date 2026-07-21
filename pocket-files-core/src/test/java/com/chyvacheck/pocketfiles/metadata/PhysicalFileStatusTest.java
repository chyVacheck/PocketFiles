package com.chyvacheck.pocketfiles.metadata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class PhysicalFileStatusTest {

	@Test
	void shouldReturnCode() {
		assertEquals(0, PhysicalFileStatus.PENDING.getCode());
		assertEquals(1, PhysicalFileStatus.ACTIVE.getCode());
		assertEquals(2, PhysicalFileStatus.ORPHANED.getCode());
		assertEquals(3, PhysicalFileStatus.DELETED.getCode());
		assertEquals(4, PhysicalFileStatus.MISSING.getCode());
		assertEquals(5, PhysicalFileStatus.FAILED.getCode());
	}

	@Test
	void shouldReturnStatusFromCode() {
		assertEquals(PhysicalFileStatus.PENDING, PhysicalFileStatus.fromCode(0));
		assertEquals(PhysicalFileStatus.ACTIVE, PhysicalFileStatus.fromCode(1));
		assertEquals(PhysicalFileStatus.ORPHANED, PhysicalFileStatus.fromCode(2));
		assertEquals(PhysicalFileStatus.DELETED, PhysicalFileStatus.fromCode(3));
		assertEquals(PhysicalFileStatus.MISSING, PhysicalFileStatus.fromCode(4));
		assertEquals(PhysicalFileStatus.FAILED, PhysicalFileStatus.fromCode(5));
	}

	@Test
	void shouldThrowExceptionWhenCodeIsUnknown() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> PhysicalFileStatus.fromCode(6));

		assertEquals("Unknown physical file status code: 6", exception.getMessage());
	}
}
