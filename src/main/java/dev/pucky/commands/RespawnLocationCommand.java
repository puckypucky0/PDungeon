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
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;

public class RespawnLocationCommand implements MinecraftCommand {

    public ConfigManager configManager;

    public RespawnLocationCommand(ConfigManager configManager) {
        this.configManager = configManager;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        FileConfiguration config = PDungeon.getInstance().getConfig();

        if (args.length != 4) {
            sender.sendMessage(MMUtil.formatColorsPrefixed(
                    "<n>Lütfen komutu <v>/pdungeon respawn-location <n>\"<v>dünya ismi <n>X Y Z<n>\" şeklinde kullanın."
            ));
            return;
        }

        if (!CommandUtils.checkIntegerArgs(args, 1, 2, 3).isEmpty()) {
            sender.sendMessage(MMUtil.formatColorsPrefixed(
                    "<n>Lütfen komutu X, Y ve Z değerlerinin hepsini <v>geçerli bir sayısal değer ile değiştirip <n>tekrar çalıştırın."
            ));
            return;
        }

        Location location = CommandUtils.parseLocationFromArgs(args, 0, 1, 2, 3);

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

        config.set("settings.respawn-location.world", location.getWorld().getName());
        config.set("settings.respawn-location.position", CommandUtils.stringifyLocation(location));
        PDungeon.instance.saveConfig();
        configManager.load();

        sender.sendMessage(MMUtil.formatColorsPrefixed(
                "<n>Belirttiğiniz lokasyon <v>(" + CommandUtils.stringifyLocation(location) + ") <n>oyuncuların yeniden doğacağı lokasyon olarak belirlendi."
        ));
    }

    @Override
    public MinecraftCommandMetadata metadata() {
        return MinecraftCommandMetadata.of(
                        "respawn-location",
                        "Sets the spawn location of the players for every dungeon arena."
                )
                .permission("op")
                .playerOnly()
                .tabCompletes(

                        TabCompleteEntry.ofPlayer(0, p -> List.of(p.getWorld().getName())), // World name
                        TabCompleteEntry.ofPlayer(1, p -> List.of(String.valueOf(p.getLocation().getBlockX()))), // X
                        TabCompleteEntry.ofPlayer(2, p -> List.of(String.valueOf(p.getLocation().getBlockY()))), // Y
                        TabCompleteEntry.ofPlayer(3, p -> List.of(String.valueOf(p.getLocation().getBlockZ()))) // Z
                );
    }
}
