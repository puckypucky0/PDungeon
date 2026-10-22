package dev.pucky.placeholder;

import dev.pucky.PDungeon;
import dev.pucky.config.ConfigManager;
import dev.pucky.dungeon.DungeonInstance;
import dev.pucky.dungeon.DungeonManager;
import dev.pucky.dungeon.DungeonStatus;
import dev.pucky.util.CommandUtils;
import dev.pucky.util.MMUtil;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class DungeonExpansion extends PlaceholderExpansion {

    @Override public @NotNull String getIdentifier() { return "pdungeon"; }
    @Override public @NotNull String getAuthor() { return "pucky"; }
    @Override public @NotNull String getVersion() { return "1.0.0"; }
    @Override public boolean persist() { return true; }

    // %pdungeon_arena_3_wave% "= "arena_3_wave"
    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (params.equals("title"))
            return PDungeon.getInstance().getConfigManager().getPlaceholderText("pdungeon")
                    .replace("{version}", PDungeon.getInstance().getPluginMeta().getVersion());
        String[] parts = params.split("_", 3);           // ["arena", "3", "wave"]
        if (parts.length != 3 || !parts[0].equals("arena")) return null;

        Integer id = CommandUtils.parseIntOrNull(parts[1]);
        if (id == null) return null;

        DungeonInstance instance = DungeonManager.getAll().get(id);
        ConfigManager config = PDungeon.getInstance().getConfigManager();
        String key = parts[2];

        return switch (key) {
            case "status" -> instance == null
                    ? config.getPlaceholderText("status.empty")
                    : config.getPlaceholderText(instance.getStatus() == DungeonStatus.WAITING
                                                ? "status.between_waves" : "status.fighting");
            case "wave" -> text(config, key, instance, () -> instance.getCurrentWave());
            case "max_wave" -> text(config, key, instance, () -> instance.getDifficulty().waves().size());
            case "alive_player_count" -> text(config, key, instance, () -> instance.getPlayers().size());
            case "alive_mobs" -> text(config, key, instance, () -> instance.getAliveMobs().size());
            case "pending_mobs" -> text(config, key, instance, () -> instance.getPendingSpawns());
            case "time" -> text(config, key, instance, () -> {
                long seconds = (System.currentTimeMillis() - instance.getStartedAt()) / 1000;
                return String.format("%02d:%02d", seconds / 60, seconds % 60);
            });
            case "difficulty" -> text(config, key, instance, () -> MMUtil.serialize(instance.getDifficulty().name()));
            default -> null;
        };
    }

    private String text(ConfigManager config, String key, DungeonInstance instance, Supplier<Object> value) {
        if (instance == null) return config.getPlaceholderText("empty");
        return config.getPlaceholderText(key).replace("{value}", String.valueOf(value.get()));
    }
}