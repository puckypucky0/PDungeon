package dev.pucky.dungeon;

import dev.pucky.PDungeon;
import dev.pucky.config.ConfigManager;
import dev.pucky.util.LootUtil;
import dev.pucky.util.MMUtil;
import dev.pucky.util.MobUtil;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Builder @Getter @Setter
public class DungeonInstance {
    private ConfigManager.Arena arenaConfig;
    private long startedAt;
    private Set<UUID> players;
    private int currentWave;
    private Set<UUID> aliveMobs;
    private DungeonStatus status;
    private int pendingSpawns;
    private BukkitTask spawnTask;
    private ConfigManager.Difficulty difficulty;

    public void start() {
        if (this.status == DungeonStatus.RUNNING)
            return;

        // player check
        Component component = MMUtil.formatColorsPrefixed("<n>Oyuncu kontrolü <v>tamamlandı.")
                .appendNewline();

        for (UUID player : this.players) {
            if (!Bukkit.getOfflinePlayer(player).isOnline()) {
                component = component.append(
                        MMUtil.formatColorsPrefixed("<s>• <v>" + Bukkit.getOfflinePlayer(player).getName() + " <n>[oyundan çıkmış]")
                                .appendNewline()
                );
            }
        }
        this.players.removeIf(uuid -> !Bukkit.getOfflinePlayer(uuid).isOnline());
        if (this.players.isEmpty()) return;

        if (!DungeonManager.add(this)) return;
        component = component.append(MMUtil.formatColorsPrefixed("<n>Geçersiz oyuncular listeden çıkarıldı, <v>geri kalanlar ile zindan devam edecek."));

        this.startedAt = System.currentTimeMillis();
        this.currentWave = 0;
        this.aliveMobs = new HashSet<>();
        this.status = DungeonStatus.RUNNING;

        for (UUID uuid : this.players) {
            Player player = Bukkit.getPlayer(uuid);
            player.sendMessage(component);

            player.showTitle(Title.title(
                    MMUtil.title(),
                    MMUtil.deserialize("<#7046ED>Başlıyoruz...")
            ));
            player.teleport(this.arenaConfig.playerSpawn());
            player.playSound(player.getLocation(), Sound.BLOCK_END_PORTAL_SPAWN, 1f, 0.9f);
        }

        this.nextWave();
    }
    public void nextWave() {
        ConfigManager configManager = PDungeon.getInstance().getConfigManager();

        if (this.status.isFinished()) return;
        this.currentWave++;

        this.status = DungeonStatus.RUNNING;

        ConfigManager.Wave wave = this.difficulty.waves().get(this.currentWave);
        if (wave == null) {
            this.end(DungeonStatus.ENDED);
            return;
        }

        List<ConfigManager.Mob> mobs = new ArrayList<>();

        for (String id : wave.mobs().keySet()) {
            for (int i = 0; i < wave.mobs().get(id); i++) {
                mobs.add(configManager.getMob(id));
            }
        }
        pendingSpawns = mobs.size();
        Collections.shuffle(mobs);


        for (UUID uuid : this.players) {
            Player player = Bukkit.getPlayer(uuid);

            Title.Times times = Title.Times.times(
                    Duration.of(500, ChronoUnit.MILLIS),
                    Duration.of(1500, ChronoUnit.MILLIS),
                    Duration.of(500, ChronoUnit.MILLIS)
            );

            Title title = Title.title(
                    MMUtil.title().append(
                            MMUtil.deserialize(" <reset>— <#FF3E3E>WAVE <#A70000>" + this.currentWave)
                    ),
                    MMUtil.formatColorsPrefixed("<v>" + mobs.size() + " <n>yaratık geliyor."), times
            );

            player.showTitle(title);
        }

        this.spawnTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (status.isFinished() || mobs.isEmpty())
                    cancel();

                else {
                    ConfigManager.Mob mob = mobs.get(0);
                    spawnMob(mob);
                    mobs.remove(0);
                    --pendingSpawns;

                    for (UUID player : players) {
                        if (Bukkit.getPlayer(player) != null)
                            Bukkit.getPlayer(player).sendMessage(mob.name().append(MMUtil.formatColors(
                                    " <n>geliyor, <f>kaç! <v>[<n>" + pendingSpawns + " tane kaldı<v>]"
                            )));
                    }
                }

            }
        }.runTaskTimer(PDungeon.instance, 0L, configManager.getSpawnInterval());

    }
    public void end(DungeonStatus status) {
        if (this.status.isFinished())
            return;

        this.status = status;

        for (UUID aliveMob : this.aliveMobs) {
            Entity mob = Bukkit.getEntity(aliveMob);

            if (mob != null) {
                mob.getPassengers().forEach(Entity::remove);
                mob.remove();
            }
        }

        this.aliveMobs.clear();

        long seconds = (System.currentTimeMillis() - this.startedAt) / 1000;
        String duration = String.format("%d:%02d", seconds / 60, seconds % 60);

        String endMessage = switch (this.status) {
            case ENDED -> "<s>Zindanı tamamladınız! <n>Temizlenen dalga: <v>" + (this.currentWave - 1) + " <n>| Süre: <v>" + duration;
            case ENDED_FAIL -> "<f>Zindan başarısız oldu. <n>Ulaşılan dalga: <v>" + this.currentWave + " <n>| Süre: <v>" + duration;
            default -> "<n>Zindan bitti: <v>" + this.status.getMessage();
        };

        for (UUID uuid : this.players) {
            Player player = Bukkit.getPlayer(uuid);

            if (player != null) {
                player.sendMessage(MMUtil.formatColorsPrefixed(endMessage));

                if (this.status == DungeonStatus.ENDED) {
                    player.showTitle(Title.title(
                            MMUtil.formatColorsPrefixed("<s>Zafer!"),
                            MMUtil.formatColorsPrefixed("<n>Zindanı <v>" + duration + " <n>sürede tamamladınız.")
                    ));
                    player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
                }

                player.teleport(this.arenaConfig.respawnLocation());
                if (this.status == DungeonStatus.ENDED)
                    this.giveLoot(player);
            }
        }

        if (spawnTask != null) {
            spawnTask.cancel();
            this.pendingSpawns = 0;
        }

        DungeonManager.remove(this.arenaConfig.id());
    }

    private void spawnMob(ConfigManager.Mob mob) {
        List<Location> spawns = arenaConfig.mobSpawns();
        Location loc = spawns.get(ThreadLocalRandom.current().nextInt(spawns.size()));
        LivingEntity mobEntity = (LivingEntity) loc.getWorld().spawnEntity(loc, mob.type());

        mobEntity.setGlowing(true);

        NamespacedKey key = new NamespacedKey(PDungeon.getInstance(), "mob_id");
        mobEntity.getPersistentDataContainer().set(key, PersistentDataType.STRING, mob.id());

        mobEntity.customName(mob.name()); // Name
        mobEntity.setCustomNameVisible(true);

        mobEntity.getAttribute(Attribute.MAX_HEALTH).setBaseValue(mob.health());
        mobEntity.setHealth(mob.health());

        mobEntity.getAttribute(Attribute.ATTACK_DAMAGE).setBaseValue(mob.damage());

        mobEntity.getAttribute(Attribute.SCALE).setBaseValue(mob.scale());
        AttributeInstance movementSpeed = mobEntity.getAttribute(Attribute.MOVEMENT_SPEED);
        movementSpeed.setBaseValue(movementSpeed.getBaseValue() * mob.movementSpeedMultiplier());

        mobEntity.getAttribute(Attribute.FOLLOW_RANGE).setBaseValue(100.0);

        mobEntity.setRemoveWhenFarAway(false);
        aliveMobs.add(mobEntity.getUniqueId());

        TextDisplay label = loc.getWorld().spawn(loc, TextDisplay.class, d -> {
            d.setBillboard(Display.Billboard.CENTER);
            d.setPersistent(false);
            d.setShadowed(true);
            d.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
            d.setTransformation(new Transformation(
                    new Vector3f(0, 0.3f, 0), new AxisAngle4f(),
                    new Vector3f(1, 1, 1), new AxisAngle4f()));
        });

        mobEntity.addPassenger(label);
        MobUtil.updateLabel(mobEntity);
        mobEntity.setCustomNameVisible(false);

        PDungeon.getInstance().getConfigManager().getMobSpawnParticle()
                .spawn(mobEntity.getLocation());
    }

    private void giveLoot(Player player) {
        Component itemsWon = MMUtil.formatColorsPrefixed("<s>Zindan ödülleri:").appendNewline();

        for (ConfigManager.LootEntry entry : this.difficulty.loot()) {
            ItemStack item = LootUtil.roll(entry);

            if (item != null) {
                itemsWon = itemsWon.append(MMUtil.formatColorsPrefixed("<v>● [<n>" + item.getAmount() + "<v> tane] "))
                        .append(entry.item().displayName())
                        .append(MMUtil.formatColors(" <v>%" + entry.chance() + " şansın vardı ve <s><b>tuttu!</b>"))
                        .appendNewline();

                Map<Integer, ItemStack> leftover = player.getInventory().addItem(item);
                leftover.forEach((stackIndex, stack) -> {
                    player.getWorld().dropItemNaturally(player.getLocation(), stack);
                });
            } else {
                itemsWon = itemsWon.append(MMUtil.formatColorsPrefixed("<v>● "))
                        .append(entry.item().displayName())
                        .append(MMUtil.formatColors(" <v>%" + entry.chance() + " şansın vardı ve <f><b>tutmadı.</b>"))
                        .appendNewline();
            }
        }

        player.sendMessage(itemsWon);
        player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 5f, 0.9f);
    }
}
