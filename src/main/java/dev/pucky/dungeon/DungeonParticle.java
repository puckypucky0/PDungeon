package dev.pucky.dungeon;

import lombok.Builder;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.bukkit.Location;
import org.bukkit.Particle;

@Builder @Getter @Accessors(fluent = true)
public class DungeonParticle {
    private Particle particleType;
    private double particleMultiplier;
    private double offsetX;
    private double offsetY;
    private double offsetZ;
    private double offsetExtra;

    public void spawn(Location location) {
        location.getWorld().spawnParticle(
                this.particleType,
                location,
                Math.toIntExact((long) (25 * this.particleMultiplier)),
                this.offsetX,
                this.offsetY,
                this.offsetZ,
                this.offsetExtra
        );
    }
}
