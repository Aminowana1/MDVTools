package com.mdvcraft.tools.amulets;

import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

/** Optional official Floodgate/Geyser APIs; names and UUID patterns are never used. */
final class BedrockPlayerDetector implements Predicate<UUID> {
    private final Plugin plugin;
    private final List<Bridge> bridges = new ArrayList<>();
    private final Set<String> reportedFailures = new HashSet<>();

    BedrockPlayerDetector(Plugin plugin) {
        this.plugin = plugin;
    }

    void reload() {
        bridges.clear();
        reportedFailures.clear();
        addBridge("floodgate", "org.geysermc.floodgate.api.FloodgateApi",
                "getInstance", "isFloodgatePlayer", true);
        addBridge("Geyser-Spigot", "org.geysermc.geyser.api.GeyserApi",
                "api", "connectionByUuid", false);
        if (bridges.isEmpty()) {
            plugin.getLogger().warning("Doble shift de Bedrock: no hay API de Floodgate/Geyser disponible. "
                    + "Instala Floodgate en este servidor (con los datos del proxy) o Geyser-Spigot.");
        }
    }

    private void addBridge(String pluginName, String className, String instanceMethod,
                           String lookupMethod, boolean booleanResult) {
        Plugin provider = plugin.getServer().getPluginManager().getPlugin(pluginName);
        if (provider == null || !provider.isEnabled()) return;
        try {
            Class<?> api = Class.forName(className, true, provider.getClass().getClassLoader());
            bridges.add(new Bridge(provider, api.getMethod(instanceMethod),
                    api.getMethod(lookupMethod, UUID.class), booleanResult));
        } catch (ReflectiveOperationException | LinkageError exception) {
            reportFailure(pluginName, exception);
        }
    }

    @Override
    public boolean test(UUID playerId) {
        for (Bridge bridge : bridges) {
            if (!bridge.provider.isEnabled()) continue;
            try {
                Object api = bridge.instance.invoke(null);
                if (api == null) continue;
                Object result = bridge.lookup.invoke(api, playerId);
                if (bridge.booleanResult ? Boolean.TRUE.equals(result) : result != null) return true;
            } catch (ReflectiveOperationException | LinkageError exception) {
                reportFailure(bridge.provider.getName(), exception);
            }
        }
        return false;
    }

    private void reportFailure(String provider, Throwable exception) {
        if (reportedFailures.add(provider)) {
            plugin.getLogger().warning("No pude consultar la API de " + provider
                    + " para el doble shift: " + exception.getClass().getSimpleName());
        }
    }

    private record Bridge(Plugin provider, Method instance, Method lookup, boolean booleanResult) { }
}
