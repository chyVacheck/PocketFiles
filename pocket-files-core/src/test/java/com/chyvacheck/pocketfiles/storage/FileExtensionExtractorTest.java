package com.chyvacheck.pocketfiles.storage;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class FileExtensionExtractorTest {
    @Test
    void shouldExtractExtension() {
        // Act
        String extension = FileExtensionExtractor.extract("photo.png");

        // Assert
        assertEquals("png", extension);
    }

    @Test
    void shouldConvertExtensionToLowerCase() {
        // Act
        String extension = FileExtensionExtractor.extract("document.PDF");

        // Assert
        assertEquals("pdf", extension);
    }

    @Test
    void shouldExtractExtensionAfterLastDot() {
        // Act
        String extension = FileExtensionExtractor.extract("archive.tar.gz");

        // Assert
        assertEquals("gz", extension);
    }

    @Test
    void shouldReturnNullWhenFileHasNoExtension() {
        // Act
        String extension = FileExtensionExtractor.extract("file");

        // Assert
        assertNull(extension);
    }

    @Test
    void shouldReturnNullWhenFileNameIsHiddenFileWithoutExtension() {
        // Act
        String extension = FileExtensionExtractor.extract(".env");

        // Assert
        assertNull(extension);
    }

    @Test
    void shouldReturnNullWhenFileNameEndsWithDot() {
        // Act
        String extension = FileExtensionExtractor.extract("file.");

        // Assert
        assertNull(extension);
    }

    @Test
    void shouldReturnNullWhenOriginalNameIsNull() {
        // Act
        String extension = FileExtensionExtractor.extract(null);

        // Assert
        assertNull(extension);
    }

    @Test
    void shouldReturnNullWhenOriginalNameIsBlank() {
        // Act
        String extension = FileExtensionExtractor.extract("   ");

        // Assert
        assertNull(extension);
    }

    @Test
    void shouldTrimOriginalNameBeforeExtractingExtension() {
        // Act
        String extension = FileExtensionExtractor.extract(" photo.PNG ");

        // Assert
        assertEquals("png", extension);
    }
}