package dev.pucky.gui;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class PuckyGui {
    private static JavaPlugin plugin;

    private PuckyGui() {}

    // plugin açılırken bir kez çağrılır
    public static void init(JavaPlugin javaPlugin) {
        if (plugin != null) return;
        plugin = javaPlugin;
        Bukkit.getPluginManager().registerEvents(new GuiListener(), javaPlugin);
        Bukkit.getPluginManager().registerEvents(new ChatInput(), javaPlugin);
    }

    // plugin kapanırken açık menüleri kapatır, yoksa listener gidince eşyalar alınabilir
    public static void closeAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getOpenInventory().getTopInventory().getHolder(false) instanceof Gui)
                player.closeInventory();
        }
    }

    public static JavaPlugin getPlugin() {
        if (plugin == null) throw new IllegalStateException("PuckyGui.init(plugin) çağrılmadı.");
        return plugin;
    }
}
