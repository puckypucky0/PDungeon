package dev.pucky.config;

import dev.pucky.util.CommandUtils;
import dev.pucky.util.MMUtil;
import dev.pucky.util.logs.Logger;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public final class ConfigManager {

    private final JavaPlugin plugin;

    private final Map<Integer, Arena> arenas = new LinkedHashMap<>();
    private final Map<String, Mob> mobs = new LinkedHashMap<>();
    private final Map<String, Difficulty> difficulties = new TreeMap<>();
    private final Map<String, String> placeholderText = new HashMap<>();

    @Getter private int maxPlayers;
    @Getter private int waveDelay;
    @Getter private int spawnInterval;
    private Location globalRespawn;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();

        arenas.clear();
        mobs.clear();
        difficulties.clear();
        placeholderText.clear();

        maxPlayers = plugin.getConfig().getInt("settings.max-players-per-arena", 4);
        waveDelay = plugin.getConfig().getInt("settings.wave-delay-seconds", 5);
        spawnInterval = plugin.getConfig().getInt("settings.spawn-interval-tick", 10);

        // tüm arenalar için ortak dönüş noktası (başka dünyada olabilir)
        World respawnWorld = Bukkit.getWorld(plugin.getConfig().getString("settings.respawn-location.world", ""));
        globalRespawn = respawnWorld == null ? null
                : toLocation(respawnWorld, plugin.getConfig().getString("settings.respawn-location.position"));
        if (globalRespawn == null)
            warn("settings.respawn-location geçersiz ya da dünyası yüklü değil.");

        loadArenas();
        loadMobs();
        loadDifficulties();
        loadPlaceholders();

        plugin.getLogger().info(arenas.size() + " arena, " + mobs.size() + " mob, " + difficulties.size() + " zorluk yüklendi.");
    }

    private void loadArenas() {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("arenas");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            ConfigurationSection s = section.getConfigurationSection(key);
            if (s == null) continue;

            int id = Integer.parseInt(key);
            World world = Bukkit.getWorld(s.getString("world", ""));

            if (world == null) {
                warn("Arena " + id + ": dünya bulunamadı.");
                continue;
            }

            Location playerSpawn = toLocation(world, s.getString("player-spawn"));
            if (playerSpawn == null) {
                warn("Arena " + id + ": player-spawn geçersiz.");
                continue;
            }

            List<Location> mobSpawns = new ArrayList<>();
            for (String raw : s.getStringList("mob-spawns")) {
                Location loc = toLocation(world, raw);
                if (loc != null) mobSpawns.add(loc);
            }
            if (mobSpawns.isEmpty()) {
                warn("Arena " + id + ": mob-spawns boş.");
                continue;
            }

            // dönüş noktası: arenanın kendi respawn-location'ı > settings.respawn-location > dünyanın spawn'ı
            Location respawnLocation = toLocation(world, s.getString("respawn-location"));
            if (respawnLocation == null) respawnLocation = globalRespawn;
            if (respawnLocation == null) {
                warn("Arena " + id + ": respawn-location yok, dünyanın spawn noktası kullanılacak.");
                respawnLocation = world.getSpawnLocation();
            }

            arenas.put(id, new Arena(id, playerSpawn, respawnLocation, mobSpawns));
        }
    }

    private void loadMobs() {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("mobs");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            ConfigurationSection s = section.getConfigurationSection(key);
            if (s == null) continue;

            String id = key.toUpperCase(Locale.ROOT);
            EntityType type = EntityType.valueOf(s.getString("type", "ZOMBIE").toUpperCase(Locale.ROOT));
            Component name = MiniMessage.miniMessage().deserialize(s.getString("name", id));

            mobs.put(id, new Mob(
                            id,
                            name,
                            type,
                            this.loadLoot(s.getMapList("loot")),
                            s.getDouble("health", 20),
                            s.getDouble("damage", 3),
                            s.getDouble("scale", 1),
                            s.getDouble("movementSpeedMultiplier", 1)
                    )
            );
        }
    }

    private void loadPlaceholders() {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("placeholders");
        if (section == null) return;

        Set<String> keys = section.getKeys(true);

        for (String key : keys) {
            if (!section.isString(key)) continue;
            placeholderText.putIfAbsent(key, section.getString(key));
        }
    }

    private Map<Integer, Wave> loadWaves(ConfigurationSection wavesSection) {
        if (wavesSection == null) return null;

        TreeMap<Integer, Wave> waveMap = new TreeMap<>();

        for (String waveId : wavesSection.getKeys(false)) { // wave id's 1, 2
            ConfigurationSection mobsSection = wavesSection.getConfigurationSection(waveId);
            if (mobsSection == null) continue;

            Map<String, Integer> mobsMap = new LinkedHashMap<>();

            for (String mobKey : mobsSection.getKeys(false)) {
                String mobId = mobKey.toUpperCase(Locale.ROOT);

                if (mobs.containsKey(mobId))
                    mobsMap.put(mobId, mobsSection.getInt(mobKey));
                else warn("Dalga " + waveId + ": tanımsız mob '" + mobId + "'.");
            }

            int waveIdInt = Integer.parseInt(waveId);

            if (!mobsMap.isEmpty()) waveMap.put(waveIdInt, new Wave(waveIdInt, mobsMap));

        }
        return waveMap;
    }

    private List<LootEntry> loadLoot(List<Map<?, ?>> list) {
        List<LootEntry> lootEntries = new ArrayList<>();

        for (Map<?, ?> lootDataMap : list) {
            boolean itemCheck = lootDataMap.get("item") instanceof String;
            boolean chanceCheck = lootDataMap.get("chance") instanceof Number;
            boolean minCheck = lootDataMap.get("min") instanceof Integer;
            boolean maxCheck = lootDataMap.get("max") instanceof Integer;
            boolean check = itemCheck && chanceCheck && minCheck && maxCheck;

            if (check) {

                String itemBase64 = (String) lootDataMap.get("item");
                double chance = ((Number) lootDataMap.get("chance")).doubleValue();
                int min = (int) lootDataMap.get("min");
                int max = (int) lootDataMap.get("max");

                ItemStack itemStack = null;
                try {
                    itemStack = CommandUtils.decode(itemBase64);
                } catch (Exception e) {
                    Logger.fail("<yellow>Failed to decode <red>Base64 item.");
                    Logger.fail("<yellow>" + itemBase64.substring(0, 15) + "...");
                }

                if (itemStack != null) {
                    lootEntries.add(
                            new LootEntry(
                                    itemStack,
                                    chance,
                                    min,
                                    max
                            )
                    );
                }
            }
        }
        return lootEntries;
    }

    private void loadDifficulties() {
        ConfigurationSection difficultySection = plugin.getConfig().getConfigurationSection("difficulties");

        if (difficultySection == null) return;

        for (String difficultyId : difficultySection.getKeys(false)) { // difficulty id si ZOR KOLAY
            ConfigurationSection difficultyInfoSection = difficultySection.getConfigurationSection(difficultyId);
            if (difficultyInfoSection == null) continue;

            String id = difficultyId.toUpperCase(Locale.ROOT);
            Component name = difficultyInfoSection.getString("name") != null
                    ? MMUtil.deserialize(difficultyInfoSection.getString("name"))
                    : MMUtil.formatColors("<f>INVALID DIFFICULTY");
            Map<Integer, Wave> waves = this.loadWaves(difficultyInfoSection.getConfigurationSection("waves"));

            if (waves == null || waves.isEmpty()) {
                warn("waves empty for difficulty " + id);
                continue;
            }

            List<Integer> arenaIds = difficultyInfoSection.getIntegerList("arenas");
            if (arenaIds.isEmpty())
                warn("Zorluk " + id + ": arenas listesi boş, bu zorlukla zindan başlatılamaz.");

            difficulties.put(id, new Difficulty(id, name, waves, this.loadLoot(difficultyInfoSection.getMapList("loot")), arenaIds));
        }
    }


    public Arena getArena(int id) {
        return arenas.get(id);
    }

    public List<Arena> getArenas() {
        return List.copyOf(arenas.values());
    }

    /** Config'teki tüm arena anahtarları — eksik ayarlı (yüklenemeyen) arenalar dahil. */
    public List<String> getArenaKeys() {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("arenas");
        return section == null ? List.of() : List.copyOf(section.getKeys(false));
    }

    public Mob getMob(String id) {
        return mobs.get(id.toUpperCase(Locale.ROOT));
    }

    public Difficulty getDifficulty(String id) {
        return difficulties.get(id.toUpperCase(Locale.ROOT));
    }

    public List<String> getDifficulties() {
        return List.copyOf(difficulties.keySet());
    }

    public Map<String, Mob> getMobs() { return Map.copyOf(mobs); }

    public String getPlaceholderText(String key) {
        return placeholderText.getOrDefault(key, "");
    }

    private Location toLocation(World world, String raw) {
        if (raw == null) return null;
        String[] parts = raw.split(",");
        if (parts.length != 3) return null;
        return new Location(
                world,
                Double.parseDouble(parts[0].trim()),
                Double.parseDouble(parts[1].trim()),
                Double.parseDouble(parts[2].trim())
        );
    }

    private void warn(String message) {
        Logger.warn(message);
    }

    public record Arena(int id, Location playerSpawn, Location respawnLocation, List<Location> mobSpawns) {
    }

    public record Mob(String id, Component name, EntityType type, List<LootEntry> loot, double health, double damage, double scale, double movementSpeedMultiplier) {
    }

    public record Wave(int number, Map<String, Integer> mobs) {
    }

    public record Difficulty(String id, Component name, Map<Integer, Wave> waves, List<LootEntry> loot, List<Integer> arenas) {
    }

    public record LootEntry(ItemStack item, double chance, int min, int max) {
    }
}