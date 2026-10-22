package dev.pucky.commands;

import dev.pucky.PDungeon;
import dev.pucky.commands.util.MinecraftCommand;
import dev.pucky.commands.util.MinecraftCommandMetadata;
import dev.pucky.commands.util.tab.TabCompleteEntry;
import dev.pucky.config.ConfigManager;
import dev.pucky.util.CommandUtils;
import dev.pucky.util.MMUtil;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;

public class PlayerSpawnLocationCommand implements MinecraftCommand {

    public ConfigManager configManager;

    public PlayerSpawnLocationCommand(ConfigManager configManager) {
        this.configManager = configManager;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        FileConfiguration config = PDungeon.getInstance().getConfig();
        ConfigurationSection section = config.getConfigurationSection("arenas");

        if (args.length != 5) { // /pdungeon player-spawn-location 1 world -20 30 -450
            sender.sendMessage(MMUtil.formatColorsPrefixed(
                    "<n>Lütfen komutu <v>/pdungeon player-spawn-location <n>\"<v>arena numarası <n>dünya ismi <v>X Y Z<n>\" şeklinde kullanın."
            ));
            return;
        }

        if (!CommandUtils.checkIntegerArgs(args, 0, 2, 3, 4).isEmpty()) {
            sender.sendMessage(MMUtil.formatColorsPrefixed(
                    "<n>Lütfen komutu arena numarası, X, Y ve Z değerlerinin hepsini <v>geçerli bir sayısal değer ile değiştirip <n>tekrar çalıştırın."
            ));
            return;
        }

        if (section == null)
            section = config.createSection("arenas");

        Location location = CommandUtils.parseLocationFromArgs(args, 1, 2, 3, 4);

        if (location == null) {
            sender.sendMessage(MMUtil.formatColorsPrefixed(
                    "<n>Belirttiğiniz <v>dünya veya koordinatlar <n>geçersiz."
            ));
            return;
        }

        Block block = location.getBlock();
        boolean isSafe = !block.isLiquid() && block.isPassable();

        if (!isSafe) {
            sender.sendMessage(MMUtil.formatColorsPrefixed(
                    "<n>Belirttiğiniz konumdaki blok <v>(" + block.getType() + ") <n>oyuncuların doğması için <v>güvenli bir blok değil."
            ));
            return;
        }

        String arenaWorld = section.getString(args[0] + ".world");
        if (arenaWorld != null && !arenaWorld.equals(location.getWorld().getName())) {
            sender.sendMessage(MMUtil.formatColorsPrefixed(
                    "<n>Bu arena <v>" + arenaWorld + " <n>dünyasında. Farklı bir dünyada <v>konum belirleyemezsiniz."
            ));
            return;
        }

        section.set(args[0] + ".world", location.getWorld().getName());
        section.set(args[0] + ".player-spawn", CommandUtils.stringifyLocation(location));
        PDungeon.instance.saveConfig();
        configManager.load();

        sender.sendMessage(MMUtil.formatColorsPrefixed(
                "<n>Belirttiğiniz lokasyon <v>(" + CommandUtils.stringifyLocation(location) + ") <n>oyuncuların doğacağı lokasyon olarak belirlendi."
        ));
        // /pdungeon player-spawn-location 1 world x y z
    }

    @Override
    public MinecraftCommandMetadata metadata() {
        return MinecraftCommandMetadata.of(
                        "player-spawn-location",
                        "Sets the spawn location of the players for a dungeon arena."
                )
                .permission("op")
                .playerOnly()
                .tabCompletes(
                        TabCompleteEntry.of(0, ctx -> configManager.getArenaKeys()),

                        TabCompleteEntry.ofPlayer(1, p -> List.of(p.getWorld().getName())), // World name
                        TabCompleteEntry.ofPlayer(2, p -> List.of(String.valueOf(p.getLocation().getBlockX()))), // X
                        TabCompleteEntry.ofPlayer(3, p -> List.of(String.valueOf(p.getLocation().getBlockY()))), // Y
                        TabCompleteEntry.ofPlayer(4, p -> List.of(String.valueOf(p.getLocation().getBlockZ()))) // Z
                );
    }
}
