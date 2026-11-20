package dev.pucky.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.function.Consumer;

// "Emin misin?" menüsü: solda onayla, ortada bilgi, sağda iptal
public class ConfirmGui extends Gui {

    public ConfirmGui(String title, ItemStack info, Consumer<Player> onConfirm, Consumer<Player> onCancel) {
        super(title, 3);

        setButton(11, ItemBuilder.of(Material.LIME_CONCRETE).name("<green><bold>Onayla").build(),
                click -> finish(click.player(), onConfirm));

        setButton(15, ItemBuilder.of(Material.RED_CONCRETE).name("<red><bold>İptal").build(),
                click -> finish(click.player(), onCancel));

        if (info != null) setItem(13, info);

        fill(ItemBuilder.of(Material.GRAY_STAINED_GLASS_PANE).name(" ").build());
    }

    public ConfirmGui(String title, Consumer<Player> onConfirm, Consumer<Player> onCancel) {
        this(title, null, onConfirm, onCancel);
    }

    // önce menü kapanır, sonra karar çalışır; karar yeni bir menü açabilir
    private void finish(Player player, Consumer<Player> action) {
        Bukkit.getScheduler().runTask(PuckyGui.getPlugin(), () -> {
            player.closeInventory();
            if (action != null) action.accept(player);
        });
    }
}
