package com.martelstudios.openquests.core.commands;

import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.server.core.command.system.arguments.types.AssetArgumentType;
import com.martelstudios.openquests.core.models.OpenQuestAsset;

/**
 * Command argument types for OpenQuests values.
 */
public final class OpenQuestArgTypes {

    /**
     * Resolves an {@link OpenQuestAsset} by id and suggests every loaded one. Reads the asset store
     * on each request, so quests added by a reload are suggested without re-registering commands.
     * To suggest quest ids on an argument that parses something else, pass it to
     * {@code RequiredArg.withSuggestionOverride}.
     */
    public static final AssetArgumentType<OpenQuestAsset, DefaultAssetMap<String, OpenQuestAsset>> QUEST_ASSET = new AssetArgumentType<>(
        "openquests.commands.argtype.quest.name", OpenQuestAsset.class, "openquests.commands.argtype.quest.usage");

    private OpenQuestArgTypes() {
    }
}
