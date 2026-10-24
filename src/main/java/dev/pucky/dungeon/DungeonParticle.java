package dev.pucky.dungeon;

import lombok.Builder;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.bukkit.Location;
import org.bukkit.Particle;

@Builder @Getter @Accessors(fluent = true)
public class DungeonParticle {
    public Particle particleType;
    public double particleMultiplier;
    public double offsetX;
    public double offsetY;
    public double offsetZ;
    public double offsetExtra;

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
