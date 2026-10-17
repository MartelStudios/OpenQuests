package com.martelstudios.openquests.mmoskilltree.editor;

import com.hypixel.hytale.codec.schema.metadata.ui.UIEditor;

/**
 * The asset editor's dropdowns of MMO Skill Tree values that are configuration rather than assets,
 * so no key validator offers them. {@link DataSetEvents} answers for the lists this plugin owns.
 */
public final class DataSets {
    public static final String SKILLS = "openquestsmmoskilltree:skills";

    /**
     * A skill, a category or {@code ALL}, which is what a lamp or a boost picks from.
     */
    public static final String SKILL_POOLS = "openquestsmmoskilltree:skill_pools";

    /**
     * The ids abilities are cast under, which are not the file names of their assets.
     */
    public static final String ABILITIES = "openquestsmmoskilltree:abilities";

    public static final UIEditor SKILL_DROPDOWN = new UIEditor(new UIEditor.Dropdown(SKILLS));
    public static final UIEditor SKILL_POOL_DROPDOWN = new UIEditor(new UIEditor.Dropdown(SKILL_POOLS));
    public static final UIEditor ABILITY_DROPDOWN = new UIEditor(new UIEditor.Dropdown(ABILITIES));

    /**
     * CommonLib's own list of bounty boards.
     */
    public static final UIEditor BOARD_DROPDOWN = new UIEditor(new UIEditor.Dropdown("ziggfreedcommon:boards"));

    private DataSets() {}
}
