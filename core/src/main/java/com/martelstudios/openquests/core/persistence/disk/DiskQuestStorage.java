package com.martelstudios.openquests.core.persistence.disk;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.datastore.DiskDataStore;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.QuestState;
import com.martelstudios.openquests.core.persistence.PlayerQuestRecord;
import com.martelstudios.openquests.core.persistence.QuestProgressionRecord;
import com.martelstudios.openquests.core.persistence.QuestStorage;
import com.martelstudios.openquests.core.persistence.QuestStorageException;
import com.martelstudios.openquests.core.rewards.models.PendingRewards;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * The quests as JSON files under the universe directory, which is where a server with no database
 * keeps them. Three directories: one file per progression, one per player, one per scope index.
 *
 * <p>A progression gets a file of its own whoever holds it.
 */
public class DiskQuestStorage implements QuestStorage {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static final String ID = "Disk";

    private final String rootPath;

    /**
     * One lock per player file, kept for the life of the server: a lock map that forgets entries
     * would hand two writers different locks for the same file.
     */
    private final Map<UUID, Object> playerLocks = new ConcurrentHashMap<>();

    private DiskDataStore<QuestProgressionRecord> progressions;
    private DiskDataStore<PlayerQuestRecord> players;
    private DiskDataStore<QuestIndexRecord> indexes;

    public DiskQuestStorage(@Nonnull String rootPath) {
        this.rootPath = rootPath;
    }

    @Override
    public void start() {
        progressions = new DiskDataStore<>(child("progressions"), QuestProgressionRecord.CODEC);
        players = new DiskDataStore<>(child("players"), PlayerQuestRecord.CODEC);
        indexes = new DiskDataStore<>(child("indexes"), QuestIndexRecord.CODEC);

        // list() and loadAll() walk a directory stream, which throws on one that is not there yet
        createDirectory(progressions.getPath());
        createDirectory(players.getPath());
        createDirectory(indexes.getPath());

        LOGGER.atInfo().log("Quest storage: JSON files under %s", rootPath);
    }

    @Override
    public void close() {
        // Every write already reached the disk before it returned
    }

    @Nonnull
    @Override
    public String getId() {
        return ID;
    }

    @Nullable
    @Override
    public AbstractQuestProgression<?> loadProgression(@Nonnull UUID questId) {
        QuestProgressionRecord record;
        try {
            record = progressions.load(questId.toString());
        } catch (IOException e) {
            LOGGER.atWarning().withCause(e).log("Failed to read quest %s", questId);
            return null;
        }

        return record == null ? null : record.quest;
    }

    @Nonnull
    @Override
    public List<AbstractQuestProgression<?>> loadProgressions(@Nonnull Collection<UUID> questIds) {
        List<AbstractQuestProgression<?>> quests = new ArrayList<>(questIds.size());

        for (UUID questId : questIds) {
            AbstractQuestProgression<?> quest = loadProgression(questId);
            if (quest != null) quests.add(quest);
        }
        return quests;
    }

    @Nonnull
    @Override
    public List<AbstractQuestProgression<?>> loadAllProgressions() {
        Map<String, QuestProgressionRecord> records;
        try {
            records = progressions.loadAll();
        } catch (IOException e) {
            LOGGER.atWarning().withCause(e).log("Failed to read the quest directory");
            return List.of();
        }

        List<AbstractQuestProgression<?>> quests = new ArrayList<>(records.size());
        for (QuestProgressionRecord record : records.values()) {
            if (record != null && record.quest != null) quests.add(record.quest);
        }
        return quests;
    }

    @Override
    public void saveProgressions(@Nonnull Collection<AbstractQuestProgression<?>> quests) {
        Map<UUID, Set<UUID>> newLinks = new HashMap<>();

        for (AbstractQuestProgression<?> quest : quests) {
            progressions.save(quest.getId().toString(), new QuestProgressionRecord(quest));

            collectOfflineHolders(quest, newLinks);
        }

        newLinks.forEach((playerId, questIds) -> updatePlayer(playerId, record -> record.getQuestIds().addAll(questIds)));
    }

    @Override
    public void deleteProgression(@Nonnull AbstractQuestProgression<?> quest) {
        UUID questId = quest.getId();

        try {
            progressions.remove(questId.toString());
        } catch (IOException e) {
            LOGGER.atWarning().withCause(e).log("Failed to delete quest %s", questId);
        }

        for (UUID playerId : holders(quest)) {
            updatePlayer(playerId, record -> record.getQuestIds().remove(questId));
        }
    }

    /**
     * An online player's index is written whole when their session ends. An offline one has nobody
     * holding theirs, so a quest reaching them would be written with no way back to it.
     */
    private static void collectOfflineHolders(@Nonnull AbstractQuestProgression<?> quest, @Nonnull Map<UUID, Set<UUID>> into) {
        Universe universe = Universe.get();

        for (UUID playerId : holders(quest)) {
            // No universe means nobody is online, which errs towards writing the link down
            if (universe != null && universe.getPlayer(playerId) != null) continue;

            into.computeIfAbsent(playerId, id -> new HashSet<>()).add(quest.getId());
        }
    }

    @Nonnull
    private static Set<UUID> holders(@Nonnull AbstractQuestProgression<?> quest) {
        Set<UUID> holders = new HashSet<>(quest.getPlayers());
        holders.addAll(quest.getAbandonedPlayers());

        return holders;
    }

    /**
     * Reads a player's file, changes it and writes it back, one writer at a time. A file that is
     * there but cannot be read is left alone: an empty record over it would cost a catalogue and
     * a debt to add one id.
     */
    private void updatePlayer(@Nonnull UUID playerId, @Nonnull Consumer<PlayerQuestRecord> change) {
        synchronized (playerLocks.computeIfAbsent(playerId, id -> new Object())) {
            PlayerQuestRecord record;
            try {
                record = players.load(playerId.toString());
            } catch (IOException e) {
                LOGGER.atWarning().withCause(e).log("Failed to read the quests of player %s, leaving them alone", playerId);
                return;
            }

            if (record == null) {
                if (Files.exists(players.getPath().resolve(playerId + ".json"))) {
                    LOGGER.atWarning().log("The quest file of player %s could not be read, leaving it alone", playerId);
                    return;
                }

                record = new PlayerQuestRecord();
            }

            change.accept(record);

            players.save(playerId.toString(), record);
        }
    }

    @Nonnull
    @Override
    public Set<UUID> loadIndex(@Nonnull String indexKey) {
        QuestIndexRecord record;
        try {
            record = indexes.load(fileName(indexKey));
        } catch (IOException e) {
            LOGGER.atWarning().withCause(e).log("Failed to read the %s quest index", indexKey);
            return Set.of();
        }

        return record == null ? Set.of() : record.getQuestIds();
    }

    @Override
    public void saveIndex(@Nonnull String indexKey, @Nonnull Set<UUID> questIds) {
        indexes.save(fileName(indexKey), new QuestIndexRecord(questIds));
    }

    @Nonnull
    @Override
    public PlayerQuestRecord loadPlayer(@Nonnull UUID playerId) {
        PlayerQuestRecord record;
        try {
            record = players.load(playerId.toString());
        } catch (IOException e) {
            LOGGER.atWarning().withCause(e).log("Failed to read the quests of player %s", playerId);
            return new PlayerQuestRecord();
        }

        return record == null ? new PlayerQuestRecord() : record;
    }

    @Override
    public void savePlayer(@Nonnull UUID playerId, @Nonnull PlayerQuestRecord record) {
        synchronized (playerLocks.computeIfAbsent(playerId, id -> new Object())) {
            players.save(playerId.toString(), record);
        }
    }

    @Override
    public void addPendingRewards(@Nonnull UUID playerId, @Nonnull PendingRewards owed) {
        updatePlayer(playerId, record -> {
            record.getPendingRewards().remove(owed);
            record.getPendingRewards().add(owed);
        });
    }

    @Override
    public void recordCompletion(@Nonnull UUID playerId, @Nonnull String assetId, @Nonnull QuestState outcome, @Nullable Instant startedAt, @Nullable Instant completedAt) {
        updatePlayer(playerId, record -> record.recordCompletion(assetId, outcome, startedAt, completedAt));
    }

    @Nonnull
    private String child(@Nonnull String name) {
        return Paths.get(rootPath, name).toString();
    }

    /**
     * A scope key reads {@code world:<uuid>}, which no Windows file system takes. Anything a file
     * name cannot carry becomes an underscore.
     */
    @Nonnull
    private static String fileName(@Nonnull String indexKey) {
        StringBuilder name = new StringBuilder(indexKey.length());

        for (int i = 0; i < indexKey.length(); i++) {
            char c = indexKey.charAt(i);
            name.append(Character.isLetterOrDigit(c) || c == '-' || c == '_' ? c : '_');
        }
        return name.toString();
    }

    private static void createDirectory(@Nonnull Path path) {
        try {
            Files.createDirectories(path);
        } catch (IOException e) {
            throw new QuestStorageException("Failed to create the quest directory " + path, e);
        }
    }
}
