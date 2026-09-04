package com.mdvcraft.tools.fishing.mmocore;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;
import java.util.Optional;

/** Optional MythicMobs support without adding MythicMobs as a compile dependency. */
final class MythicMobReflectionSpawner {
    private final JavaPlugin owner;
    private boolean warnedMissing;

    MythicMobReflectionSpawner(JavaPlugin owner) {
        this.owner = owner;
    }

    boolean spawn(String mobId, int level, Location location) {
        Plugin mythic = Bukkit.getPluginManager().getPlugin("MythicMobs");
        if (mythic == null || !mythic.isEnabled()) {
            warnMissing("MythicMobs no está habilitado; no se pudo invocar '" + mobId + "'.");
            return false;
        }

        try {
            Class<?> mythicBukkitClass = Class.forName("io.lumine.mythic.bukkit.MythicBukkit");
            Object instance = mythicBukkitClass.getMethod("inst").invoke(null);
            Object mobManager = instance.getClass().getMethod("getMobManager").invoke(instance);

            Method getMythicMob = findMethod(mobManager.getClass(), "getMythicMob", String.class);
            if (getMythicMob == null) throw new NoSuchMethodException("getMythicMob(String)");

            Object result = getMythicMob.invoke(mobManager, mobId);
            Object mythicMob;
            if (result instanceof Optional<?> optional) mythicMob = optional.orElse(null);
            else mythicMob = result;

            if (mythicMob == null) {
                owner.getLogger().warning("[FishingDrops] MythicMob inexistente: '" + mobId + "'.");
                return false;
            }

            Class<?> adapterClass = Class.forName("io.lumine.mythic.bukkit.BukkitAdapter");
            Method adapt = adapterClass.getMethod("adapt", Location.class);
            Object mythicLocation = adapt.invoke(null, location);

            Method spawnMethod = findSpawnMethod(mythicMob.getClass(), mythicLocation.getClass());
            if (spawnMethod == null) throw new NoSuchMethodException("MythicMob#spawn(AbstractLocation, level)");

            Class<?> levelType = spawnMethod.getParameterTypes()[1];
            Object levelArg;
            if (levelType == int.class || levelType == Integer.class) levelArg = level;
            else if (levelType == float.class || levelType == Float.class) levelArg = (float) level;
            else if (levelType == double.class || levelType == Double.class) levelArg = (double) level;
            else if (levelType == long.class || levelType == Long.class) levelArg = (long) level;
            else throw new IllegalStateException("Tipo de nivel MythicMobs no soportado: " + levelType.getName());

            spawnMethod.invoke(mythicMob, mythicLocation, levelArg);
            return true;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            owner.getLogger().warning("[FishingDrops] No se pudo invocar MythicMob '" + mobId + "': "
                    + exception.getClass().getSimpleName()
                    + (exception.getMessage() == null ? "" : " - " + exception.getMessage()));
            return false;
        }
    }

    private void warnMissing(String message) {
        if (warnedMissing) return;
        warnedMissing = true;
        owner.getLogger().warning("[FishingDrops] " + message);
    }

    private Method findMethod(Class<?> type, String name, Class<?>... params) {
        Class<?> current = type;
        while (current != null) {
            try {
                Method method = current.getMethod(name, params);
                method.setAccessible(true);
                return method;
            } catch (NoSuchMethodException ignored) {
                try {
                    Method method = current.getDeclaredMethod(name, params);
                    method.setAccessible(true);
                    return method;
                } catch (NoSuchMethodException ignoredAgain) {
                    current = current.getSuperclass();
                }
            }
        }
        return null;
    }

    private Method findSpawnMethod(Class<?> mobClass, Class<?> locationClass) {
        for (Method method : mobClass.getMethods()) {
            if (!method.getName().equals("spawn") || method.getParameterCount() != 2) continue;
            Class<?>[] params = method.getParameterTypes();
            if (!params[0].isAssignableFrom(locationClass) && !locationClass.isAssignableFrom(params[0])) continue;
            if (!isNumeric(params[1])) continue;
            method.setAccessible(true);
            return method;
        }
        return null;
    }

    private boolean isNumeric(Class<?> type) {
        return type == int.class || type == Integer.class
                || type == float.class || type == Float.class
                || type == double.class || type == Double.class
                || type == long.class || type == Long.class;
    }
}
