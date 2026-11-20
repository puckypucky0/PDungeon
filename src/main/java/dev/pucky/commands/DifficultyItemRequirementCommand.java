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

public class DifficultyItemRequirementCommand implements MinecraftCommand {

    public ConfigManager configManager;

    public DifficultyItemRequirementCommand(ConfigManager configManager) {
        this.configManager = configManager;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;

        if (args.length < 2 || args.length > 4 || !List.of("add", "set", "list", "remove").contains(args[0])) {
            Component appropriateUseComponent = MMUtil.formatColorsPrefixed("<n>Komutun doğru kullanımı: ")
                    .appendNewline()
                    .append(MMUtil.formatColors("<v>/pdungeon difficulty-item-requirement <n>add <v><zorluk> <n><tüketilsin mi>")).appendNewline()
                    .append(MMUtil.formatColors("<v>/pdungeon difficulty-item-requirement <n>set <v><zorluk> <n><sıra numarası> <v><tüketilsin mi>")).appendNewline()
                    .append(MMUtil.formatColors("<v>/pdungeon difficulty-item-requirement <n>list <v><zorluk>")).appendNewline()
                    .append(MMUtil.formatColors("<v>/pdungeon difficulty-item-requirement <n>remove <v><zorluk> <n><sıra numarası>")).appendNewline();
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
        List<ConfigManager.ItemRequirement> itemRequirements = difficulty.requirements().itemRequirements();
        String path = "difficulties." + difficulty.id() + ".requirements.items";

        switch (args[0]) {
            case "list" -> { // pd difficulty-item-requirement list ZOR
                Component listComponent = MMUtil.formatColorsPrefixed("<v>" + args[1] + " <n>zorluğunda toplamda <v>" + itemRequirements.size() + " gerekli eşya <n>var.")
                        .appendNewline();

                int order = 1;
                for (ConfigManager.ItemRequirement itemRequirement : itemRequirements) {
                    Component component = MMUtil.deserialize("<#406e4c>• " + order + ". eşya: <reset>");

                    ItemStack item;
                    try {
                        item = CommandUtils.decode(itemRequirement.base64());
                    } catch (Exception e) {
                        item = null;
                    }

                    if (item == null) {
                        component = component.append(MMUtil.formatColors("<f>Geçersiz eşya kodu."));
                    } else {
                        component = component.append(item.displayName())
                                .append(MMUtil.formatColors(
                                        " <v>[<n>" + item.getType().name() + " x" + item.getAmount() + "<v>] <n>" + (itemRequirement.consume() ? "Bu eşya zindan başında <s>tüketilir." : "Bu eşya zindan başında <f>tüketilmez.")
                                ));
                    }

                    listComponent = listComponent.append(component).appendNewline();
                    order++;
                }

                player.sendMessage(listComponent);
            }
            case "add" -> { // pd difficulty-item-requirement add ZOR true
                if (args.length != 3 || !List.of("true", "false").contains(args[2])) {
                    player.sendMessage(MMUtil.formatColorsPrefixed(
                            "<n>Doğru kullanım: <v>/pdungeon difficulty-item-requirement <n>add <v><zorluk> <n><eşya girişte yok olsun mu>"
                    ));
                    return;
                }
                boolean consume = Boolean.parseBoolean(args[2]);

                if (player.getInventory().getItemInMainHand().getType().equals(Material.AIR)) {
                    player.sendMessage(MMUtil.formatColorsPrefixed(
                            "<n>Gerekli eşya olarak eklenecek eşyayı <v>elinizde tutmalısınız."
                    ));
                    return;
                }

                ItemStack itemStack = player.getInventory().getItemInMainHand();
                String base64 = CommandUtils.encode(itemStack);

                List<Map<?, ?>> currentMapList = PDungeon.getInstance().getConfig().getMapList(path);
                currentMapList.add(
                        Map.of(
                                "code", base64,
                                "consume", consume
                        )
                );
                PDungeon.getInstance().getConfig().set(path, currentMapList);
                PDungeon.getInstance().saveConfig();
                configManager.load();

                player.sendMessage(MMUtil.formatColorsPrefixed("<n>Elinizdeki eşya <v>" + difficulty.id() + " <n>zorluğuna gerekli eşya olarak <s>başarıyla eklendi."));
            }
            case "set" -> { // pd difficulty-item-requirement set ZOR 1 true
                if (args.length != 4 || !List.of("true", "false").contains(args[3])) {
                    player.sendMessage(MMUtil.formatColorsPrefixed(
                            "<n>Doğru kullanım: <v>/pdungeon difficulty-item-requirement <n>set <v><zorluk> <n><sıra numarası> <v><true|false>"
                    ));
                    return;
                }

                if (CommandUtils.parseIntOrNull(args[2]) == null) {
                    player.sendMessage(MMUtil.formatColorsPrefixed("<f>Geçersiz sıra numarası değeri."));
                    return;
                }
                int index = CommandUtils.parseIntOrNull(args[2]);

                if (index < 1 || itemRequirements.size() < index) {
                    player.sendMessage(MMUtil.formatColorsPrefixed(
                            "<n>Bu zorlukta şuan <v>" + itemRequirements.size() + " <n>gerekli eşya var. Dolayısıyla yeni bir gerekli eşya eklemek için " +
                                    "<v>\"add\" <n>kullanmalı veya kayıtları değiştirmek için <v>" + itemRequirements.size() + ". sıradaki kayıttan <n>daha önce gelen bir kayıt belirtmelisiniz."
                    ));
                    return;
                }
                boolean consume = Boolean.parseBoolean(args[3]);

                if (player.getInventory().getItemInMainHand().getType().equals(Material.AIR)) {
                    player.sendMessage(MMUtil.formatColorsPrefixed(
                            "<n>Gerekli eşya olarak eklenecek eşyayı <v>elinizde tutmalısınız."
                    ));
                    return;
                }
                ItemStack itemStack = player.getInventory().getItemInMainHand();
                String base64 = CommandUtils.encode(itemStack);

                List<Map<?, ?>> currentMapList = PDungeon.getInstance().getConfig().getMapList(path);
                currentMapList.set(index - 1,
                        Map.of(
                                "code", base64,
                                "consume", consume
                        )
                );
                PDungeon.getInstance().getConfig().set(path, currentMapList);
                PDungeon.getInstance().saveConfig();
                configManager.load();

                player.sendMessage(MMUtil.formatColorsPrefixed("<v>" + index + " <n>sıra numaralı gerekli eşya elinizdeki eşya ile <s>değiştirildi."));
            }
            case "remove" -> { // pd difficulty-item-requirement remove ZOR 1
                if (args.length != 3) {
                    player.sendMessage(MMUtil.formatColorsPrefixed(
                            "<n>Doğru kullanım: <v>/pdungeon difficulty-item-requirement <n>remove <v><zorluk> <n><sıra numarası>"
                    ));
                    return;
                }

                if (CommandUtils.parseIntOrNull(args[2]) == null) {
                    player.sendMessage(MMUtil.formatColorsPrefixed("<f>Geçersiz sıra numarası değeri."));
                    return;
                }
                int index = CommandUtils.parseIntOrNull(args[2]);

                if (index < 1 || itemRequirements.size() < index) {
                    player.sendMessage(MMUtil.formatColorsPrefixed(
                            "<n>Bu zorlukta şuan <v>" + itemRequirements.size() + " <n>gerekli eşya var. Lütfen <v>1 <n>ile <v>" + itemRequirements.size() + " <n>arasında bir sıra numarası girin."
                    ));
                    return;
                }

                List<Map<?, ?>> currentMapList = PDungeon.getInstance().getConfig().getMapList(path);
                currentMapList.remove(index - 1);
                PDungeon.getInstance().getConfig().set(path, currentMapList);
                PDungeon.getInstance().saveConfig();
                configManager.load();

                player.sendMessage(MMUtil.formatColorsPrefixed("<v>" + index + " <n>sıra numaralı gerekli eşya bu zorluktan kaldırıldı."));
            }
        }
    }

    @Override
    public MinecraftCommandMetadata metadata() {
        return MinecraftCommandMetadata.of(
                "difficulty-item-requirement",
                "Set some item requirement per dungeon difficulty."
        )
                .permission("op") // /pd difficulty-item-requirement set ZOR 1 true
                .playerOnly()
                .tabCompletes(
                        TabCompleteEntry.of(0, "add", "set", "list", "remove"),
                        TabCompleteEntry.of(1, ctx -> configManager.getDifficulties()),
                        TabCompleteEntry.of(2, ctx -> { // set/remove → sıra numarası, add → tüketilsin mi
                            if (Objects.equals(ctx.argAt(0), "set")
                            || Objects.equals(ctx.argAt(0), "remove")) {
                                ConfigManager.Difficulty tabDifficulty = configManager.getDifficulty(ctx.argAt(1));
                                if (tabDifficulty == null) return List.of();
                                int count = tabDifficulty.requirements().itemRequirements().size();

                                List<String> suggestions = new ArrayList<>();

                                for (int i = 1; i <= count; i++) {
                                    suggestions.add(String.valueOf(i));
                                }

                                return suggestions;
                            } else if (Objects.equals(ctx.argAt(0), "add"))
                                return List.of("true", "false");
                            else return List.of();
                        }),
                        TabCompleteEntry.of(3, ctx -> Objects.equals(ctx.argAt(0), "set") ? List.of("true", "false") : List.of())
                );
    }
}
