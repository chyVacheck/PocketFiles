package com.chyvacheck.pocketfiles.storage;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public final class StoredFileNameGenerator {

    private StoredFileNameGenerator() {
        // Utility-класс не должен создаваться через new.
    }

    public static String generate(UUID physicalFileUuid, String extension) {
        // UUID физического файла обязателен, потому что именно он становится основой
        // имени файла.
        Objects.requireNonNull(physicalFileUuid, "physicalFileUuid must not be null");

        // Если расширения нет, сохраняем файл просто под UUID без точки в конце.
        if (extension == null || extension.isBlank()) {
            return physicalFileUuid.toString();
        }

        // Приводим расширение к единому формату: без точки, без пробелов, в нижнем
        // регистре.
        String normalizedExtension = normalizeExtension(extension);

        // Собираем итоговое имя файла: uuid.extension.
        return physicalFileUuid + "." + normalizedExtension;
    }

    private static String normalizeExtension(String extension) {
        // Убираем пробелы в начале и конце.
        String trimmed = extension.trim();

        // Позволяем передавать расширение как "png" и как ".png".
        if (trimmed.startsWith(".")) {
            trimmed = trimmed.substring(1);
        }

        // Используем Locale.ROOT, чтобы результат не зависел от языка системы.
        return trimmed.toLowerCase(Locale.ROOT);
    }
}