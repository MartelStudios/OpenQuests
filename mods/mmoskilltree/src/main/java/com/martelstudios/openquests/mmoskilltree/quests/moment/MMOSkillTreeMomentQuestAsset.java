package com.martelstudios.openquests.mmoskilltree.quests.moment;

import com.martelstudios.openquests.extension.quests.quantity.QuantityQuestAsset;

import javax.annotation.Nullable;

/**
 * Count a kind of moment of MMO Skill Tree: XP gained, an ability cast, a bounty collected. A subtype
 * names its fields for what they are ({@code Skill}, {@code Board}) and maps them here.
 */
public abstract class MMOSkillTreeMomentQuestAsset extends QuantityQuestAsset {

    /**
     * Written in a field left open, so the asset editor shows what it means rather than nothing.
     */
    public static final String ANY = "ALL";

    /**
     * @return the value, or {@code null} for one left open.
     */
    @Nullable
    public static String openOrNull(@Nullable String value) {
        return value == null || ANY.equalsIgnoreCase(value) ? null : value;
    }

    /**
     * @return what a moment must be about to count, {@code null} for any.
     */
    @Nullable
    public abstract String getTarget();

    /**
     * @return what a moment must be qualified by to count, {@code null} for any.
     */
    @Nullable
    public String getQualifier() {
        return null;
    }
}
