package dev.pucky.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

// butona tıklanınca lambda'ya gelen bilgi
public record GuiClick(Player player, int slot, ClickType type, Gui gui) {

    public boolean isLeft() { return type.isLeftClick(); }

    public boolean isRight() { return type.isRightClick(); }

    public boolean isShift() { return type.isShiftClick(); }
}
