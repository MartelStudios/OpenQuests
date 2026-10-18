package com.martelstudios.openquests.core.chapters;

import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.AssetStore;
import com.hypixel.hytale.assetstore.codec.AssetBuilderCodec;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.assetstore.map.JsonAssetWithMap;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.OpenQuestAsset;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

/**
 * A set of quests read together, filed under a {@link OpenQuestChapterGroup}. Unlike a category,
 * which labels a quest, a chapter is a place in the journal and decides the order its quests are
 * read in. The chapter lists its quests, so quest assets know nothing of where they are shown.
 */
public class OpenQuestChapter implements JsonAssetWithMap<String, DefaultAssetMap<String, OpenQuestChapter>> {

    public static final AssetBuilderCodec<String, OpenQuestChapter> CODEC = AssetBuilderCodec.builder(OpenQuestChapter.class, OpenQuestChapter::new, Codec.STRING, (chapter, id) -> chapter.id = id, chapter -> chapter.id, (chapter, data) -> chapter.data = data, chapter -> chapter.data)
                                                                                             .append(new KeyedCodec<>("NameKey", Codec.STRING), (chapter, key) -> chapter.nameKey = key, chapter -> chapter.nameKey)
                                                                                             .addValidator(Validators.nonNull())
                                                                                             .add()
                                                                                             .append(new KeyedCodec<>("Group", Codec.STRING), (chapter, group) -> chapter.group = group, chapter -> chapter.group)
                                                                                             .addValidator(Validators.nonNull())
                                                                                             .addValidator(OpenQuestChapterGroup.VALIDATOR_CACHE.getValidator())
                                                                                             .add()
                                                                                             .append(new KeyedCodec<>("Order", Codec.INTEGER), (chapter, order) -> chapter.order = order, chapter -> chapter.order)
                                                                                             .add()
                                                                                             .append(new KeyedCodec<>("Icon", Codec.STRING), (chapter, icon) -> chapter.icon = icon, chapter -> chapter.icon)
                                                                                             .addValidator(OpenQuestAsset.ICON_VALIDATOR)
                                                                                             .add()
                                                                                             .append(new KeyedCodec<>("Quests", new ArrayCodec<>(ChapterQuest.CODEC, ChapterQuest[]::new)), (chapter, quests) -> chapter.quests = quests, chapter -> chapter.quests)
                                                                                             .add()
                                                                                             .build();

    protected String id;
    protected AssetExtraInfo.Data data;
    protected String nameKey;
    protected String group;
    protected int order;

    @Nullable
    protected String icon;

    @Nonnull
    protected ChapterQuest[] quests = new ChapterQuest[0];

    protected OpenQuestChapter() {}

    /**
     * @return the quests of this chapter as it shows them, in the order they are read.
     */
    @Nonnull
    public ChapterQuest[] getQuests() {
        return quests;
    }

    /**
     * @param runOf the player's run of a quest asset, or {@code null} for one they were never handed.
     * @return whether this chapter is shown to that player: once one of its quests is. A chapter has
     * no visibility of its own, so a quest kept out of sight cannot be glimpsed through it.
     */
    public boolean isVisible(@Nonnull Function<String, AbstractQuestProgression<?>> runOf) {
        for (ChapterQuest quest : quests) {
            if (isShown(quest.getQuest(), runOf.apply(quest.getQuest()))) return true;
        }
        return false;
    }

    /**
     * @param run the player's run of that quest, or {@code null} for one they were never handed.
     * @return whether a chapter lists that quest: the run's own visibility where there is one, and
     * otherwise its asset's, which only {@code Always} lets through, as a locked quest.
     */
    public static boolean isShown(@Nonnull String questAssetId, @Nullable AbstractQuestProgression<?> run) {
        if (run != null) return run.isVisible();

        OpenQuestAsset asset = OpenQuestAsset.getAsset(questAssetId);
        return asset != null && asset.getVisibility().isReachedBy(null);
    }

    /**
     * @return the icon drawn before the name, as a path under {@code Common/}, or {@code null}.
     */
    @Nullable
    public String getIcon() {
        return icon;
    }

    @Override
    public String getId() {
        return id;
    }

    /**
     * @return the translation key of the name shown to players.
     */
    @Nonnull
    public String getNameKey() {
        return nameKey;
    }

    /**
     * @return the id of the group this chapter is filed under.
     */
    @Nonnull
    public String getGroup() {
        return group;
    }

    /**
     * @return where the chapter stands within its group, lowest first.
     */
    public int getOrder() {
        return order;
    }

    /**
     * @return the store every pack's chapters are loaded into.
     */
    public static AssetStore<String, OpenQuestChapter, DefaultAssetMap<String, OpenQuestChapter>> getAssetStore() {
        return AssetRegistry.getAssetStore(OpenQuestChapter.class);
    }

    /**
     * @return the chapter under that id, or {@code null} for one no pack declares.
     */
    @Nullable
    public static OpenQuestChapter getChapter(@Nonnull String chapterId) {
        return getAssetStore().getAssetMap().getAsset(chapterId);
    }

    /**
     * @return the chapters filed under that group in reading order, ties broken by id.
     */
    @Nonnull
    public static List<OpenQuestChapter> getChaptersOf(@Nonnull String groupId) {
        return getAssetStore().getAssetMap()
                              .getAssetMap()
                              .values()
                              .stream()
                              .filter(chapter -> groupId.equals(chapter.getGroup()))
                              .sorted(Comparator.comparingInt(OpenQuestChapter::getOrder).thenComparing(OpenQuestChapter::getId))
                              .toList();
    }
}
