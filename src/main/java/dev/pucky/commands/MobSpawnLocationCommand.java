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

import java.util.ArrayList;
import java.util.List;

public class MobSpawnLocationCommand implements MinecraftCommand {

    public ConfigManager configManager;

    public MobSpawnLocationCommand(ConfigManager configManager) {
        this.configManager = configManager;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        FileConfiguration config = PDungeon.getInstance().getConfig();
        ConfigurationSection section = config.getConfigurationSection("arenas");

        if (args.length != 6) { // /pdungeon mob-spawn-location 1 1 world -20 30 -450
            sender.sendMessage(MMUtil.formatColorsPrefixed(
                    "<n>Lütfen komutu <v>/pdungeon mob-spawn-location <n>\"<v>arena numarası <v>koordinat sıra numarası <n>dünya ismi <v>X Y Z<n>\" şeklinde kullanın."
            ));
            return;
        }

        if (!CommandUtils.checkIntegerArgs(args, 0, 3, 4, 5).isEmpty() ||
            (!args[1].equalsIgnoreCase("add") && CommandUtils.parseIntOrNull(args[1]) == null)) {
            sender.sendMessage(MMUtil.formatColorsPrefixed(
                    "<n>Lütfen komutu arena numarası, X, Y ve Z değerlerinin hepsini <v>geçerli bir sayısal değer ile değiştirip <n>tekrar çalıştırın. <v>Koordinat sıra numarası <n>yerine <v>\"add\" <n>yazabilirsiniz."
            ));
            return;
        }

        if (section == null)
            section = config.createSection("arenas");

        Location location = CommandUtils.parseLocationFromArgs(args, 2, 3, 4, 5);

        if (location == null) {
            sender.sendMessage(MMUtil.formatColorsPrefixed(
                    "<n>Belirttiğiniz <v>dünya veya koordinatlar <n>geçersiz."
            ));
            return;
        }

        Block block = location.getBlock();
        boolean isSafe = !block.isLiquid() &&
                block.isPassable();

        if (!isSafe) {
            sender.sendMessage(MMUtil.formatColorsPrefixed(
                    "<n>Belirttiğiniz konumdaki blok <v>(" + block.getType() + ") <n>yaratıkların doğması için <v>güvenli bir blok değil."
            ));
            return;
        }
        // /pdungeon mob-spawn-location 1 1 world x y z

        String arenaWorld = section.getString(args[0] + ".world");
        if (arenaWorld != null && !arenaWorld.equals(location.getWorld().getName())) {
            sender.sendMessage(MMUtil.formatColorsPrefixed(
                    "<n>Bu arena <v>" + arenaWorld + " <n>dünyasında. Farklı bir dünyada <v>konum belirleyemezsiniz."
            ));
            return;
        }

        List<String> spawns = section.getStringList(args[0] + ".mob-spawns");
        String value = CommandUtils.stringifyLocation(location);
        int spawnNumber;

        if (args[1].equalsIgnoreCase("add")) {
            spawns.add(value);
            spawnNumber = spawns.size();
        } else {
            Integer index = CommandUtils.parseIntOrNull(args[1]);
            if (index == null || index < 1 || index > spawns.size()) {
                sender.sendMessage(MMUtil.deserialize("<red>Geçersiz spawn numarası."));
                return;
            }
            spawns.set(index - 1, value);
            spawnNumber = index;
        }

        section.set(args[0] + ".world", location.getWorld().getName());
        section.set(args[0] + ".mob-spawns", spawns);
        PDungeon.instance.saveConfig();
        configManager.load();

        sender.sendMessage(MMUtil.formatColorsPrefixed(
                "<n>Belirttiğiniz konum <v>(" + value + ") <n>mobların doğması için kullanılacak noktalardan <v>" + spawnNumber + ". <n>nokta olarak belirlendi."
        ));
    }

    @Override
    public MinecraftCommandMetadata metadata() {
        return MinecraftCommandMetadata.of(
                "mob-spawn-location",
                "Sets location for the dungeon mobs spawn points."
        )
            .permission("op")
            .playerOnly()
                .tabCompletes(
                        TabCompleteEntry.of(0, ctx -> configManager.getArenaKeys()),

                        TabCompleteEntry.of(1, ctx -> { // Which mob spawn
                            if (CommandUtils.parseIntOrNull(ctx.argAt(0)) == null) return List.of();

                            int spawnCount = PDungeon.getInstance().getConfig()
                                    .getStringList("arenas." + ctx.argAt(0) + ".mob-spawns").size();

                            List<String> suggestions = new ArrayList<>();
                            suggestions.add("add");

                            for (int i = 1; i <= spawnCount; i++) {
                                suggestions.add(String.valueOf(i));
                            }

                            return suggestions;
                        }),
                        TabCompleteEntry.ofPlayer(2, p -> List.of(p.getWorld().getName())), // World name
                        TabCompleteEntry.ofPlayer(3, p -> List.of(String.valueOf(p.getLocation().getBlockX()))), // X
                        TabCompleteEntry.ofPlayer(4, p -> List.of(String.valueOf(p.getLocation().getBlockY()))), // Y
                        TabCompleteEntry.ofPlayer(5, p -> List.of(String.valueOf(p.getLocation().getBlockZ()))) // Z
                );
    }
}
