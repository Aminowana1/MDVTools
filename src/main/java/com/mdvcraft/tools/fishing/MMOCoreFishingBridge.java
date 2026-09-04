package com.mdvcraft.tools.fishing;

import org.bukkit.Bukkit;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Reflection bridge to MMOCore's FishingManager.
 *
 * MMOCore versions differ in the calculateDropTable signature:
 * - older builds: calculateDropTable(Player)
 * - current builds: calculateDropTable(Player, FishHook)
 *
 * Supporting both keeps MDVTools source independent from MMOCore at compile time.
 */
public final class MMOCoreFishingBridge {
    private final JavaPlugin owner;

    private String pluginName = "MMOCore";
    private Plugin mmocorePlugin;
    private Object fishingManager;
    private Method calculateDropTableMethod;
    private InvocationMode invocationMode;
    private boolean resolved;
    private boolean warned;
    private boolean announced;

    public MMOCoreFishingBridge(JavaPlugin owner) {
        this.owner = owner;
    }

    public void reload(String pluginName) {
        this.pluginName = pluginName == null || pluginName.isBlank() ? "MMOCore" : pluginName.trim();
        this.mmocorePlugin = null;
        this.fishingManager = null;
        this.calculateDropTableMethod = null;
        this.invocationMode = null;
        this.resolved = false;
        this.warned = false;
        this.announced = false;
    }

    /**
     * @return true when MMOCore has a custom fishing drop table matching this
     * player and hook. If MMOCore cannot be resolved, false is returned so
     * vanilla fishing is never altered in strict mode.
     */
    public boolean hasCustomFishingTable(Player player, FishHook hook) {
        if (!resolve()) return false;

        try {
            Object table = switch (invocationMode) {
                case PLAYER_AND_HOOK -> calculateDropTableMethod.invoke(fishingManager, player, hook);
                case PLAYER_ONLY -> calculateDropTableMethod.invoke(fishingManager, player);
            };
            return table != null;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            warnOnce("No se pudo consultar la tabla de pesca custom de MMOCore", exception);
            return false;
        }
    }

    public boolean isAvailable() {
        return resolve();
    }

    public String resolvedSignature() {
        if (!resolve() || invocationMode == null) return "no-resuelto";
        return invocationMode == InvocationMode.PLAYER_AND_HOOK
                ? "calculateDropTable(Player, FishHook)"
                : "calculateDropTable(Player)";
    }

    private boolean resolve() {
        if (resolved) {
            return mmocorePlugin != null && fishingManager != null && calculateDropTableMethod != null && invocationMode != null;
        }
        resolved = true;

        mmocorePlugin = Bukkit.getPluginManager().getPlugin(pluginName);
        if (mmocorePlugin == null || !mmocorePlugin.isEnabled()) {
            warnOnce("No se encontró el plugin '" + pluginName + "' habilitado", null);
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

            MethodMatch match = findCalculateMethod(fishingManager.getClass());
            if (match == null) {
                warnOnce("No se encontró FishingManager#calculateDropTable(Player, FishHook) ni la firma legacy (Player)", null);
                return false;
            }

            calculateDropTableMethod = match.method();
            invocationMode = match.mode();
            calculateDropTableMethod.setAccessible(true);

            if (!announced) {
                announced = true;
                owner.getLogger().info("[FishingFightTimer] Puente MMOCore listo: " + resolvedSignatureNoResolve() + ".");
            }
            return true;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            warnOnce("No se pudo inicializar el puente de pesca con MMOCore", exception);
            return false;
        }
    }

    private String resolvedSignatureNoResolve() {
        if (invocationMode == null) return "no-resuelto";
        return invocationMode == InvocationMode.PLAYER_AND_HOOK
                ? "calculateDropTable(Player, FishHook)"
                : "calculateDropTable(Player)";
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

    private MethodMatch findCalculateMethod(Class<?> type) {
        MethodMatch legacy = null;
        Class<?> current = type;
        while (current != null) {
            for (Method method : current.getDeclaredMethods()) {
                if (!method.getName().equals("calculateDropTable")) continue;

                Class<?>[] parameters = method.getParameterTypes();
                if (parameters.length == 2 && isPlayerParameter(parameters[0]) && isFishHookParameter(parameters[1])) {
                    return new MethodMatch(method, InvocationMode.PLAYER_AND_HOOK);
                }

                if (parameters.length == 1 && isPlayerParameter(parameters[0]) && legacy == null) {
                    legacy = new MethodMatch(method, InvocationMode.PLAYER_ONLY);
                }
            }
            current = current.getSuperclass();
        }
        return legacy;
    }

    private boolean isPlayerParameter(Class<?> type) {
        return type.isAssignableFrom(Player.class) || Player.class.isAssignableFrom(type);
    }

    private boolean isFishHookParameter(Class<?> type) {
        return type.isAssignableFrom(FishHook.class) || FishHook.class.isAssignableFrom(type);
    }

    private void warnOnce(String message, Throwable throwable) {
        if (warned) return;
        warned = true;
        if (throwable == null) {
            owner.getLogger().warning("[FishingFightTimer] " + message
                    + ". El límite total no se aplicará mientras 'only-mmocore-custom-fishing' sea true.");
        } else {
            owner.getLogger().warning("[FishingFightTimer] " + message + ": "
                    + throwable.getClass().getSimpleName()
                    + (throwable.getMessage() == null ? "" : " - " + throwable.getMessage()));
        }
    }

    private enum InvocationMode {
        PLAYER_AND_HOOK,
        PLAYER_ONLY
    }

    private record MethodMatch(Method method, InvocationMode mode) {
    }
}
