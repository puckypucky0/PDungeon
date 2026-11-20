package dev.pucky.editor;

import dev.pucky.PDungeon;
import dev.pucky.config.ConfigManager;
import dev.pucky.dungeon.DungeonParticle;
import dev.pucky.gui.ChatInput;
import dev.pucky.gui.Gui;
import dev.pucky.gui.ItemBuilder;
import dev.pucky.util.EditorUtil;
import dev.pucky.util.MMUtil;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class SettingsEditor {
    public static void settingsMenu(Player player) {
        FileConfiguration config = PDungeon.getInstance().getConfig();
        ConfigManager configManager = PDungeon.getInstance().getConfigManager();
        Gui gui = new Gui("<s><b>Eklenti ayarları", 4);

        int maxPlayers = configManager.getMaxPlayers();
        int waveDelay = configManager.getWaveDelay();
        int spawnInterval = configManager.getSpawnInterval();

        String respawnWorldName = config.getString("settings.respawn-location.world", "");
        World respawnWorld = Bukkit.getWorld(respawnWorldName);
        String respawnWorldString = respawnWorld == null
                ? "<b>" + respawnWorldName + " <f>(geçersiz dünya)"
                : "<b>" + respawnWorldName + " <s>(geçerli dünya)";

        String rawRespawnLocation = config.getString("settings.respawn-location.position", null);
        Location respawnLocation = configManager.toLocation(respawnWorld, rawRespawnLocation);
        String respawnLocationString = respawnLocation == null
                ? "<b><f>(geçersiz lokasyon)"
                : "<b>" + rawRespawnLocation;

        ItemStack maxPlayersOption = ItemBuilder.of(Material.TRIPWIRE_HOOK)
                .name("<yellow>Zindan başına en fazla oyuncu sayısı: <b>" + maxPlayers + " oyuncu")
                .lore("<n>Sol tıkla: <v>Değeri 1 arttır",
                        "<n>Sağ tıkla: <v>Değeri 1 azalt")
                .build();

        ItemStack waveDelayOption = ItemBuilder.of(Material.CLOCK)
                .name("<yellow>Dalgalar arası bekleme süresi: <b>" + waveDelay + " saniye")
                .lore("<n>Sol tıkla: <v>Değeri 1 saniye arttır",
                        "<n>Sağ tıkla: <v>Değeri 1 saniye azalt")
                .build();

        ItemStack spawnIntervalOption = ItemBuilder.of(Material.COMPASS)
                .name("<yellow>Yaratık doğma arası bekleme tick'i: <b>" + spawnInterval + " tick")
                .lore("<n>Sol tıkla: <v>Değeri 1 tick arttır",
                        "<n>Sağ tıkla: <v>Değeri 1 tick azalt")
                .build();

        ItemStack respawnWorldOption = ItemBuilder.of(Material.GRASS_BLOCK)
                .name("<yellow>Zindandan çıkış lokasyonu dünyası: " + respawnWorldString)
                .lore("<n>Sol tıkla: <v>Değeri sohbetten değiştir",
                        "<n>Sağ tıkla: <v>Bu dünyanın başlangıç noktasına ışınlan")
                .build();

        ItemStack respawnLocationOption = ItemBuilder.of(Material.RED_BANNER)
                .name("<yellow>Zindandan çıkış lokasyonu: " + respawnLocationString)
                .lore("<n>Sol tıkla: <v>Lokasyonu sohbetten olduğun yer ile değiştir",
                        "<n>Sağ tıkla: <v>Bu lokasyona ışınlan")
                .build();

        maxPlayersPerArenaOptionButton(gui, maxPlayersOption, player, maxPlayers);
        waveDelayOptionButton(gui, waveDelayOption, player, waveDelay);
        spawnIntervalTickOption(gui, spawnIntervalOption, player, spawnInterval);
        respawnWorldOptionButton(gui, respawnWorldOption, player, respawnWorld);
        respawnLocationPositionButton(gui, respawnLocationOption, player, respawnLocation);
        particleButtons(gui);

        // ESC ve sol alttaki "Geri" zorluk listesine döner
        gui.back(DifficultyEditor::openDifficultyList);
        gui.backButton(27);

        gui.fill(ItemBuilder.of(Material.GRAY_STAINED_GLASS_PANE).name(" ").build());

        gui.open(player);
    }

    public static void maxPlayersPerArenaOptionButton(Gui gui, ItemStack maxPlayersOption, Player player, int maxPlayers) {
        gui.setButton(10, maxPlayersOption,
                click -> {
                    if (click.isLeft()) {
                        EditorUtil.set("settings.max-players-per-arena", maxPlayers + 1);
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BANJO, 1, 1);
                        settingsMenu(player);
                    }

                    if (click.isRight()) {
                        if (maxPlayers <= 1) {
                            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BANJO, 1, 0);
                        } else {
                            EditorUtil.set("settings.max-players-per-arena", maxPlayers - 1);
                            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BANJO, 1, 1);
                            settingsMenu(player);
                        }

                    }
                }
        );
    }

    public static void waveDelayOptionButton(Gui gui, ItemStack waveDelayOption, Player player, int waveDelay) {
        gui.setButton(11, waveDelayOption,
            click -> {
                if (click.isLeft()) {
                    EditorUtil.set("settings.wave-delay-seconds", waveDelay + 1);
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BANJO, 1, 1);
                    settingsMenu(player);
                }

                if (click.isRight()) {
                    if (waveDelay <= 1) {
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BANJO, 1, 0);
                    } else {
                        EditorUtil.set("settings.wave-delay-seconds", waveDelay - 1);
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BANJO, 1, 1);
                        settingsMenu(player);
                    }
                }
            }
        );
    }

    public static void spawnIntervalTickOption(Gui gui, ItemStack spawnIntervalOption, Player player, int spawnInterval) {
        gui.setButton(12, spawnIntervalOption,
                click -> {
                    if (click.isLeft()) {
                        EditorUtil.set("settings.spawn-interval-tick", spawnInterval + 1);
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BANJO, 1, 1);
                        settingsMenu(player);
                    }

                    if (click.isRight()) {
                        if (spawnInterval <= 1) {
                            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BANJO, 1, 0);
                        } else {
                            EditorUtil.set("settings.spawn-interval-tick", spawnInterval - 1);
                            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BANJO, 1, 1);
                            settingsMenu(player);
                        }

                    }
                }
        );
    }

    public static void respawnWorldOptionButton(Gui gui, ItemStack respawnWorldOption, Player player, World respawnWorld) {
        gui.setButton(13, respawnWorldOption,
                click -> {
                    if (click.isLeft()) {
                        ChatInput.ask(click.player(),
                                "<yellow>Yeniden doğma lokasyonunun dünyasını yaz. <gray>(vazgeçmek için: iptal)",
                                worldName -> {
                                    if (Bukkit.getWorld(worldName) == null) {
                                        player.sendMessage(MMUtil.formatColorsPrefixed("<v>" + worldName + " <f>adında yüklü bir dünya yok."));
                                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BANJO, 1, 0);
                                        settingsMenu(player);
                                    } else {
                                        EditorUtil.set("settings.respawn-location.world", worldName);
                                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BANJO, 1, 1);
                                        settingsMenu(player);
                                    }
                                }, () -> settingsMenu(player)
                        );
                    }

                    if (click.isRight()) {
                        if (respawnWorld == null) {
                            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BANJO, 1, 0);
                        } else {
                            click.player().teleport(respawnWorld.getSpawnLocation());
                            settingsMenu(player);
                        }
                    }
                }
        );
    }

    public static void respawnLocationPositionButton(Gui gui, ItemStack respawnWorldOption, Player player, Location respawnLocation) {
        gui.setButton(14, respawnWorldOption,
                click -> {
                    if (click.isLeft()) {
                        ChatInput.ask(click.player(),
                                "<yellow>Yeniden doğma lokasyonunu üstünde durduğun blok olarak ayarlamak için <b>ayarla</b> işlemi iptal etmek için <b>iptal</b> yaz.",
                                prompt -> {
                                    if (!List.of("ayarla", "iptal").contains(prompt.toLowerCase())) {
                                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BANJO, 1, 0);
                                        settingsMenu(player);
                                    }

                                    if (prompt.equalsIgnoreCase("ayarla")) {
                                        Location location = click.player().getLocation();
                                        String value = String.format("%s, %s, %s",
                                                location.getBlockX(),
                                                location.getBlockY(),
                                                location.getBlockZ()
                                        );
                                        EditorUtil.set("settings.respawn-location.world", location.getWorld().getName());
                                        EditorUtil.set("settings.respawn-location.position", value);
                                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BANJO, 1, 1);
                                        settingsMenu(player);
                                    }
                                }, () -> settingsMenu(player)
                        );
                    }

                    if (click.isRight()) {
                        if (respawnLocation == null) {
                            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BANJO, 1, 0);
                        } else {
                            click.player().teleport(respawnLocation);
                            settingsMenu(player);
                        }
                    }
                }
        );
    }

    public static void particleButtons(Gui gui) {
        ConfigManager configManager = PDungeon.getInstance().getConfigManager();

        DungeonParticle mobSpawnParticle = configManager.getMobSpawnParticle();
        DungeonParticle mobDeathParticle = configManager.getMobDeathParticle();
        DungeonParticle playerDeathParticle = configManager.getPlayerDeathParticle();

        ItemStack mobSpawnParticleOption = ItemBuilder.of(Material.BLAZE_POWDER)
                .name("<#db7900>Zindan yaratığı doğma partikülü: <b>" + mobSpawnParticle.particleType().name())
                .lore("<n>Sol tıkla: <v>Bu partikülü simüle et",
                        "<f>Bu ayar yalnızca config dosyasından düzenlenebilir.")
                .build();

        ItemStack mobDeathParticleOption = ItemBuilder.of(Material.BLAZE_POWDER)
                .name("<#db7900>Zindan yaratığı ölme partikülü: <b>" + mobDeathParticle.particleType().name())
                .lore("<n>Sol tıkla: <v>Bu partikülü simüle et",
                        "<f>Bu ayar yalnızca config dosyasından düzenlenebilir.")
                .build();

        ItemStack playerDeathParticleOption = ItemBuilder.of(Material.BLAZE_POWDER)
                .name("<#db7900>Zindan oyuncusu ölme partikülü: <b>" + playerDeathParticle.particleType().name())
                .lore("<n>Sol tıkla: <v>Bu partikülü simüle et",
                        "<f>Bu ayar yalnızca config dosyasından düzenlenebilir.")
                .build();

        gui.setButton(15, mobDeathParticleOption,
                click -> {
                    if (click.isLeft()) {
                        EditorUtil.simulateDungeonParticle(click.player(), mobDeathParticle, click.gui());
                    }
                }
        ).setButton(16, mobSpawnParticleOption,
                click -> {
                    if (click.isLeft()) {
                        EditorUtil.simulateDungeonParticle(click.player(), mobSpawnParticle, click.gui());
                    }
                }
        ).setButton(21, playerDeathParticleOption,
                click -> {
                    if (click.isLeft()) {
                        EditorUtil.simulateDungeonParticle(click.player(), playerDeathParticle, click.gui());
                    }
                }
        );
    }
}
