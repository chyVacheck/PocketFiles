package com.chyvacheck.pocketfiles.storage;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StoredFileNameGeneratorTest {

    private static final UUID PHYSICAL_FILE_UUID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

    @Test
    void shouldGenerateFileNameWithExtension() {
        // Act
        String fileName = StoredFileNameGenerator.generate(PHYSICAL_FILE_UUID, "png");

        // Assert
        assertEquals(
                PHYSICAL_FILE_UUID.toString() + ".png",
                fileName);
    }

    @Test
    void shouldGenerateFileNameWithoutExtensionWhenExtensionIsNull() {
        // Act
        String fileName = StoredFileNameGenerator.generate(PHYSICAL_FILE_UUID, null);

        // Assert
        assertEquals(
                PHYSICAL_FILE_UUID.toString(),
                fileName);
    }

    @Test
    void shouldGenerateFileNameWithoutExtensionWhenExtensionIsBlank() {
        // Act
        String fileName = StoredFileNameGenerator.generate(PHYSICAL_FILE_UUID, "   ");

        // Assert
        assertEquals(
                PHYSICAL_FILE_UUID.toString(),
                fileName);
    }

    @Test
    void shouldRemoveDotFromExtension() {
        // Act
        String fileName = StoredFileNameGenerator.generate(PHYSICAL_FILE_UUID, ".png");

        // Assert
        assertEquals(
                PHYSICAL_FILE_UUID.toString() + ".png",
                fileName);
    }

    @Test
    void shouldTrimExtension() {
        // Act
        String fileName = StoredFileNameGenerator.generate(PHYSICAL_FILE_UUID, " png ");

        // Assert
        assertEquals(
                PHYSICAL_FILE_UUID.toString() + ".png",
                fileName);
    }

    @Test
    void shouldConvertExtensionToLowerCase() {
        // Act
        String fileName = StoredFileNameGenerator.generate(PHYSICAL_FILE_UUID, "PNG");

        // Assert
        assertEquals(
                PHYSICAL_FILE_UUID.toString() + ".png",
                fileName);
    }
}