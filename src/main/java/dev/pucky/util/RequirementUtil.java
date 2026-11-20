package dev.pucky.util;

import dev.pucky.config.ConfigManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class RequirementUtil {
    // gereklilikler karşılanıyorsa null, karşılanmıyorsa eksikleri listeleyen mesaj döner
    public static Component checkRequirements(ConfigManager.Difficulty difficulty, Player player) {
        ConfigManager.Requirements requirements = difficulty.requirements();
        List<Component> missing = new ArrayList<>();

        int playtimeMinutes = player.getStatistic(Statistic.PLAY_ONE_MINUTE) / (20 * 60);
        if (playtimeMinutes < requirements.playtimeMinutes()) {
            missing.add(MMUtil.formatColors(
                    " <gray>| <v>" + requirements.playtimeMinutes() + " dakika <n>oynama süresi <gray>(" + playtimeMinutes + ")"
            ));
        }

        for (ConfigManager.ItemRequirement itemRequirement : requirements.itemRequirements()) {
            ItemStack item;
            try {
                item = CommandUtils.decode(itemRequirement.base64());
            } catch (Exception e) {
                continue;
            }

            if (!player.getInventory().containsAtLeast(item, item.getAmount())) {
                missing.add(MMUtil.formatColors(" <gray>| <v>" + item.getAmount() + "x ")
                        .append(item.displayName()));
            }
        }

        if (missing.isEmpty()) return null;

        Component message = MMUtil.formatColors("<n>Bu zindana girmek için <f>gereklilikleri karşılamıyorsunuz.");
        for (Component line : missing) {
            message = message.appendNewline().append(line);
        }
        return message;
    }
}
