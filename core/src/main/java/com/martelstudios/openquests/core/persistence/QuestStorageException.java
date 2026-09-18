package com.martelstudios.openquests.core.persistence;

import javax.annotation.Nonnull;

/**
 * A backend could not do what was asked of it. Unchecked on purpose: persistence sits under
 * everything, and a checked exception would reach code that has nothing to answer with.
 */
public class QuestStorageException extends RuntimeException {

    public QuestStorageException(@Nonnull String message) {
        super(message);
    }

    public QuestStorageException(@Nonnull String message, @Nonnull Throwable cause) {
        super(message, cause);
    }
}
