package com.martelstudios.openquests.mmoskilltree.editor;

import com.hypixel.hytale.codec.schema.metadata.ui.UIEditor;

/**
 * The asset editor's dropdowns of MMO Skill Tree values that are configuration rather than assets,
 * so no key validator offers them. {@link DataSetEvents} answers for each. A list for a field that
 * may be left open starts with {@code ALL}, the value such a field holds.
 */
public final class DataSets {
    public static final String SKILLS = "openquestsmmoskilltree:skills";

    public static final String SKILLS_OR_ALL = "openquestsmmoskilltree:skills_or_all";

    /**
     * A skill, a category or {@code ALL}, which is what a lamp or a boost picks from.
     */
    public static final String SKILL_POOLS = "openquestsmmoskilltree:skill_pools";

    /**
     * The ids abilities are cast under, which are not the file names of their assets.
     */
    public static final String ABILITIES_OR_ALL = "openquestsmmoskilltree:abilities_or_all";

    public static final String BOARDS_OR_ALL = "openquestsmmoskilltree:boards_or_all";

    public static final UIEditor SKILL_DROPDOWN = new UIEditor(new UIEditor.Dropdown(SKILLS));
    public static final UIEditor SKILL_OR_ALL_DROPDOWN = new UIEditor(new UIEditor.Dropdown(SKILLS_OR_ALL));
    public static final UIEditor SKILL_POOL_DROPDOWN = new UIEditor(new UIEditor.Dropdown(SKILL_POOLS));
    public static final UIEditor ABILITY_OR_ALL_DROPDOWN = new UIEditor(new UIEditor.Dropdown(ABILITIES_OR_ALL));
    public static final UIEditor BOARD_OR_ALL_DROPDOWN = new UIEditor(new UIEditor.Dropdown(BOARDS_OR_ALL));

    private DataSets() {}
}
