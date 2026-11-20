package dev.pucky.util;

import dev.pucky.PDungeon;
import dev.pucky.config.ConfigManager;
import dev.pucky.dungeon.DungeonParticle;
import dev.pucky.gui.ChatInput;
import dev.pucky.gui.Gui;
import io.papermc.paper.entity.LookAnchor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.List;
import java.util.Map;


public class EditorUtil implements Listener {
    private static boolean dirty = false;

    // bellekteki config'i değiştirir ve record'ları yeniler, diske yazmaz
    public static void set(String path, Object value) {
        PDungeon.getInstance().getConfig().set(path, value);
        PDungeon.getInstance().getConfigManager().parse();
        dirty = true;
    }

    public static void flush() {
        if (!dirty) return;
        PDungeon.getInstance().saveConfig();
        dirty = false;
    }

    public static void removeDifficultyLootEntry(ConfigManager.Difficulty difficulty, int index) {
        String path = "difficulties." + difficulty.id() + ".loot";
        List<Map<?, ?>> currentMapList = PDungeon.getInstance().getConfig().getMapList(path);
        if (index < 0 || index >= currentMapList.size()) return;

        currentMapList.remove(index);
        set(path, currentMapList);
    }

    public static void simulateDungeonParticle(Player player, DungeonParticle particle, Gui gui) {
        player.closeInventory(InventoryCloseEvent.Reason.PLUGIN);
        Bukkit.getScheduler().runTaskLater(PDungeon.getInstance(), () -> {
            if (player.isOnline() && player.isValid()) {
                Location location = player.getLocation().add(2, 1, 0);
                player.lookAt(location, LookAnchor.EYES);

                particle.spawn(location);
            }
        }, 20L);

        Bukkit.getScheduler().runTaskLater(PDungeon.getInstance(), () -> {
            if (player.isOnline() && player.isValid())
                gui.open(player);
        }, 60L);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!dirty) return;
        if (!(event.getInventory().getHolder(false) instanceof Gui)) return;
        if (!(event.getPlayer() instanceof Player player)) return;

        // menüler arası geçişte de kapanma olur. bir tick beklemek gerekir:
        // hâlâ bir menü açıksa ya da sohbetten değer bekleniyorsa editörden çıkmamıştır
        Bukkit.getScheduler().runTask(PDungeon.getInstance(), () -> {
            if (player.isOnline() && player.getOpenInventory().getTopInventory().getHolder(false) instanceof Gui) return;
            if (player.isOnline() && ChatInput.isWaiting(player)) return;
            flush();
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        flush();
    }
}
