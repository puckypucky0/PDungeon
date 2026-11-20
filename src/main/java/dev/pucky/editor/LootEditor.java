package dev.pucky.editor;

import dev.pucky.PDungeon;
import dev.pucky.config.ConfigManager;
import dev.pucky.gui.ChatInput;
import dev.pucky.gui.Gui;
import dev.pucky.gui.ItemBuilder;
import dev.pucky.gui.PaginatedGui;
import dev.pucky.util.CommandUtils;
import dev.pucky.util.EditorUtil;
import dev.pucky.util.MMUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LootEditor {
    public static void openLootMenu(Player player, String id, int page) {
        ConfigManager configManager = PDungeon.getInstance().getConfigManager();

        ConfigManager.Difficulty difficulty = configManager.getDifficulty(id);
        if (difficulty == null) return;

        PaginatedGui gui = new PaginatedGui(MMUtil.serialize(difficulty.name()) + " ganimetleri", 6);
        gui.back(p -> DifficultyEditor.openDifficulty(p, id));

        for (int i = 0; i < difficulty.loot().size(); i++) {
            ConfigManager.LootEntry lootEntry = difficulty.loot().get(i);
            ItemStack itemStack = lootEntry.item().clone();
            ItemStack originalItemStack = lootEntry.item().clone();
            List<Component> lore;

            if (itemStack.lore() == null)
                lore = new ArrayList<>();
            else lore = itemStack.lore();

            lore.add(MMUtil.formatColors("<!i><n>Sol tıkla: <v>Değerleri düzenle"));
            lore.add(MMUtil.formatColors("<!i><n>Sağ tıkla: <v>Envanterine al"));
            lore.add(MMUtil.formatColors("<!i><n>Shift + sağ tıkla: <v>Ganimeti kaldır"));
            lore.add(MMUtil.EMPTY_COMPONENT);
            lore.add(MMUtil.formatColors("<!i><n>Sıra: <v>" + (i + 1) + ". eşya"));
            lore.add(MMUtil.formatColors("<!i><n>Düşme şansı: <v>%" + lootEntry.chance()));
            lore.add(MMUtil.formatColors("<!i><n>En az düşme sayısı: <v>" + lootEntry.min()));
            lore.add(MMUtil.formatColors("<!i><n>En fazla düşme sayısı: <v>" + lootEntry.max()));

            itemStack.lore(lore);

            int finalI = i;
            // slot değil listedeki sıra: 2. sayfanın ilk eşyası 45. sıradır
            gui.addItem(itemStack,
                    click -> {
                        if (click.isRight()) {
                            if (click.isShift()) {
                                EditorUtil.removeDifficultyLootEntry(difficulty, finalI);
                                click.player().playSound(click.player(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1f, 1.4f);

                                // aynı sayfada kal. son sayfadaki son eşya silinirse open() bir önceki sayfaya düşer
                                openLootMenu(click.player(), id, gui.getPage());
                            } else {
                                click.player().getInventory().addItem(originalItemStack).forEach((index, item) -> {
                                    click.player().getWorld().dropItemNaturally(click.player().getLocation(), item);
                                    click.player().sendMessage(MMUtil.formatColorsPrefixed("<n>Eşya yere düştü."));
                                });
                            }
                        }

                        if (click.isLeft()) {
                            openLootEntryEditMenu(click.player(), id, finalI, gui.getPage());
                        }
                    }
            );
        }

        gui.open(player, page);
    }

    // page: ganimet menüsünde hangi sayfadan gelindiği, geri dönerken oraya açılır
    public static void openLootEntryEditMenu(Player player, String difficultyId, int index, int page) {
        ConfigManager.LootEntry entry = PDungeon.getInstance().getConfigManager().getDifficulty(difficultyId).loot().get(index);
        Gui gui = new Gui("Ganimeti düzenle", 3);

        gui.setItem(4, entry.item());

        dropChanceButton(gui, entry, difficultyId, index, player, page);
        minDropButton(gui, entry, difficultyId, index, player, page);
        maxDropButton(gui, entry, difficultyId, index, player, page);

        gui.setButton(18,
                ItemBuilder.of(Material.ARROW)
                        .name("<gray>Geri")
                        .build(),
                click -> {
                    openLootMenu(click.player(), difficultyId, page);
        });

        gui.fill(ItemBuilder.of(Material.GRAY_STAINED_GLASS_PANE).name(" ").build());
        gui.open(player);
    }

    // editLootEntry("ZOR", 0, "chance", 25.0)
    public static boolean editLootEntry(String difficultyId, int index, String key, Object value) {
        String path = "difficulties." + difficultyId + ".loot";
        List<Map<?, ?>> lootList = PDungeon.getInstance().getConfig().getMapList(path);
        if (index < 0 || index >= lootList.size()) return false;

        // config'ten gelen map değiştirilemez olabilir, kopyasıyla çalış
        Map<Object, Object> entry = new HashMap<>(lootList.get(index));
        entry.put(key, value);
        lootList.set(index, entry);

        EditorUtil.set(path, lootList);
        return true;
    }

    private static void dropChanceButton(Gui gui, ConfigManager.LootEntry entry, String difficultyId, int index, Player player, int page) {
        gui.setButton(12,
                ItemBuilder.of(Material.EMERALD)
                        .name("<green>Düşme şansı: <b>" + entry.chance())
                        .lore("<gray>Sol tıkla: <yellow>Sohbetten yeni bir değer gir")
                        .glow()
                        .build(),
                click -> {
                    if (click.isLeft()) {
                        ChatInput.ask(click.player(),
                                "<yellow>Bu eşyanın yüzdelik düşme değerini sohbete yaz. (Örneğin: 0.5, 1, 5) <gray>(vazgeçmek için: iptal)",
                                value -> {
                                    Double chance = CommandUtils.parseDoubleOrNull(value);
                                    if (chance == null || chance < 0) {
                                        click.player().sendMessage(MMUtil.formatColorsPrefixed("<f>Geçersiz düşme ihtimali: <v>" + value));
                                    } else {
                                        editLootEntry(difficultyId, index, "chance", Math.min(chance, 100));
                                        click.player().playSound(click.player().getLocation(), Sound.BLOCK_NOTE_BLOCK_BANJO, 1, 1);
                                    }
                                    openLootEntryEditMenu(player, difficultyId, index, page);
                                }, () -> {
                                    openLootEntryEditMenu(click.player(), difficultyId, index, page);
                                    click.player().playSound(click.player().getLocation(), Sound.BLOCK_NOTE_BLOCK_BANJO, 1, 0);
                                }
                        );
                    }
                });
    }

    private static void minDropButton(Gui gui, ConfigManager.LootEntry entry, String difficultyId, int index, Player player, int page) {
        gui.setButton(13,
                ItemBuilder.of(Material.EMERALD)
                        .name("<green>En az düşme sayısı: <b>" + entry.min())
                        .lore("<gray>Sol tıkla: <yellow>Sohbetten yeni bir değer gir")
                        .glow()
                        .build(),
                click -> {
                    if (click.isLeft()) {
                        ChatInput.ask(click.player(),
                                "<yellow>Bu eşyanın en az düşme sayısı değerini sohbete yaz. (Örneğin: 1, 5) <gray>(vazgeçmek için: iptal)",
                                value -> {
                                    Integer min = CommandUtils.parseIntOrNull(value);
                                    if (min == null || min < 0) {
                                        click.player().sendMessage(MMUtil.formatColorsPrefixed("<f>Geçersiz en az düşme sayısı: <v>" + value));
                                    } else if (min > entry.max()) {
                                        click.player().sendMessage(MMUtil.formatColorsPrefixed("<f>Geçersiz en az düşme sayısı (en az düşme en fazla düşmeden [" + entry.max() + "] fazla olamaz) : <v>" + value));
                                    } else {
                                        editLootEntry(difficultyId, index, "min", min);
                                        click.player().playSound(click.player().getLocation(), Sound.BLOCK_NOTE_BLOCK_BANJO, 1, 1);
                                    }
                                    openLootEntryEditMenu(player, difficultyId, index, page);
                                }, () -> {
                                    openLootEntryEditMenu(click.player(), difficultyId, index, page);
                                    click.player().playSound(click.player().getLocation(), Sound.BLOCK_NOTE_BLOCK_BANJO, 1, 0);
                                }
                        );
                    }
                });
    }

    private static void maxDropButton(Gui gui, ConfigManager.LootEntry entry, String difficultyId, int index, Player player, int page) {
        gui.setButton(14,
                ItemBuilder.of(Material.EMERALD)
                        .name("<green>En fazla düşme sayısı: <b>" + entry.max())
                        .lore("<gray>Sol tıkla: <yellow>Sohbetten yeni bir değer gir")
                        .glow()
                        .build(),
                click -> {
                    if (click.isLeft()) {
                        ChatInput.ask(click.player(),
                                "<yellow>Bu eşyanın en fazla düşme sayısı değerini sohbete yaz. (Örneğin: 15, 25) <gray>(vazgeçmek için: iptal)",
                                value -> {
                                    Integer max = CommandUtils.parseIntOrNull(value);
                                    if (max == null || max <= 0) {
                                        click.player().sendMessage(MMUtil.formatColorsPrefixed("<f>Geçersiz en fazla düşme sayısı: <v>" + value));
                                    } else if (entry.min() > max) {
                                        click.player().sendMessage(MMUtil.formatColorsPrefixed("<f>Geçersiz en fazla düşme sayısı (en az düşme en az düşmeden [" + entry.min() + "] az olamaz) : <v>" + value));
                                    } else {
                                        editLootEntry(difficultyId, index, "max", max);
                                        click.player().playSound(click.player().getLocation(), Sound.BLOCK_NOTE_BLOCK_BANJO, 1, 1);
                                    }
                                    openLootEntryEditMenu(player, difficultyId, index, page);
                                }, () -> {
                                    openLootEntryEditMenu(click.player(), difficultyId, index, page);
                                    click.player().playSound(click.player().getLocation(), Sound.BLOCK_NOTE_BLOCK_BANJO, 1, 0);
                                }
                        );
                    }
                });
    }


}
