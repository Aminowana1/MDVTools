package com.mdvcraft.tools.amulets;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Tracks pairs of sneak presses. Releases are handled by the Bukkit listener. */
final class DoubleSneakTracker<T> {
    private final Map<UUID, Press<T>> firstPresses = new HashMap<>();

    boolean press(UUID playerId, T item, long now, long windowNanos) {
        Press<T> previous = firstPresses.remove(playerId);
        if (previous != null && now - previous.time >= 0L
                && now - previous.time <= windowNanos && Objects.equals(previous.item, item)) {
            return true;
        }
        firstPresses.put(playerId, new Press<>(item, now));
        return false;
    }

    void forget(UUID playerId) {
        firstPresses.remove(playerId);
    }

    void clear() {
        firstPresses.clear();
    }

    private record Press<T>(T item, long time) { }
}
