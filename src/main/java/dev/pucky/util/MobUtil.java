package dev.pucky.util;

import net.kyori.adventure.text.Component;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.TextDisplay;

public class MobUtil {
    public static void updateLabel(LivingEntity mob) {
        if (mob.customName() == null) return;

        double hp = mob.getHealth();
        double max = mob.getAttribute(Attribute.MAX_HEALTH).getValue();
        Component text = mob.customName().appendNewline()
                .append(MMUtil.deserialize("<red>❤ <white>" + (int) Math.ceil(hp) + "<gray>/" + (int) max));

        for (Entity p : mob.getPassengers())
            if (p instanceof TextDisplay d) d.text(text);
    }
}
