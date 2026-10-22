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

public class MobLootCommand implements MinecraftCommand {

    public ConfigManager configManager;

    public MobLootCommand(ConfigManager configManager) {
        this.configManager = configManager;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;

        if (args.length < 2 || args.length > 6 || !List.of("add", "set", "list", "remove").contains(args[0])) {
            Component appropriateUseComponent = MMUtil.formatColorsPrefixed("<n>Komutun doğru kullanımı: ")
                    .appendNewline()
                    .append(MMUtil.formatColors("<v>/pdungeon mob-loot <n>add <v><mob_key> <n><düşme ihtimali> <v><en az eşya sayısı> <n><en fazla eşya sayısı>")).appendNewline()
                    .append(MMUtil.formatColors("<v>/pdungeon mob-loot <n>set <v><mob_key> <n><sıra numarası> <v><düşme ihtimali> <n><en az eşya sayısı> <v><en fazla eşya sayısı>")).appendNewline()
                    .append(MMUtil.formatColors("<v>/pdungeon mob-loot <n>list <v><mob_key>")).appendNewline()
                    .append(MMUtil.formatColors("<v>/pdungeon mob-loot <n>remove <v><mob_key> <n><sıra numarası>")).appendNewline();
            player.sendMessage(appropriateUseComponent);
            return;
        }

        if (configManager.getMob(args[1]) == null) {
            player.sendMessage(MMUtil.formatColorsPrefixed(
                    "<f>Geçersiz mob ID."
            ));
            return;
        }
        ConfigManager.Mob mob = configManager.getMob(args[1]);

        switch (args[0]) {
            case "list" -> { // pd loot list ZOR
                Component lootListComponent = MMUtil.formatColorsPrefixed("<v>" + args[1] + " <n>mobu için kayıtlı toplamda <v>" + mob.loot().size() + " ganimet eşyası <n>var.")
                        .appendNewline();

                int order = 1;
                for (ConfigManager.LootEntry lootEntry : mob.loot()) {
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
                            "<n>Doğru kullanım: <v>/pdungeon mob-loot <n>add <v><mob_key> <n><düşme ihtimali> <v><en az eşya sayısı> <n><en fazla eşya sayısı>"
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

                List<Map<?, ?>> currentMapList = PDungeon.getInstance().getConfig().getMapList("mobs." + mob.id() + ".loot");
                currentMapList.add(
                        Map.of(
                                "item", base64,
                                "chance", Math.clamp(chance, 0, 100),
                                "min", Math.clamp(min, 1, max),
                                "max", max
                        )
                );
                PDungeon.getInstance().getConfig().set("mobs." + mob.id() + ".loot", currentMapList);
                PDungeon.getInstance().saveConfig();
                configManager.load();

                player.sendMessage(MMUtil.formatColorsPrefixed("<n>Elinizdeki eşya <v>" + mob.id() + " <n>mobuna <s>başarıyla eklendi."));

            }
            case "set" -> { // pd loot set ZOR index chance min max
                if (args.length != 6) {
                    player.sendMessage(MMUtil.formatColorsPrefixed(
                            "<n>Doğru kullanım: <v>/pdungeon mob-loot set <n><mob_id> <v><sıra numarası> <n><düşme ihtimali> <v><en az eşya sayısı> <n><en fazla eşya sayısı>"
                    ));
                    return;
                }

                if (CommandUtils.parseIntOrNull(args[2]) == null) {
                    player.sendMessage(MMUtil.formatColorsPrefixed("<f>Geçersiz sıra numarası değeri."));
                    return;
                }
                int lootIndex = CommandUtils.parseIntOrNull(args[2]);

                if (lootIndex < 1 || mob.loot().size() < lootIndex) {
                    player.sendMessage(MMUtil.formatColorsPrefixed(
                            "<n>Şuan bu mob'a kayıtlı <v>" + mob.loot().size() + " <n>ganimet girdisi var. Dolayısıyla yeni bir ganimet kaydı eklemek için " +
                                    "<v>\"add\" <n>kullanmalı veya kayıtları değiştirmek için <v>" + mob.loot().size() + ". sıradaki ganimet kaydından <n>daha önce gelen bir kayıt belirtmelisiniz."
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

                List<Map<?, ?>> currentMapList = PDungeon.getInstance().getConfig().getMapList("mobs." + mob.id() + ".loot");
                currentMapList.set(lootIndex - 1,
                        Map.of(
                                "item", base64,
                                "chance", Math.clamp(chance, 0, 100),
                                "min", Math.clamp(min, 1, max),
                                "max", max
                        )
                );
                PDungeon.getInstance().getConfig().set("mobs." + mob.id() + ".loot", currentMapList);
                PDungeon.getInstance().saveConfig();
                configManager.load();

                player.sendMessage(MMUtil.formatColorsPrefixed("<v>" + lootIndex + " <n>sıra numaralı ganimet elinizdeki eşya ile <s>değiştirildi."));
            }
            case "remove" -> { // pd loot remove ZOR 1
                if (args.length != 3) {
                    player.sendMessage(MMUtil.formatColorsPrefixed(
                            "<n>Doğru kullanım: <v>/pdungeon mob-loot <n>remove <v><mob_key> <n><sıra numarası>"
                    ));
                    return;
                }

                if (CommandUtils.parseIntOrNull(args[2]) == null) {
                    player.sendMessage(MMUtil.formatColorsPrefixed("<f>Geçersiz sıra numarası değeri."));
                    return;
                }
                int lootIndex = CommandUtils.parseIntOrNull(args[2]);

                if (lootIndex < 1 || mob.loot().size() < lootIndex) {
                    player.sendMessage(MMUtil.formatColorsPrefixed(
                            "<n>Bu mob için şuan <v>" + mob.loot().size() + " <n>ganimet girdisi var. Lütfen <v>1 <n>ile <v>" + mob.loot().size() + " <n>arasında bir sıra numarası girin."
                    ));
                    return;
                }

                List<Map<?, ?>> currentMapList = PDungeon.getInstance().getConfig().getMapList("mobs." + mob.id() + ".loot");
                currentMapList.remove(lootIndex - 1);
                PDungeon.getInstance().getConfig().set("mobs." + mob.id() + ".loot", currentMapList);
                PDungeon.getInstance().saveConfig();
                configManager.load();

                player.sendMessage(MMUtil.formatColorsPrefixed("<v>" + lootIndex + " <n>sıra numaralı ganimet bu mobdan kaldırıldı."));
            }
        }


    }

    @Override
    public MinecraftCommandMetadata metadata() {
        return MinecraftCommandMetadata.of(
                        "mob-loot",
                        "Set some loot per dungeon mob."
                )
                .permission("op")
                .playerOnly()
                .tabCompletes(
                        TabCompleteEntry.of(0, "add", "set", "list", "remove"),
                        TabCompleteEntry.of(1, ctx -> configManager.getMobs().keySet()),
                        TabCompleteEntry.of(2, ctx -> { // set/remove → sıra numarası, add → şans
                            if (Objects.equals(ctx.argAt(0), "set")
                                    || Objects.equals(ctx.argAt(0), "remove")) {
                                ConfigManager.Mob tabMobID = configManager.getMob(ctx.argAt(1));
                                if (tabMobID == null) return List.of();
                                int lootCount = tabMobID.loot().size();

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
