package dev.pucky.commands;

import dev.pucky.PDungeon;
import dev.pucky.commands.util.MinecraftCommand;
import dev.pucky.commands.util.MinecraftCommandMetadata;
import dev.pucky.commands.util.tab.TabCompleteEntry;
import dev.pucky.config.ConfigManager;
import dev.pucky.util.CommandUtils;
import dev.pucky.util.MMUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class DifficultyLootCommand implements MinecraftCommand {

    public ConfigManager configManager;

    public DifficultyLootCommand(ConfigManager configManager) {
        this.configManager = configManager;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;

        if (args.length < 2 || args.length > 6 || !List.of("add", "set", "list", "remove").contains(args[0])) {
            Component appropriateUseComponent = MMUtil.formatColorsPrefixed("<n>Komutun doğru kullanımı: ")
                    .appendNewline()
                    .append(MMUtil.formatColors("<v>/pdungeon difficulty-loot <n>add <v><zorluk> <n><düşme ihtimali> <v><en az eşya sayısı> <n><en fazla eşya sayısı>")).appendNewline()
                    .append(MMUtil.formatColors("<v>/pdungeon difficulty-loot <n>set <v><zorluk> <n><sıra numarası> <v><düşme ihtimali> <n><en az eşya sayısı> <v><en fazla eşya sayısı>")).appendNewline()
                    .append(MMUtil.formatColors("<v>/pdungeon difficulty-loot <n>list <v><zorluk>")).appendNewline()
                    .append(MMUtil.formatColors("<v>/pdungeon difficulty-loot <n>remove <v><zorluk> <n><sıra numarası>")).appendNewline();
            player.sendMessage(appropriateUseComponent);
            return;
        }

        if (configManager.getDifficulty(args[1]) == null) {
            player.sendMessage(MMUtil.formatColorsPrefixed(
                    "<f>Geçersiz zorluk."
            ));
            return;
        }
        ConfigManager.Difficulty difficulty = configManager.getDifficulty(args[1]);

        switch (args[0]) {
            case "list" -> { // pd loot list ZOR
                Component lootListComponent = MMUtil.formatColorsPrefixed("<v>" + args[1] + " <n>zorluğunda toplamda <v>" + difficulty.loot().size() + " ganimet eşyası <n>var.")
                        .appendNewline();

                int order = 1;
                for (ConfigManager.LootEntry lootEntry : difficulty.loot()) {
                    Component component = MMUtil.deserialize("<#406e4c>• " + order + ". eşya: <reset>")
                            .append(lootEntry.item().displayName())
                            .append(MMUtil.formatColors(
                                    " <v>[<n>" + lootEntry.item().getType().name() + "<v>] %<n>"
                                            + lootEntry.chance() + " şans, <v>[<n>" + lootEntry.min() + ", " + lootEntry.max() + "<v>] <n>arasında çıkabilir."
                            ))
                            .appendNewline();
                    lootListComponent = lootListComponent.append(component);
                    order++;
                }

                player.sendMessage(lootListComponent);
                return;
            }
            case "add" -> { // pd loot add ZOR chance min max
                if (args.length != 5) {
                    player.sendMessage(MMUtil.formatColorsPrefixed(
                            "<n>Doğru kullanım: <v>/pdungeon difficulty-loot <n>add <v><zorluk> <n><düşme ihtimali> <v><en az eşya sayısı> <n><en fazla eşya sayısı>"
                    ));
                    return;
                }

                if (CommandUtils.parseDoubleOrNull(args[2]) == null) {
                    player.sendMessage(MMUtil.formatColorsPrefixed("<f>Geçersiz düşme ihtimali değeri."));
                    return;
                }

                if (CommandUtils.parseIntOrNull(args[3]) == null) {
                    player.sendMessage(MMUtil.formatColorsPrefixed("<f>Geçersiz en az eşya sayısı değeri."));
                    return;
                }

                if (CommandUtils.parseIntOrNull(args[4]) == null) {
                    player.sendMessage(MMUtil.formatColorsPrefixed("<f>Geçersiz en fazla eşya sayısı değeri."));
                    return;
                }

                if (player.getInventory().getItemInMainHand().getType().equals(Material.AIR)) {
                    player.sendMessage(MMUtil.formatColorsPrefixed(
                            "<n>Ganimet olarak eklenecek eşyayı <v>elinizde tutmalısınız."
                    ));
                    return;
                }

                ItemStack itemStack = player.getInventory().getItemInMainHand();
                String base64 = CommandUtils.encode(itemStack);
                double chance = CommandUtils.parseDoubleOrNull(args[2]);
                int min = CommandUtils.parseIntOrNull(args[3]);
                int max = CommandUtils.parseIntOrNull(args[4]);

                if (max < 1) {
                    player.sendMessage(MMUtil.formatColorsPrefixed("<n>En fazla eşya sayısı <v>en az 1 <n>olmalı."));
                    return;
                }

                List<Map<?, ?>> currentMapList = PDungeon.getInstance().getConfig().getMapList("difficulties." + difficulty.id() + ".loot");
                currentMapList.add(
                        Map.of(
                                "item", base64,
                                "chance", Math.clamp(chance, 0, 100),
                                "min", Math.clamp(min, 1, max),
                                "max", max
                        )
                );
                PDungeon.getInstance().getConfig().set("difficulties." + difficulty.id() + ".loot", currentMapList);
                PDungeon.getInstance().saveConfig();
                configManager.load();

                player.sendMessage(MMUtil.formatColorsPrefixed("<n>Elinizdeki eşya <v>" + difficulty.id() + " <n>zorluğuna <s>başarıyla eklendi."));

            }
            case "set" -> { // pd loot set ZOR index chance min max
                if (args.length != 6) {
                    player.sendMessage(MMUtil.formatColorsPrefixed(
                            "<n>Doğru kullanım: <v>/pdungeon difficulty-loot set <n><zorluk> <v><sıra numarası> <n><düşme ihtimali> <v><en az eşya sayısı> <n><en fazla eşya sayısı>"
                    ));
                    return;
                }

                if (CommandUtils.parseIntOrNull(args[2]) == null) {
                    player.sendMessage(MMUtil.formatColorsPrefixed("<f>Geçersiz sıra numarası değeri."));
                    return;
                }
                int lootIndex = CommandUtils.parseIntOrNull(args[2]);

                if (lootIndex < 1 || difficulty.loot().size() < lootIndex) {
                    player.sendMessage(MMUtil.formatColorsPrefixed(
                            "<n>Bu zorlukta şuan <v>" + difficulty.loot().size() + " <n>ganimet girdisi var. Dolayısıyla yeni bir ganimet kaydı eklemek için " +
                                    "<v>\"add\" <n>kullanmalı veya kayıtları değiştirmek için <v>" + difficulty.loot().size() + ". sıradaki ganimet kaydından <n>daha önce gelen bir kayıt belirtmelisiniz."
                    ));
                    return;
                }

                if (CommandUtils.parseDoubleOrNull(args[3]) == null) {
                    player.sendMessage(MMUtil.formatColorsPrefixed("<f>Geçersiz düşme ihtimali değeri."));
                    return;
                }
                double chance = CommandUtils.parseDoubleOrNull(args[3]);

                if (CommandUtils.parseIntOrNull(args[4]) == null) {
                    player.sendMessage(MMUtil.formatColorsPrefixed("<f>Geçersiz en az eşya sayısı değeri."));
                    return;
                }
                int min = CommandUtils.parseIntOrNull(args[4]);


                if (CommandUtils.parseIntOrNull(args[5]) == null) {
                    player.sendMessage(MMUtil.formatColorsPrefixed("<f>Geçersiz en fazla eşya sayısı değeri."));
                    return;
                }
                int max = CommandUtils.parseIntOrNull(args[5]);


                if (player.getInventory().getItemInMainHand().getType().equals(Material.AIR)) {
                    player.sendMessage(MMUtil.formatColorsPrefixed(
                            "<n>Ganimet olarak eklenecek eşyayı <v>elinizde tutmalısınız."
                    ));
                    return;
                }
                ItemStack itemStack = player.getInventory().getItemInMainHand();
                String base64 = CommandUtils.encode(itemStack);

                if (max < 1) {
                    player.sendMessage(MMUtil.formatColorsPrefixed("<n>En fazla eşya sayısı <v>en az 1 <n>olmalı."));
                    return;
                }

                List<Map<?, ?>> currentMapList = PDungeon.getInstance().getConfig().getMapList("difficulties." + difficulty.id() + ".loot");
                currentMapList.set(lootIndex - 1,
                        Map.of(
                                "item", base64,
                                "chance", Math.clamp(chance, 0, 100),
                                "min", Math.clamp(min, 1, max),
                                "max", max
                        )
                );
                PDungeon.getInstance().getConfig().set("difficulties." + difficulty.id() + ".loot", currentMapList);
                PDungeon.getInstance().saveConfig();
                configManager.load();

                player.sendMessage(MMUtil.formatColorsPrefixed("<v>" + lootIndex + " <n>sıra numaralı ganimet elinizdeki eşya ile <s>değiştirildi."));
            }
            case "remove" -> { // pd loot remove ZOR 1
                if (args.length != 3) {
                    player.sendMessage(MMUtil.formatColorsPrefixed(
                            "<n>Doğru kullanım: <v>/pdungeon difficulty-loot <n>remove <v><zorluk> <n><sıra numarası>"
                    ));
                    return;
                }

                if (CommandUtils.parseIntOrNull(args[2]) == null) {
                    player.sendMessage(MMUtil.formatColorsPrefixed("<f>Geçersiz sıra numarası değeri."));
                    return;
                }
                int lootIndex = CommandUtils.parseIntOrNull(args[2]);

                if (lootIndex < 1 || difficulty.loot().size() < lootIndex) {
                    player.sendMessage(MMUtil.formatColorsPrefixed(
                            "<n>Bu zorlukta şuan <v>" + difficulty.loot().size() + " <n>ganimet girdisi var. Lütfen <v>1 <n>ile <v>" + difficulty.loot().size() + " <n>arasında bir sıra numarası girin."
                    ));
                    return;
                }

                List<Map<?, ?>> currentMapList = PDungeon.getInstance().getConfig().getMapList("difficulties." + difficulty.id() + ".loot");
                currentMapList.remove(lootIndex - 1);
                PDungeon.getInstance().getConfig().set("difficulties." + difficulty.id() + ".loot", currentMapList);
                PDungeon.getInstance().saveConfig();
                configManager.load();

                player.sendMessage(MMUtil.formatColorsPrefixed("<v>" + lootIndex + " <n>sıra numaralı ganimet bu zorluktan kaldırıldı."));
            }
        }


    }

    @Override
    public MinecraftCommandMetadata metadata() {
        return MinecraftCommandMetadata.of(
                "difficulty-loot",
                "Set some loot per dungeon difficulty."
        )
                .permission("op")
                .playerOnly()
                .tabCompletes(
                        TabCompleteEntry.of(0, "add", "set", "list", "remove"),
                        TabCompleteEntry.of(1, ctx -> configManager.getDifficulties()),
                        TabCompleteEntry.of(2, ctx -> { // set/remove → sıra numarası, add → şans
                            if (Objects.equals(ctx.argAt(0), "set")
                            || Objects.equals(ctx.argAt(0), "remove")) {
                                ConfigManager.Difficulty tabDifficulty = configManager.getDifficulty(ctx.argAt(1));
                                if (tabDifficulty == null) return List.of();
                                int lootCount = tabDifficulty.loot().size();

                                List<String> suggestions = new ArrayList<>();

                                for (int i = 1; i <= lootCount; i++) {
                                    suggestions.add(String.valueOf(i));
                                }

                                return suggestions;
                            } else if (Objects.equals(ctx.argAt(0), "add")) // pd loot add ZOR 0.5 1 2
                                return List.of("0.5", "1", "5", "10", "25", "50", "100");
                            else return List.of();
                        })
                );
    }
}
