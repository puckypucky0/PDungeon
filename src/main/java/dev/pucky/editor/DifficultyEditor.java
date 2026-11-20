package dev.pucky.editor;

import dev.pucky.PDungeon;
import dev.pucky.config.ConfigManager;
import dev.pucky.gui.ChatInput;
import dev.pucky.gui.Gui;
import dev.pucky.gui.ItemBuilder;
import dev.pucky.util.CommandUtils;
import dev.pucky.util.EditorUtil;
import dev.pucky.util.MMUtil;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class DifficultyEditor {
    private static ItemStack difficultyInfoItem(ConfigManager.Difficulty difficulty) {
        return ItemBuilder.of(Material.IRON_SWORD)
                .name(MMUtil.serialize(difficulty.name()) + " <white>(" + difficulty.id() + ")")
                .lore(
                        "<gray>Cooldown: <yellow>" + difficulty.cooldown() + " dk",
                        "<gray>Dalga sayısı: <yellow>" + difficulty.waves().size(),
                        "",
                        "<green>Düzenlemek için tıkla"
                )
                .hideFlags()
                .build();
    }

    public static void openDifficultyList(Player player) {
        ConfigManager configManager = PDungeon.getInstance().getConfigManager();
        Gui gui = new Gui("<dark_gray>Zorluklar", 5);

        int slot = 10;
        for (String id : configManager.getDifficulties()) {
            ConfigManager.Difficulty difficulty = configManager.getDifficulty(id);

            gui.setButton(slot, difficultyInfoItem(difficulty),
                    click -> openDifficulty(click.player(), id)
            );

            slot++;
        }

        gui.setButton(8, ItemBuilder.of(Material.LEVER).name("<gray>Eklenti ayarları").build(),
                click -> {
                    SettingsEditor.settingsMenu(player);
                });

        // boş slotlar cam olsun
        gui.fill(ItemBuilder.of(Material.BLACK_STAINED_GLASS_PANE).name(" ").build());
        gui.open(player);
    }

    public static void openDifficulty(Player player, String id) {
        ConfigManager configManager = PDungeon.getInstance().getConfigManager();
        ConfigManager.Difficulty difficulty = configManager.getDifficulty(id);

        Gui gui = new Gui("<dark_gray>Zorluk: " + id, 4);

        // 2a) ChatInput örneği
        gui.setButton(10,
                ItemBuilder.of(Material.CLOCK)
                        .name("<yellow>Zindan giriş bekleme süresi: <b>" + difficulty.cooldown() + " dk")
                        .lore("<gray>Sol tık: <white>sohbetten yeni değer gir")
                        .build(),
                click -> ChatInput.ask(click.player(),
                        "<yellow>Yeni cooldown dakikasını yaz. <gray>(vazgeçmek için: iptal)",
                        value -> {
                            Integer minutes = CommandUtils.parseIntOrNull(value);
                            if (minutes == null || minutes < 0) {
                                click.player().sendMessage(MMUtil.formatColorsPrefixed("<f>Geçersiz sayı: <v>" + value));
                            } else {
                                EditorUtil.set("difficulties." + id + ".cooldown-minutes", minutes);
                                click.player().sendMessage(MMUtil.formatColorsPrefixed("<s>Cooldown <v>" + minutes + " dk <s>yapıldı."));
                            }
                            openDifficulty(click.player(), id);   // güncel değerle menüyü yeniden aç
                        },
                        () -> openDifficulty(click.player(), id)  // "iptal" yazarsa
                )
        );

        gui.setButton(11,
                ItemBuilder.of(Material.EMERALD)
                        .name("<green>Zindan ganimeti: <b>" + difficulty.loot().size() + " adet")
                        .lore("<gray>Sol tık: <white>Ganimetleri görüntüle")
                        .build(),
                click -> LootEditor.openLootMenu(click.player(), id, 0)
        );


        // 2c) Geri butonu
        gui.setButton(15,
                ItemBuilder.of(Material.ARROW).name("<gray>Geri").build(),
                click -> openDifficultyList(click.player())
        );

        // 2d) Sadece görüntü, tıklanınca bir şey olmaz
        gui.setItem(4, ItemBuilder.of(Material.BOOK)
                .name(MMUtil.serialize(difficulty.name()) + " (" + difficulty.id() + ")")
                .glow()
                .build()
        );

        gui.onClose(p -> p.playSound(p.getLocation(), Sound.UI_STONECUTTER_SELECT_RECIPE, 1, 1));
        gui.fill(ItemBuilder.of(Material.GRAY_STAINED_GLASS_PANE).name(" ").build());
        gui.open(player);
    }
}
