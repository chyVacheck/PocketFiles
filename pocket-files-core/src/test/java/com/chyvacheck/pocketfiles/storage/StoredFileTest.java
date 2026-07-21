package com.chyvacheck.pocketfiles.storage;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StoredFileTest {
    private static final UUID UUID_VALUE = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

    private static final String ORIGINAL_NAME = "photo.png";

    private static final String RELATIVE_PATH = "2026/01/02/photo.png";

    private static final String EXTENSION = "png";

    private static final long SIZE_BYTES = 5L;

    private static final String SHA256 = "185f8db32271fe25f561a6fc938b2e264306ec304eda518007d1764826381969";

    private static final Path ABSOLUTE_PATH = Path.of("/tmp/pocket-files/files/2026/01/02/photo.png");

    @Test
    void shouldCreateStoredFile() {
        // Act
        StoredFile storedFile = createStoredFile();

        // Assert
        assertEquals(UUID_VALUE, storedFile.uuid());
        assertEquals(ORIGINAL_NAME, storedFile.originalName());
        assertEquals(RELATIVE_PATH, storedFile.relativePath());
        assertEquals(EXTENSION, storedFile.extension());
        assertEquals(SIZE_BYTES, storedFile.sizeBytes());
        assertEquals(SHA256, storedFile.sha256());
        assertEquals(ABSOLUTE_PATH, storedFile.absolutePath());
    }

    @Test
    void shouldCreateStoredFileWithoutExtension() {
        // Act
        StoredFile storedFile = StoredFile.at(
                UUID_VALUE,
                "README",
                "2026/01/02/README",
                null,
                SIZE_BYTES,
                SHA256,
                ABSOLUTE_PATH);

        // Assert
        assertNull(storedFile.extension());
    }

    @Test
    void shouldTrimStringValues() {
        // Act
        StoredFile storedFile = StoredFile.at(
                UUID_VALUE,
                " photo.png ",
                " 2026/01/02/photo.png ",
                " png ",
                SIZE_BYTES,
                " " + SHA256 + " ",
                ABSOLUTE_PATH);

        // Assert
        assertEquals("photo.png", storedFile.originalName());
        assertEquals("2026/01/02/photo.png", storedFile.relativePath());
        assertEquals("png", storedFile.extension());
        assertEquals(SHA256, storedFile.sha256());
    }

    @Test
    void shouldThrowExceptionWhenUuidIsNull() {
        // Act + Assert
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> StoredFile.at(
                        null,
                        ORIGINAL_NAME,
                        RELATIVE_PATH,
                        EXTENSION,
                        SIZE_BYTES,
                        SHA256,
                        ABSOLUTE_PATH));

        assertEquals("uuid must not be null", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenOriginalNameIsNull() {
        // Act + Assert
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> StoredFile.at(
                        UUID_VALUE,
                        null,
                        RELATIVE_PATH,
                        EXTENSION,
                        SIZE_BYTES,
                        SHA256,
                        ABSOLUTE_PATH));

        assertEquals("originalName must not be null", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenOriginalNameIsBlank() {
        // Act + Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> StoredFile.at(
                        UUID_VALUE,
                        "   ",
                        RELATIVE_PATH,
                        EXTENSION,
                        SIZE_BYTES,
                        SHA256,
                        ABSOLUTE_PATH));

        assertEquals("originalName must not be blank", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenRelativePathIsNull() {
        // Act + Assert
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> StoredFile.at(
                        UUID_VALUE,
                        ORIGINAL_NAME,
                        null,
                        EXTENSION,
                        SIZE_BYTES,
                        SHA256,
                        ABSOLUTE_PATH));

        assertEquals("relativePath must not be null", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenRelativePathIsBlank() {
        // Act + Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> StoredFile.at(
                        UUID_VALUE,
                        ORIGINAL_NAME,
                        "   ",
                        EXTENSION,
                        SIZE_BYTES,
                        SHA256,
                        ABSOLUTE_PATH));

        assertEquals("relativePath must not be blank", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenExtensionIsBlank() {
        // Act + Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> StoredFile.at(
                        UUID_VALUE,
                        ORIGINAL_NAME,
                        RELATIVE_PATH,
                        "   ",
                        SIZE_BYTES,
                        SHA256,
                        ABSOLUTE_PATH));

        assertEquals("extension must not be blank", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenSizeBytesIsNegative() {
        // Act + Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> StoredFile.at(
                        UUID_VALUE,
                        ORIGINAL_NAME,
                        RELATIVE_PATH,
                        EXTENSION,
                        -1L,
                        SHA256,
                        ABSOLUTE_PATH));

        assertEquals("sizeBytes must not be negative", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenSha256IsNull() {
        // Act + Assert
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> StoredFile.at(
                        UUID_VALUE,
                        ORIGINAL_NAME,
                        RELATIVE_PATH,
                        EXTENSION,
                        SIZE_BYTES,
                        null,
                        ABSOLUTE_PATH));

        assertEquals("sha256 must not be null", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenSha256IsBlank() {
        // Act + Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> StoredFile.at(
                        UUID_VALUE,
                        ORIGINAL_NAME,
                        RELATIVE_PATH,
                        EXTENSION,
                        SIZE_BYTES,
                        "   ",
                        ABSOLUTE_PATH));

        assertEquals("sha256 must not be blank", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenAbsolutePathIsNull() {
        // Act + Assert
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> StoredFile.at(
                        UUID_VALUE,
                        ORIGINAL_NAME,
                        RELATIVE_PATH,
                        EXTENSION,
                        SIZE_BYTES,
                        SHA256,
                        null));

        assertEquals("absolutePath must not be null", exception.getMessage());
    }

    private StoredFile createStoredFile() {
        return StoredFile.at(
                UUID_VALUE,
                ORIGINAL_NAME,
                RELATIVE_PATH,
                EXTENSION,
                SIZE_BYTES,
                SHA256,
                ABSOLUTE_PATH);
    }
}