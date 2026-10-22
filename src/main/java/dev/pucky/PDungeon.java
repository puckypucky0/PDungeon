package dev.pucky;

import dev.pucky.commands.*;
import dev.pucky.commands.util.MinecraftCommandManager;
import dev.pucky.config.ConfigManager;
import dev.pucky.dungeon.DungeonInstance;
import dev.pucky.dungeon.DungeonListener;
import dev.pucky.dungeon.DungeonManager;
import dev.pucky.dungeon.DungeonStatus;
import dev.pucky.party.PartyListener;
import dev.pucky.placeholder.DungeonExpansion;
import io.papermc.paper.entity.TeleportFlag;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Getter
public final class PDungeon extends JavaPlugin {
    @Getter public static PDungeon instance;
    @Getter private ConfigManager configManager;
    @Getter private MinecraftCommandManager commandManager;

    @Override
    public void onEnable() {
        instance = this;

        configManager = new ConfigManager(this);
        configManager.load();

        commandManager = new MinecraftCommandManager(this, "pdungeon");
        commandManager.register(
                new MobSpawnLocationCommand(configManager),
                new PlayerSpawnLocationCommand(configManager),
                new PartyCommand(),
                new StartDungeonInstanceCommand(configManager),
                new ReloadCommand(configManager),
                new DifficultyLootCommand(configManager),
                new MobLootCommand(configManager),
                new RespawnLocationCommand(configManager),
                new LogsCommand(configManager)
        );

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI"))
            new DungeonExpansion().register();

        this.getServer().getPluginManager().registerEvents(new DungeonListener(), this);
        this.getServer().getPluginManager().registerEvents(new PartyListener(), this);


        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (DungeonInstance instance : DungeonManager.getAll().values()) {
                for (UUID aliveMob : List.copyOf(instance.getAliveMobs())) {
                    Entity mob = Bukkit.getEntity(aliveMob);
                    if (mob instanceof Mob m) {
                        Location spawn = instance.getArenaConfig().playerSpawn();

                        if (mob.getWorld() != spawn.getWorld() || mob.getLocation().distance(spawn) >= 30.0) {
                            List<Location> spawns = instance.getArenaConfig().mobSpawns();
                            Location loc = spawns.get(ThreadLocalRandom.current().nextInt(spawns.size()));

                            mob.teleport(loc, TeleportFlag.EntityState.RETAIN_PASSENGERS);
                            loc.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, loc, 25, 0.3, 0.6, 0.3, 0.02);
                            loc.getWorld().spawnParticle(Particle.PORTAL, loc, 60, 0.4, 0.8, 0.4, 0.5);
                        }

                        if (!(m.getTarget() instanceof Player p) || !instance.getPlayers().contains(p.getUniqueId())) {
                            List<UUID> players = List.copyOf(instance.getPlayers());
                            if (!players.isEmpty()) {
                                Player target = Bukkit.getPlayer(players.get(ThreadLocalRandom.current().nextInt(players.size())));
                                if (target != null) m.setTarget(target);
                            }
                        }
                    }
                }
            }
        }, 20L, 40L);
    }

    @Override
    public void onDisable() {
        for (DungeonInstance run : DungeonManager.getAll().values()) {
            run.end(DungeonStatus.KILLED_FORCE);
        }
    }
}
