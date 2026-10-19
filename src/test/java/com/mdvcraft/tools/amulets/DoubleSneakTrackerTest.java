package com.mdvcraft.tools.amulets;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DoubleSneakTrackerTest {
    private final DoubleSneakTracker<String> tracker = new DoubleSneakTracker<>();
    private final UUID player = UUID.randomUUID();

    @Test
    void requiresTwoPressesAndConsumesThePair() {
        assertFalse(tracker.press(player, "amulet", 0, 450));
        assertTrue(tracker.press(player, "amulet", 450, 450));
        assertFalse(tracker.press(player, "amulet", 451, 450));
        assertTrue(tracker.press(player, "amulet", 452, 450));
    }

    @Test
    void expiredFirstPressStartsANewPair() {
        assertFalse(tracker.press(player, "amulet", 0, 450));
        assertFalse(tracker.press(player, "amulet", 451, 450));
        assertTrue(tracker.press(player, "amulet", 452, 450));
    }

    @Test
    void changingAmuletOrPlayerCannotCompleteTheFirstPair() {
        assertFalse(tracker.press(player, "one", 0, 450));
        assertFalse(tracker.press(UUID.randomUUID(), "one", 1, 450));
        assertFalse(tracker.press(player, "two", 2, 450));
        assertTrue(tracker.press(player, "two", 3, 450));
    }

    @Test
    void quittingOrReloadingForgetsPendingPresses() {
        assertFalse(tracker.press(player, "amulet", 0, 450));
        tracker.forget(player);
        assertFalse(tracker.press(player, "amulet", 1, 450));
        tracker.clear();
        assertFalse(tracker.press(player, "amulet", 2, 450));
    }
}
