package com.martelstudios.openquests.core.teams;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;

/**
 * The team types the packs declare. A port, so the services read them without the asset store and
 * tests hand in their own.
 */
public interface TeamTypes {

    /**
     * @return the team type under that id, or {@code null} for one no pack declares.
     */
    @Nullable
    OpenQuestTeamType get(@Nonnull String id);

    /**
     * @return every declared team type, read anew at each call as packs reload.
     */
    @Nonnull
    Collection<OpenQuestTeamType> all();

    /**
     * @return team types read from that collection at each call, so later changes to it show.
     */
    @Nonnull
    static TeamTypes of(@Nonnull Collection<OpenQuestTeamType> types) {
        return new TeamTypes() {
            @Nullable
            @Override
            public OpenQuestTeamType get(@Nonnull String id) {
                for (OpenQuestTeamType type : types) {
                    if (type.getId().equals(id)) return type;
                }
                return null;
            }

            @Nonnull
            @Override
            public Collection<OpenQuestTeamType> all() {
                return types;
            }
        };
    }
}
