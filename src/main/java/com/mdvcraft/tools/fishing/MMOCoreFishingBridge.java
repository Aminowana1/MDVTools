package com.mdvcraft.tools.fishing;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Tiny reflection bridge used only to determine whether MMOCore has an active
 * custom fishing drop table for a player. MDVTools does not depend on MMOCore
 * at compile time, so updating MMOCore does not force this module to be rebuilt.
 */
public final class MMOCoreFishingBridge {
    private final JavaPlugin owner;

    private String pluginName = "MMOCore";
    private Plugin mmocorePlugin;
    private Object fishingManager;
    private Method calculateDropTableMethod;
    private boolean resolved;
    private boolean warned;

    public MMOCoreFishingBridge(JavaPlugin owner) {
        this.owner = owner;
    }

    public void reload(String pluginName) {
        this.pluginName = pluginName == null || pluginName.isBlank() ? "MMOCore" : pluginName.trim();
        this.mmocorePlugin = null;
        this.fishingManager = null;
        this.calculateDropTableMethod = null;
        this.resolved = false;
        this.warned = false;
    }

    /**
     * @return true only when MMOCore is present and calculateDropTable(player)
     * returns a non-null custom fishing table.
     */
    public boolean hasCustomFishingTable(Player player) {
        if (!resolve()) return false;

        try {
            return calculateDropTableMethod.invoke(fishingManager, player) != null;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            warnOnce("No se pudo consultar la tabla de pesca custom de MMOCore", exception);
            return false;
        }
    }

    private boolean resolve() {
        if (resolved) {
            return mmocorePlugin != null && fishingManager != null && calculateDropTableMethod != null;
        }
        resolved = true;

        mmocorePlugin = Bukkit.getPluginManager().getPlugin(pluginName);
        if (mmocorePlugin == null || !mmocorePlugin.isEnabled()) {
            return false;
        }

        try {
            Field managerField = findField(mmocorePlugin.getClass(), "fishingManager");
            if (managerField == null) {
                warnOnce("No se encontró MMOCore.fishingManager", null);
                return false;
            }
            managerField.setAccessible(true);
            fishingManager = managerField.get(mmocorePlugin);
            if (fishingManager == null) {
                warnOnce("MMOCore.fishingManager es null", null);
                return false;
            }

            calculateDropTableMethod = findCalculateMethod(fishingManager.getClass());
            if (calculateDropTableMethod == null) {
                warnOnce("No se encontró FishingManager#calculateDropTable(Player)", null);
                return false;
            }
            calculateDropTableMethod.setAccessible(true);
            return true;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            warnOnce("No se pudo inicializar el puente de pesca con MMOCore", exception);
            return false;
        }
    }

    private Field findField(Class<?> type, String name) {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    private Method findCalculateMethod(Class<?> type) {
        Class<?> current = type;
        while (current != null) {
            for (Method method : current.getDeclaredMethods()) {
                if (!method.getName().equals("calculateDropTable") || method.getParameterCount() != 1) continue;
                Class<?> parameter = method.getParameterTypes()[0];
                if (parameter.isAssignableFrom(Player.class) || Player.class.isAssignableFrom(parameter)) {
                    return method;
                }
            }
            current = current.getSuperclass();
        }
        return null;
    }

    private void warnOnce(String message, Throwable throwable) {
        if (warned) return;
        warned = true;
        if (throwable == null) {
            owner.getLogger().warning("[FishingFightTimer] " + message + ". El límite total no se aplicará mientras 'only-mmocore-custom-fishing' sea true.");
        } else {
            owner.getLogger().warning("[FishingFightTimer] " + message + ": "
                    + throwable.getClass().getSimpleName()
                    + (throwable.getMessage() == null ? "" : " - " + throwable.getMessage()));
        }
    }
}
