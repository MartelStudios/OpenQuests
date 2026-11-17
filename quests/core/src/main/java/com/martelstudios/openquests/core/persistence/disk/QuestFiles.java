package com.martelstudios.openquests.core.persistence.disk;

import com.hypixel.hytale.codec.ExtraInfo;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.util.RawJsonReader;
import com.hypixel.hytale.common.util.PathUtil;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.util.BsonUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/** Hytale's JSON and backup handling at an explicit path, independent of the universe singleton. */
final class QuestFiles<T> {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private final Path path;
    private final BuilderCodec<T> codec;

    QuestFiles(Path path, BuilderCodec<T> codec) {
        this.path = path;
        this.codec = codec;
    }

    Path getPath() { return path; }

    T load(String id) throws IOException {
        Path file = file(id);
        return Files.exists(file) ? RawJsonReader.readSyncWithBak(file, codec, LOGGER) : null;
    }

    void save(String id, T value) {
        var info = ExtraInfo.THREAD_LOCAL.get();
        var document = codec.encode(value, info);
        info.getValidationResults().logOrThrowValidatorExceptions(LOGGER);
        BsonUtil.writeDocument(file(id), document).join();
    }

    void remove(String id) throws IOException {
        Files.deleteIfExists(file(id));
        Files.deleteIfExists(path.resolve(id + ".json.bak"));
    }

    Map<String, T> loadAll() throws IOException {
        Map<String, T> values = new HashMap<>();
        try (var files = Files.newDirectoryStream(path, "*.json")) {
            for (Path file : files) {
                String name = file.getFileName().toString();
                values.put(name.substring(0, name.length() - 5), RawJsonReader.readSyncWithBak(file, codec, LOGGER));
            }
        }
        return values;
    }

    void migrate() throws IOException {
        Files.createDirectories(path);
        try (var files = Files.newDirectoryStream(path, "*.bson")) {
            for (Path legacy : files) {
                String name = legacy.getFileName().toString();
                Path current = path.resolve(name.substring(0, name.length() - 5) + ".json");
                if (!Files.exists(current)) Files.move(legacy, current);
            }
        }
    }

    private Path file(String id) {
        if (!PathUtil.isValidName(id)) throw new IllegalArgumentException("Invalid quest record id " + id);
        return path.resolve(id + ".json");
    }
}
