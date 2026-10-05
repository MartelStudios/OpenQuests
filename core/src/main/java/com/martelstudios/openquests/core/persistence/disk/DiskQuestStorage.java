package com.martelstudios.openquests.core.persistence.disk;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.datastore.DiskDataStore;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.AssignmentRecord;
import com.martelstudios.openquests.core.models.AssignmentRecords;
import com.martelstudios.openquests.core.models.QuestState;
import com.martelstudios.openquests.core.persistence.PlayerMessage;
import com.martelstudios.openquests.core.persistence.PlayerQuestRecord;
import com.martelstudios.openquests.core.persistence.QuestProgressionRecord;
import com.martelstudios.openquests.core.persistence.QuestStorage;
import com.martelstudios.openquests.core.persistence.QuestStorageException;
import com.martelstudios.openquests.core.persistence.ReplicaPoll;
import com.martelstudios.openquests.core.replication.Replica;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * The quests as JSON files under the universe directory, which is where a server with no database
 * keeps them: one file per progression, per player, per index, per shared holder, per message.
 *
 * <p>Written by one server only, so a progression's file is that server's one replica, and every
 * claim goes through.
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

    /**
     * One lock per index and per shared holder, so reading a file and writing it back is one step.
     */
    private final Map<String, Object> keyLocks = new ConcurrentHashMap<>();

    private DiskDataStore<QuestProgressionRecord> progressions;
    private DiskDataStore<PlayerQuestRecord> players;
    private DiskDataStore<QuestIndexRecord> indexes;
    private DiskDataStore<AssignmentRecordsFile> assignments;
    private DiskDataStore<PlayerMessage> messages;

    public DiskQuestStorage(@Nonnull String rootPath) {
        this.rootPath = rootPath;
    }

    @Override
    public void start() {
        progressions = new DiskDataStore<>(child("progressions"), QuestProgressionRecord.CODEC);
        players = new DiskDataStore<>(child("players"), PlayerQuestRecord.CODEC);
        indexes = new DiskDataStore<>(child("indexes"), QuestIndexRecord.CODEC);
        assignments = new DiskDataStore<>(child("assignments"), AssignmentRecordsFile.CODEC);
        messages = new DiskDataStore<>(child("messages"), PlayerMessage.CODEC);

        // list() and loadAll() walk a directory stream, which throws on one that is not there yet
        createDirectory(progressions.getPath());
        createDirectory(players.getPath());
        createDirectory(indexes.getPath());
        createDirectory(assignments.getPath());
        createDirectory(messages.getPath());

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

    @Nonnull
    @Override
    public String getReplicaId() {
        return Replica.DEFAULT_ID;
    }

    @Override
    public boolean isShared() {
        return false;
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

    /**
     * Through the player's file, which names their quests; the quest has the last word on whether
     * they still hold it.
     */
    @Nonnull
    @Override
    public List<AbstractQuestProgression<?>> loadPlayerProgressions(@Nonnull UUID playerId) {
        List<AbstractQuestProgression<?>> held = new ArrayList<>();

        for (AbstractQuestProgression<?> quest : loadProgressions(loadPlayer(playerId).getQuestIds())) {
            if (quest.getPlayers().contains(playerId) || quest.getAbandonedPlayers().contains(playerId)) held.add(quest);
        }
        return held;
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
     * Nobody else writes these files, so the outcome this server reached is the outcome: it is
     * written with the quest by the next save.
     */
    @Nullable
    @Override
    public QuestState claimEnd(@Nonnull AbstractQuestProgression<?> quest) {
        return null;
    }

    @Nonnull
    @Override
    public ReplicaPoll pollReplicas(@Nonnull Map<UUID, Map<String, Long>> known) {
        return ReplicaPoll.NONE;
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
            return new HashSet<>();
        }

        return record == null ? new HashSet<>() : new HashSet<>(record.getQuestIds());
    }

    @Nonnull
    @Override
    public Map<String, Set<UUID>> loadIndexes(@Nonnull Collection<String> indexKeys) {
        Map<String, Set<UUID>> loaded = new HashMap<>();
        for (String indexKey : indexKeys) loaded.put(indexKey, loadIndex(indexKey));
        return loaded;
    }

    @Override
    public void addToIndex(@Nonnull String indexKey, @Nonnull Collection<UUID> questIds) {
        if (questIds.isEmpty()) return;
        updateIndex(indexKey, ids -> ids.addAll(questIds));
    }

    @Override
    public void removeFromIndex(@Nonnull String indexKey, @Nonnull Collection<UUID> questIds) {
        if (questIds.isEmpty()) return;
        updateIndex(indexKey, ids -> ids.removeAll(questIds));
    }

    /**
     * Removes the file rather than emptying it, so closed instances leave nothing behind.
     */
    @Override
    public void deleteIndex(@Nonnull String indexKey) {
        synchronized (lockOf(indexKey)) {
            try {
                indexes.remove(fileName(indexKey));
            } catch (NoSuchFileException e) {
                // Never written: a world nobody played a quest in
            } catch (IOException e) {
                LOGGER.atWarning().withCause(e).log("Failed to delete the %s quest index", indexKey);
            }
        }
    }

    /**
     * An index left empty loses its file, as a deleted one does.
     */
    private void updateIndex(@Nonnull String indexKey, @Nonnull Consumer<Set<UUID>> change) {
        synchronized (lockOf(indexKey)) {
            Set<UUID> ids = loadIndex(indexKey);
            change.accept(ids);

            if (ids.isEmpty()) {
                deleteIndex(indexKey);
            } else {
                indexes.save(fileName(indexKey), new QuestIndexRecord(ids));
            }
        }
    }

    @Nonnull
    @Override
    public AssignmentRecords loadAssignments(@Nonnull String holderKey) {
        return new AssignmentRecords(readAssignments(holderKey));
    }

    @Override
    public boolean claimAssignment(@Nonnull String holderKey, @Nonnull String assignmentId, @Nonnull String questAssetId, @Nullable AssignmentRecord expected, @Nonnull AssignmentRecord next) {
        synchronized (lockOf("assignments/" + holderKey)) {
            AssignmentRecords records = new AssignmentRecords(readAssignments(holderKey));
            if (!Objects.equals(records.get(assignmentId, questAssetId), expected)) return false;

            records.put(assignmentId, questAssetId, next);
            assignments.save(fileName(holderKey), new AssignmentRecordsFile(records.snapshot()));
            return true;
        }
    }

    /**
     * Removes the file rather than emptying it, so closed worlds leave nothing behind.
     */
    @Override
    public void deleteAssignments(@Nonnull String holderKey) {
        synchronized (lockOf("assignments/" + holderKey)) {
            try {
                assignments.remove(fileName(holderKey));
            } catch (NoSuchFileException e) {
                // Never written: a holder nothing was handed to
            } catch (IOException e) {
                LOGGER.atWarning().withCause(e).log("Failed to delete the %s assignment records", holderKey);
            }
        }
    }

    @Nonnull
    private Map<String, Map<String, AssignmentRecord>> readAssignments(@Nonnull String holderKey) {
        try {
            AssignmentRecordsFile file = assignments.load(fileName(holderKey));
            return file == null ? Map.of() : file.getRecords();
        } catch (IOException e) {
            LOGGER.atWarning().withCause(e).log("Failed to read the %s assignment records", holderKey);
            return Map.of();
        }
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

    /**
     * The record first, then the messages it took in: a server stopping in between would take
     * those in again next time, which a debt replacing its own copy and a disk written by one
     * server make rare enough.
     */
    @Override
    public void savePlayer(@Nonnull UUID playerId, @Nonnull PlayerQuestRecord record, @Nonnull Collection<UUID> delivered) {
        synchronized (playerLocks.computeIfAbsent(playerId, id -> new Object())) {
            players.save(playerId.toString(), record);
        }

        for (UUID messageId : delivered) {
            try {
                messages.remove(messageFile(playerId, messageId));
            } catch (NoSuchFileException e) {
                // Let go of already
            } catch (IOException e) {
                LOGGER.atWarning().withCause(e).log("Failed to let go of message %s of player %s", messageId, playerId);
            }
        }
    }

    @Override
    public void postMessage(@Nonnull PlayerMessage message) {
        messages.save(messageFile(message.getPlayerId(), message.getId()), message);
    }

    @Nonnull
    @Override
    public List<PlayerMessage> loadMessages(@Nonnull Collection<UUID> playerIds) {
        if (playerIds.isEmpty()) return List.of();

        Map<String, PlayerMessage> all;
        try {
            all = messages.loadAll();
        } catch (IOException e) {
            LOGGER.atWarning().withCause(e).log("Failed to read the player messages");
            return List.of();
        }

        List<PlayerMessage> waiting = new ArrayList<>();
        for (PlayerMessage message : all.values()) {
            if (message != null && playerIds.contains(message.getPlayerId())) waiting.add(message);
        }
        waiting.sort(Comparator.comparingLong(PlayerMessage::getAt));
        return waiting;
    }

    @Nonnull
    private Object lockOf(@Nonnull String key) {
        return keyLocks.computeIfAbsent(key, ignored -> new Object());
    }

    @Nonnull
    private static String messageFile(@Nonnull UUID playerId, @Nonnull UUID messageId) {
        return playerId + "_" + messageId;
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
