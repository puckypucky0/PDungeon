package dev.pucky.util;

import dev.pucky.PDungeon;
import dev.pucky.dungeon.DungeonParticle;
import dev.pucky.util.logs.Logger;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CheckUtil {
    public static <T> T getOrDefault(String path, Class<T> type, T def) {
        FileConfiguration config = PDungeon.instance.getConfig();
        Object value = config.get(path);

        if (!config.contains(path, true) || !type.isInstance(value)) {
            Logger.warn("<dark_red>" + path + " <gray>is missing or invalid. <dark_red>Using default: <white>" + def);
            return def;
        }
        return type.cast(value);
    }

    private static double number(ConfigurationSection section, String key, double def) {
        if (section.get(key) instanceof Number n && n.doubleValue() >= 0)
            return n.doubleValue();

        Logger.fail("<red>Particle " + key + " is malformed or missing. <white>Using " + def + " for default.");
        return def;
    }

    public static DungeonParticle validDungeonParticle(String configKey) {
        ConfigurationSection section = PDungeon.instance.getConfig().getConfigurationSection("settings." + configKey);
        if (section == null) section = new MemoryConfiguration(); // default config

        Particle particle = null;
        NamespacedKey key = section.get("key") instanceof String s ? NamespacedKey.fromString(s) : null;
        if (key != null) particle = Registry.PARTICLE_TYPE.get(key);

        if (particle == null || particle.getDataType() != Void.class) {
            Logger.fail("<red>Particle key is malformed, missing or <yellow>parameterized (not supported for this version). <white>Using flame for default.");
            particle = Particle.FLAME;
        }

        return DungeonParticle.builder()
                .particleType(particle)
                .particleMultiplier(number(section, "multiplier", 1.0))
                .offsetX(number(section, "offsetX", 0.3))
                .offsetY(number(section, "offsetY", 0.6))
                .offsetZ(number(section, "offsetZ", 0.3))
                .offsetExtra(number(section, "offsetExtra", 0.02))
                .build();
    }

    public static List<String> stringListVal(String id, String keyName, Object value, List<String> def) {
        List<String> list = new ArrayList<>();
        boolean bool = false;

        if (value instanceof List<?> l) {
            for (Object o : l) {
                if (o instanceof String)
                    list.add((String) o);
                else bool = true;
            }
        }

        if (bool)
            Logger.fatal("<red>Some of the values of list " + keyName + " isn't String, some values may be missing -- " + id);

        if (value == null || list.isEmpty()) {
            Logger.fatal("<red>" + keyName + " has fallen back to default value.");
            return def;
        }

        return list;
    }

    public static double doubleVal(String id, String keyName, Object value, double def) {
        if (value instanceof Number n && n.doubleValue() >= 0)
            return n.doubleValue();

        Logger.fail("<red>" + keyName + " is malformed or missing. <white>Using " + def + " for default - " + id);
        return def;
    }

    public static int intVal(String id, String keyName, Object value, int def) {
        if (value instanceof Number n && n.intValue() >= 0)
            return n.intValue();

        Logger.fail("<red>" + keyName + " is malformed or missing. <white>Using " + def + " for default - " + id);
        return def;
    }

    public static Map<?, ?> mapVal(String id, String keyName, Object value) {
        if (value instanceof Map<?, ?> map)
            return map;

        Logger.fail("<red>" + keyName + " is malformed or missing. <white>Using empty map for default - " + id);
        return Map.of();
    }

    public static String stringVal(String id, String keyName, Object value, String def) {
        if (value instanceof String s) {
            String stripped = MMUtil.stripLegacy(s);
            if (!stripped.isBlank())
                return stripped;
        }

        Logger.fail("<red>" + keyName + " is malformed or missing. <white>Using " + def + " for default - " + id);
        return def;
    }

    public static Boolean booleanVal(String id, String keyName, Object value, boolean def) {
        if (value instanceof Boolean b) {
            return b;
        }

        Logger.fail("<red>" + keyName + " is malformed or missing. <white>Using " + def + " for default - " + id);
        return def;
    }
}
