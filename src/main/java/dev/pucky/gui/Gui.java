package dev.pucky.gui;

import dev.pucky.util.MMUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class Gui implements InventoryHolder {
    private final Inventory inventory;
    private final Map<Integer, GuiButton> buttons = new HashMap<>();
    private Consumer<Player> closeAction;
    private Consumer<Player> backAction;

    // title MiniMessage formatında, rows 1-6
    public Gui(String title, int rows) {
        this.inventory = Bukkit.createInventory(
                this,
                Math.clamp(rows, 1, 6) * 9,
                MMUtil.formatColors(title)
        );
    }

    public Gui setButton(int slot, GuiButton button) {
        // menü dışına taşan slotları sessizce atla, envanter hata fırlatmasın
        if (slot < 0 || slot >= inventory.getSize()) return this;
        buttons.put(slot, button);
        inventory.setItem(slot, button.item());
        return this;
    }

    public Gui setButton(int slot, ItemStack item, Consumer<GuiClick> action) {
        return setButton(slot, GuiButton.of(item, action));
    }

    public Gui setItem(int slot, ItemStack item) {
        return setButton(slot, GuiButton.display(item));
    }

    public Gui removeButton(int slot) {
        buttons.remove(slot);
        inventory.setItem(slot, null);
        return this;
    }

    // boş slotları doldurur (cam panel vs.)
    public Gui fill(ItemStack item) {
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (!buttons.containsKey(slot)) setItem(slot, item);
        }
        return this;
    }

    // menüyü yeniden çizmeden önce her şeyi siler
    public Gui clear() {
        buttons.clear();
        inventory.clear();
        return this;
    }

    public Gui onClose(Consumer<Player> closeAction) {
        this.closeAction = closeAction;
        return this;
    }

    // ESC ile kapatılınca çalışır. menü her seferinde güncel veriyle kurulsun diye Gui değil metot veriyoruz
    // örnek: gui.back(p -> openDifficulty(p, id));
    public Gui back(Consumer<Player> backAction) {
        this.backAction = backAction;
        return this;
    }

    // back(...) ile verilen menüye dönen ok butonu koyar
    public Gui backButton(int slot) {
        return setButton(slot, ItemBuilder.of(Material.ARROW).name("<gray>Geri").build(), click -> {
            if (backAction != null) backAction.accept(click.player());
        });
    }

    // tıklama olayının içinden menü açmak güvenli değil, bu yüzden hep bir tick sonra açılır
    public void open(Player player) {
        Bukkit.getScheduler().runTask(PuckyGui.getPlugin(), () -> {
            if (player.isOnline()) player.openInventory(inventory);
        });
    }

    public int getSize() {
        return inventory.getSize();
    }

    void handleClick(GuiClick click) {
        GuiButton button = buttons.get(click.slot());
        if (button != null && button.action() != null)
            button.action().accept(click);
    }

    void handleClose(Player player) {
        if (closeAction != null) closeAction.accept(player);
    }

    Consumer<Player> getBackAction() {
        return backAction;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
