package com.martelstudios.openquests.core;

import javax.annotation.Nonnull;
import java.lang.reflect.Proxy;

/**
 * Stand-ins failing on any call, for a dependency a test means to show is left alone.
 */
public final class Untouched {

    private Untouched() {}

    @Nonnull
    public static <T> T of(@Nonnull Class<T> type) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, (proxy, method, args) -> switch (method.getName()) {
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> proxy == args[0];
            case "toString" -> type.getSimpleName();
            default -> throw new AssertionError("unexpected call to " + type.getSimpleName() + "." + method.getName());
        }));
    }
}
