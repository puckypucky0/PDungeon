package dev.pucky.gui;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

// sayfalı menü: üst satırlar eşyalar, en alt satır gezinme
// alt satır: [0] önceki sayfa, [4] geri (back verildiyse), [8] sonraki sayfa
public class PaginatedGui extends Gui {
    private final List<GuiButton> items = new ArrayList<>();
    private final int pageSize;
    private ItemStack navFiller = ItemBuilder.of(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();
    private int page = 0;

    // rows 2-6, son satır gezinmeye ayrılır
    public PaginatedGui(String title, int rows) {
        super(title, Math.clamp(rows, 2, 6));
        this.pageSize = getSize() - 9;
    }

    // slot vermiyorsun, eşyalar sırayla yerleşir
    public PaginatedGui addItem(GuiButton button) {
        items.add(button);
        return this;
    }

    public PaginatedGui addItem(ItemStack item, Consumer<GuiClick> action) {
        return addItem(GuiButton.of(item, action));
    }

    public PaginatedGui clearItems() {
        items.clear();
        page = 0;
        return this;
    }

    // alt satırın boş kısımlarını dolduran eşya, null verirsen boş kalır
    public PaginatedGui navFiller(ItemStack item) {
        this.navFiller = item;
        return this;
    }

    // sayfalar 0'dan başlar
    public int getPage() {
        return page;
    }

    public int getMaxPage() {
        return Math.max(0, (items.size() - 1) / pageSize);
    }

    @Override
    public void open(Player player) {
        open(player, page);
    }

    // silme gibi işlemlerden sonra aynı sayfaya dönmek için: gui.open(player, eskiGui.getPage())
    public void open(Player player, int page) {
        this.page = Math.clamp(page, 0, getMaxPage());
        render();
        super.open(player);
    }

    // açık menünün içeriğini değiştirir, menüyü kapatıp açmaya gerek yok
    private void render() {
        clear();

        int start = page * pageSize;
        for (int i = 0; i < pageSize && start + i < items.size(); i++)
            setButton(i, items.get(start + i));

        int nav = pageSize; // alt satırın ilk slotu
        String pageInfo = " <dark_gray>(" + (page + 1) + "/" + (getMaxPage() + 1) + ")";

        if (page > 0)
            setButton(nav, ItemBuilder.of(Material.ARROW).name("<yellow>Önceki sayfa" + pageInfo).build(), click -> {
                page--;
                render();
            });

        if (page < getMaxPage())
            setButton(nav + 8, ItemBuilder.of(Material.ARROW).name("<yellow>Sonraki sayfa" + pageInfo).build(), click -> {
                page++;
                render();
            });

        if (getBackAction() != null) backButton(nav + 4);

        if (navFiller != null)
            for (int slot = nav; slot < getSize(); slot++)
                if (getInventory().getItem(slot) == null) setItem(slot, navFiller);
    }
}
