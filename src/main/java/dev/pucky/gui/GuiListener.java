package dev.pucky.gui;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public class GuiListener implements Listener {

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        // getInventory() üstteki envanteri verir, menü açıksa o bizim Gui'miz
        if (!(event.getInventory().getHolder(false) instanceof Gui gui)) return;

        // shift-click, sayı tuşu, çift tık toplama dahil her şeyi iptal et
        event.setCancelled(true);

        // oyuncu kendi envanterine tıkladıysa butona gönderme
        if (event.getClickedInventory() != event.getInventory()) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        gui.handleClick(new GuiClick(player, event.getSlot(), event.getClick(), gui));
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder(false) instanceof Gui)
            event.setCancelled(true);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder(false) instanceof Gui gui)) return;
        if (!(event.getPlayer() instanceof Player player)) return;

        gui.handleClose(player);

        // sadece oyuncu ESC'ye bastıysa geri dön. başka menü açılınca (OPEN_NEW)
        // ya da kod kapatınca (PLUGIN) dönersek menüler birbirini açıp döngüye girer
        if (event.getReason() == InventoryCloseEvent.Reason.PLAYER && gui.getBackAction() != null)
            Bukkit.getScheduler().runTask(PuckyGui.getPlugin(), () -> {
                if (player.isOnline()) gui.getBackAction().accept(player);
            });
    }
}
