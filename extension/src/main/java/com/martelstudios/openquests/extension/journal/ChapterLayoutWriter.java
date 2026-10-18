package com.martelstudios.openquests.extension.journal;

import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.martelstudios.openquests.core.chapters.OpenQuestChapter;
import org.bson.BsonValue;
import org.bson.json.JsonMode;
import org.bson.json.JsonWriterSettings;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Writes a chapter edited in the journal back to the file it was loaded from, then loads it again
 * so every page reads the new layout. Takes the asset lock, so never call it on a world thread.
 */
final class ChapterLayoutWriter {

    private static final JsonWriterSettings PRETTY = JsonWriterSettings.builder()
                                                                       .indent(true)
                                                                       .outputMode(JsonMode.RELAXED)
                                                                       .build();

    private ChapterLayoutWriter() {}

    /**
     * Indented rather than written the way the asset editor does, on one line: a chapter is a file
     * people also edit by hand.
     *
     * @throws IOException for a chapter shipped inside an archive, or any file that cannot be written.
     */
    static void save(@Nonnull OpenQuestChapter chapter) throws IOException {
        var store = OpenQuestChapter.getAssetStore();
        var assetMap = store.getAssetMap();

        Path path = assetMap.getPath(chapter.getId());
        String pack = assetMap.getAssetPack(chapter.getId());
        if (path == null || pack == null || path.getFileSystem() != FileSystems.getDefault() || !Files.isWritable(path)) {
            throw new IOException("Chapter " + chapter.getId() + " is not a file that can be written: " + path);
        }

        BsonValue encoded = OpenQuestChapter.CODEC.encode(chapter, new AssetExtraInfo<>(path, new AssetExtraInfo.Data(OpenQuestChapter.class, chapter.getId(), null)));
        Files.writeString(path, encoded.asDocument().toJson(PRETTY) + System.lineSeparator());

        store.loadAssetsFromPaths(pack, List.of(path));
    }
}
