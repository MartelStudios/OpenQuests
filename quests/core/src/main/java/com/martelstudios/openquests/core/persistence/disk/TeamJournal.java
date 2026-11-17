package com.martelstudios.openquests.core.persistence.disk;

import com.martelstudios.openquests.core.persistence.CodecJson;
import com.martelstudios.openquests.core.persistence.QuestStorageException;
import com.martelstudios.openquests.core.persistence.TeamTransaction;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.channels.FileChannel;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

/** Replays a committed team move before another team operation can observe a partial file write. */
public final class TeamJournal {
    private final Path folder;
    private final Consumer<TeamTransaction> apply;

    public TeamJournal(Path folder, Consumer<TeamTransaction> apply) {
        this.folder = folder;
        this.apply = apply;
    }

    /** Completes a previous move first; a failed replay leaves its intent on disk. */
    public void recover() {
        try {
            Files.createDirectories(folder);
            try (var files = Files.list(folder)) {
                for (Path file : files.filter(path -> path.getFileName().toString().endsWith(".json")).sorted().toList()) {
                    TeamTransaction transaction = CodecJson.decode(TeamTransaction.CODEC, Files.readString(file), "team transaction " + file);
                    if (transaction == null) throw new QuestStorageException("Unreadable team transaction " + file);
                    apply.accept(transaction);
                    Files.delete(file);
                }
            }
        } catch (IOException e) {
            throw new QuestStorageException("Could not recover team transactions", e);
        }
    }

    /** Makes the intent durable before any team or notification is changed. */
    public void commit(TeamTransaction transaction) {
        if (transaction.isEmpty()) return;
        recover();
        try {
            Path revisionFile = folder.resolve("revision");
            long revision = Files.exists(revisionFile) ? Long.parseLong(Files.readString(revisionFile)) + 1 : 1;
            write(revisionFile, Long.toString(revision));
            if (transaction.message() != null) transaction.message().getTeam().setRevision(revision);
            Path intent = folder.resolve(transaction.id() + ".json");
            write(intent, CodecJson.encode(TeamTransaction.CODEC, transaction));
            recover();
        } catch (IOException e) {
            throw new QuestStorageException("Could not commit team transaction", e);
        }
    }

    private static void write(Path target, String value) throws IOException {
        Path temporary = target.resolveSibling(target.getFileName() + ".tmp");
        try (FileChannel file = FileChannel.open(temporary, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
            ByteBuffer bytes = ByteBuffer.wrap(value.getBytes(StandardCharsets.UTF_8));
            while (bytes.hasRemaining()) file.write(bytes);
            file.force(true);
        }
        // Refuse a filesystem without atomic replacement rather than risk a truncated intent.
        Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    }
}
