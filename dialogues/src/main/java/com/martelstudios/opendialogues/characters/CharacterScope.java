package com.martelstudios.opendialogues.characters;

/**
 * Where a unique character is unique: only {@link Uniqueness#Alive} and {@link Uniqueness#Forever}
 * care.
 */
public enum CharacterScope {
    /** Once per world, each world drawing it on its own. */
    World,
    /** Once in the whole universe, whatever world draws it first. */
    Universe
}
