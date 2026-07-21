package com.chyvacheck.pocketfiles.storage;

import java.util.Locale;

public final class FileExtensionExtractor {
    private FileExtensionExtractor() {
        // Utility-класс не должен создаваться через new.
    }

    public static String extract(String originalName) {
        // Если имени нет или оно пустое, расширение определить нельзя.
        if (originalName == null || originalName.isBlank()) {
            return null;
        }

        // Убираем пробелы вокруг имени файла.
        String trimmedName = originalName.trim();

        // Ищем последнюю точку в имени файла.
        int lastDotIndex = trimmedName.lastIndexOf('.');

        // Если точки нет, значит расширения нет.
        if (lastDotIndex == -1) {
            return null;
        }

        // Если точка первая, например ".env", считаем это hidden-файлом без расширения.
        if (lastDotIndex == 0) {
            return null;
        }

        // Если точка последняя, например "file.", расширения тоже нет.
        if (lastDotIndex == trimmedName.length() - 1) {
            return null;
        }

        // Берем всё после последней точки.
        String extension = trimmedName.substring(lastDotIndex + 1);

        // Для технических значений используем Locale.ROOT.
        return extension.toLowerCase(Locale.ROOT);
    }
}