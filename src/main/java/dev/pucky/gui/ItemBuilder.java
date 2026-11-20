package dev.pucky.gui;

import dev.pucky.util.MMUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ItemBuilder {
    private final ItemStack item;

    private ItemBuilder(ItemStack item) {
        this.item = item;
    }

    public static ItemBuilder of(Material material) {
        return new ItemBuilder(new ItemStack(material));
    }

    // var olan eşyayı bozmamak için kopyasıyla çalışır
    public static ItemBuilder of(ItemStack item) {
        return new ItemBuilder(item.clone());
    }

    public ItemBuilder name(String name) {
        item.editMeta(meta -> meta.displayName(text(name)));
        return this;
    }

    public ItemBuilder lore(String... lines) {
        return lore(List.of(lines));
    }

    public ItemBuilder lore(List<String> lines) {
        List<Component> lore = new ArrayList<>();
        for (String line : lines) lore.add(text(line));

        item.editMeta(meta -> meta.lore(lore));
        return this;
    }

    public ItemBuilder amount(int amount) {
        item.setAmount(Math.clamp(amount, 1, item.getMaxStackSize()));
        return this;
    }

    public ItemBuilder glow() {
        item.editMeta(meta -> meta.setEnchantmentGlintOverride(true));
        return this;
    }

    // enchant, attribute gibi alt yazıları gizler
    public ItemBuilder hideFlags() {
        item.editMeta(meta -> meta.addItemFlags(ItemFlag.values()));
        return this;
    }

    public ItemStack build() {
        return item.clone();
    }

    // eşya isimleri varsayılan olarak italik gelir, onu kapatıyoruz
    private static Component text(String mm) {
        return MMUtil.formatColors(mm).decoration(TextDecoration.ITALIC, false);
    }
}
