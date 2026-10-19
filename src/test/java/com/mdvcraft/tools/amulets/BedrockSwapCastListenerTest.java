package com.mdvcraft.tools.amulets;

import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BedrockSwapCastListenerTest {
    private final UUID id = UUID.randomUUID();
    private final Player player = mock(Player.class);
    private final PlayerInventory inventory = mock(PlayerInventory.class);
    private final ItemStack amulet = mock(ItemStack.class);
    private final ItemStack weapon = mock(ItemStack.class);
    private final AtomicLong clock = new AtomicLong();
    private final List<PlayerSwapHandItemsEvent> casts = new ArrayList<>();
    private final MemoryConfiguration config = new MemoryConfiguration();

    @BeforeEach
    void prepare() {
        when(player.getUniqueId()).thenReturn(id);
        when(player.getInventory()).thenReturn(inventory);
        when(player.isOnline()).thenReturn(true);
        when(inventory.getItemInOffHand()).thenReturn(amulet);
        when(inventory.getItemInMainHand()).thenReturn(weapon);
        when(amulet.clone()).thenReturn(amulet);
        when(weapon.clone()).thenReturn(weapon);
    }

    private BedrockSwapCastListener listener(boolean bedrock) {
        Predicate<ItemStack> matches = item -> item == amulet;
        BedrockSwapCastListener listener = new BedrockSwapCastListener(matches, uuid -> bedrock,
                casts::add, clock::get);
        listener.reload(config);
        return listener;
    }

    private void press(BedrockSwapCastListener listener, long millis) {
        clock.set(TimeUnit.MILLISECONDS.toNanos(millis));
        listener.onSneak(new PlayerToggleSneakEvent(player, true));
    }

    @Test
    void javaDoubleSneakNeverDispatchesAnySwap() {
        BedrockSwapCastListener listener = listener(false);
        press(listener, 0);
        press(listener, 100);
        press(listener, 200);
        assertTrue(casts.isEmpty());
        verifyNoInteractions(inventory);
    }

    @Test
    void bedrockRequiresSecondPressAndDoesNotMoveItems() {
        BedrockSwapCastListener listener = listener(true);
        press(listener, 0);
        listener.onSneak(new PlayerToggleSneakEvent(player, false));
        assertTrue(casts.isEmpty());
        press(listener, 100);
        assertEquals(1, casts.size());
        assertSame(amulet, casts.getFirst().getMainHandItem());
        assertSame(weapon, casts.getFirst().getOffHandItem());
        assertTrue(casts.getFirst().isCancelled());
        verify(inventory, never()).setItemInMainHand(any());
        verify(inventory, never()).setItemInOffHand(any());
        verify(player, never()).setSneaking(anyBoolean());
    }

    @Test
    void dispatchBeginsUncancelledAndCancelsBeforeMonitorWithoutRestoringDurability() {
        BedrockSwapCastListener[] holder = new BedrockSwapCastListener[1];
        ItemStack spentAmulet = mock(ItemStack.class);
        holder[0] = new BedrockSwapCastListener(item -> item == amulet, uuid -> true, event -> {
            assertFalse(event.isCancelled(), "MythicLib must receive an uncancelled F event");
            // A successful ability may update or break the actual amulet.
            when(inventory.getItemInOffHand()).thenReturn(spentAmulet);
            holder[0].preventSyntheticSwap(event);
            assertTrue(event.isCancelled(), "MONITOR listeners must see no real hand swap");
            casts.add(event);
        }, clock::get);
        holder[0].reload(config);
        press(holder[0], 0);
        press(holder[0], 100);
        assertSame(spentAmulet, inventory.getItemInOffHand());
        assertEquals(1, casts.size());
        verify(inventory, never()).setItemInOffHand(any());
    }

    @Test
    void onlyConfiguredOffhandCanStartAGesture() {
        BedrockSwapCastListener listener = listener(true);
        when(inventory.getItemInOffHand()).thenReturn(weapon);
        press(listener, 0);
        press(listener, 100);
        assertTrue(casts.isEmpty());
        when(inventory.getItemInOffHand()).thenReturn(amulet);
        press(listener, 200);
        assertTrue(casts.isEmpty());
        press(listener, 300);
        assertEquals(1, casts.size());
    }

    @Test
    void releasingAndExpiredWindowDoNotCastAndReloadForgetsFirstPress() {
        BedrockSwapCastListener listener = listener(true);
        press(listener, 0);
        listener.onSneak(new PlayerToggleSneakEvent(player, false));
        press(listener, 451);
        assertTrue(casts.isEmpty());
        listener.reload(config);
        press(listener, 452);
        assertTrue(casts.isEmpty());
        press(listener, 453);
        assertEquals(1, casts.size());
    }

    @Test
    void disabledFeatureAndQuitPreventPendingCast() {
        BedrockSwapCastListener listener = listener(true);
        press(listener, 0);
        listener.onQuit(new PlayerQuitEvent(player, "bye"));
        press(listener, 100);
        assertTrue(casts.isEmpty());
        config.set("offhand-swap-cast.bedrock-double-sneak.enabled", false);
        listener.reload(config);
        press(listener, 200);
        press(listener, 300);
        assertTrue(casts.isEmpty());
    }

    @Test
    void alteredSneakStateCannotAccidentallyTriggerShiftSwapItems() {
        BedrockSwapCastListener listener = listener(true);
        press(listener, 0);
        when(player.isSneaking()).thenReturn(true);
        press(listener, 100);
        assertTrue(casts.isEmpty());
    }
}
