package dev.pucky.gui;

import org.bukkit.inventory.ItemStack;

import java.util.function.Consumer;

// action null ise sadece görüntü, tıklanınca bir şey olmaz
public record GuiButton(ItemStack item, Consumer<GuiClick> action) {

    public static GuiButton of(ItemStack item, Consumer<GuiClick> action) {
        return new GuiButton(item, action);
    }

    public static GuiButton display(ItemStack item) {
        return new GuiButton(item, null);
    }
}
