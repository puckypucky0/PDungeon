package dev.pucky.dungeon;

import dev.pucky.util.logs.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DungeonManager {
    private static final Map<Integer, DungeonInstance> activeDungeons = new HashMap<>();

    public static boolean add(DungeonInstance instance) {
        boolean isActive = isActive(instance.getArenaConfig().id());
        if (!isActive)
            activeDungeons.put(instance.getArenaConfig().id(), instance);

        else {
            Logger.warn("<gray>A dungeon with ID <yellow>" + instance.getArenaConfig().id() + " <red>didn't start.");
            Logger.warn("<gray>there is another active dungeon with that ID.");
        }

        return !isActive;
    }

    public static void remove(int id) {
        activeDungeons.remove(id);
    }

    public static DungeonInstance get(int id) {
        return activeDungeons.get(id);
    }

    public static DungeonInstance get(UUID player) {
        for (DungeonInstance value : activeDungeons.values()) {
            if (value.getPlayers().contains(player))
                return value;
        }

        return null;
    }

    public static boolean isActive(int id) {
        return activeDungeons.containsKey(id);
    }

    public static Map<Integer, DungeonInstance> getAll() {
        return Map.copyOf(activeDungeons);
    }
}
