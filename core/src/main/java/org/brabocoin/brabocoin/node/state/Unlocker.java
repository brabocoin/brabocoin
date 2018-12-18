package org.brabocoin.brabocoin.node.state;

import org.brabocoin.brabocoin.util.Destructible;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

/**
 * Unlocks or creates an object that is password-encrypted.
 *
 * @param <T> The type of the object that is encrypted.
 */
@FunctionalInterface
public interface Unlocker<T> {

    @Nullable T unlock(boolean creation, @NotNull Function<@NotNull Destructible<char[]>, @Nullable T> creator);
}
