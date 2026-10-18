package com.martelstudios.openquests.mmoskilltree.skills;

import javax.annotation.Nullable;

/**
 * A quest asset or reward naming an MMO Skill Tree skill, checked once every asset is loaded:
 * skills are configuration, so no codec validator can tell a typo from a custom skill.
 */
public interface SkillReference {

    /**
     * @return the skill as written, {@code null} when it is left open.
     */
    @Nullable
    String getSkill();
}
