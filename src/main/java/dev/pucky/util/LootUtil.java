package dev.pucky.util;

import dev.pucky.config.ConfigManager;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nullable;
import java.util.concurrent.ThreadLocalRandom;

public class LootUtil {

    public static @Nullable ItemStack roll(ConfigManager.LootEntry entry) {
        if (ThreadLocalRandom.current().nextDouble(100) >= entry.chance())
            return null;

        int amount = ThreadLocalRandom.current().nextInt(entry.min(), entry.max() + 1);
        ItemStack item = entry.item().clone();
        item.setAmount(amount);
        return item;
    }
}
