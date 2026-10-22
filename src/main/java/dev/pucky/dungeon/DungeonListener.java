package dev.pucky.dungeon;

import dev.pucky.PDungeon;
import dev.pucky.config.ConfigManager;
import dev.pucky.party.Party;
import dev.pucky.party.PartyManager;
import dev.pucky.util.LootUtil;
import dev.pucky.util.MMUtil;
import dev.pucky.util.MobUtil;
import dev.pucky.util.logs.Logger;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DungeonListener implements Listener {
    private final Map<UUID, Location> pendingRespawns = new HashMap<>();

    private void handleOnDungeonMobDeath(Entity entity, @Nullable Entity attackerEntity) {
        ConfigManager configManager = PDungeon.getInstance().getConfigManager();
        for (DungeonInstance instance : DungeonManager.getAll().values()) {

            if (instance.getAliveMobs().contains(entity.getUniqueId())) {
                instance.getAliveMobs().remove(entity.getUniqueId());
                Logger.inform("<gray>A dungeon mob is killed by <green>" + (attackerEntity == null ? "unknown <yellow>(probably another dungeon mob)</yellow>" : attackerEntity.getName()));

                String id = entity.getPersistentDataContainer().get(new NamespacedKey(PDungeon.getInstance(), "mob_id"), PersistentDataType.STRING);
                ConfigManager.Mob mob = id == null ? null : configManager.getMob(id);


                if (mob != null) {
                    for (ConfigManager.LootEntry lootEntry : mob.loot()) {
                        ItemStack lootItem = LootUtil.roll(lootEntry);

                        if (lootItem != null && attackerEntity instanceof Player attackerPlayer) {
                            attackerPlayer.sendMessage(MMUtil.formatColorsPrefixed("<v>%" + lootEntry.chance() + " <n>şansla ")
                                    .append(lootEntry.item().displayName())
                                    .append(MMUtil.formatColors(" <s>düşürdün!")));
                            Map<Integer, ItemStack> leftover = attackerPlayer.getInventory().addItem(lootItem);
                            leftover.forEach((stackIndex, stack) -> {
                                attackerPlayer.getWorld().dropItemNaturally(attackerPlayer.getLocation(), stack);
                            });
                            entity.getWorld().playSound(attackerPlayer, Sound.ENTITY_PLAYER_LEVELUP, 10, .8f);
                        }
                    }
                }

                if (instance.getAliveMobs().isEmpty() && instance.getPendingSpawns() == 0) {
                    instance.setStatus(DungeonStatus.WAITING);

                    int waveDelay = PDungeon.getInstance().getConfigManager().getWaveDelay();
                    boolean lastWave = instance.getCurrentWave() >= instance.getDifficulty().waves().size();
                    String waveMessage = lastWave
                            ? "<s>Son dalga temizlendi! <n>Zindan <v>" + waveDelay + " saniye <n>içinde tamamlanacak."
                            : "<s>Dalga " + instance.getCurrentWave() + " temizlendi! <n>Sonraki dalga <v>" + waveDelay + " saniye <n>içinde başlıyor.";

                    for (UUID uuid : instance.getPlayers()) {
                        Player player = Bukkit.getPlayer(uuid);
                        if (player != null) {
                            player.sendMessage(MMUtil.formatColorsPrefixed(waveMessage));
                            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.2f);
                        }
                    }
                    BukkitTask task = Bukkit.getScheduler()
                            .runTaskLater(
                                    PDungeon.getInstance(),
                                    instance::nextWave,
                                    PDungeon.getInstance().getConfigManager().getWaveDelay() * 20
                            );
                    Logger.success("<green>Successfully starting next wave on <aqua>arena " + instance.getArenaConfig().id());
                }
            }
        }
    }

    private void pushAway(Player player, Entity mob) {
        Vector direction = player.getLocation().toVector().subtract(mob.getLocation().toVector());
        if (direction.lengthSquared() == 0) return;

        player.setVelocity(direction.normalize().multiply(1.2).setY(0.4));
        player.sendActionBar(MMUtil.formatColors("<f>Bu zindan sana ait değil!"));
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 1f);
    }

    @EventHandler
    public void onDungeonMobTargetPlayer(EntityTargetLivingEntityEvent event) {
        for (DungeonInstance instance : DungeonManager.getAll().values()) {
            if (instance.getAliveMobs().contains(event.getEntity().getUniqueId())) {
                if (event.getTarget() == null)
                    return;

                if (!instance.getPlayers().contains(event.getTarget().getUniqueId()))
                    event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onDungeonPlayerUseCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();

        for (DungeonInstance instance : DungeonManager.getAll().values()) {
            if (instance.getPlayers().contains(player.getUniqueId())) {
                if (!player.isOp()) {
                    event.setCancelled(true);
                    player.sendMessage(MMUtil.formatColorsPrefixed("<f>Zindandayken komut kullanamazsın."));
                }
            }
        }
    }

    @EventHandler
    public void onDungeonEvokerSummonVex(CreatureSpawnEvent event) {
        for (DungeonInstance instance : DungeonManager.getAll().values()) {

            if (event.getEntity() instanceof Vex vex) {
                boolean check = (event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.SPELL && vex.getSummoner() != null)
                        && instance.getAliveMobs().contains(vex.getSummoner().getUniqueId());

                if (check) {
                    instance.getAliveMobs().add(vex.getUniqueId());
                    for (UUID playerID : instance.getPlayers()) {
                        Player player = Bukkit.getPlayer(playerID);

                        if (player != null)
                            player.showTitle(Title.title(
                                    MMUtil.formatColorsPrefixed("<v>Yeni mob geliyor!"),
                                    MMUtil.formatColors("<f>3 yeni VEX geliyor! <v>Toplam [" + instance.getAliveMobs().size() + "]")
                            ));
                    }
                }
            }
        }
    }
    @EventHandler
    public void onDungeonMobDamage(EntityDamageEvent event) {
        UUID mobId = event.getEntity().getUniqueId();
        Entity attackerEntity = event.getDamageSource().getCausingEntity();


        if (event.getEntity() instanceof LivingEntity livingEntity) {

            for (DungeonInstance instance : DungeonManager.getAll().values()) {
                if (instance.getAliveMobs().contains(mobId)) {
                    if (attackerEntity instanceof Player player && !instance.getPlayers().contains(player.getUniqueId())) {
                        event.setCancelled(true);

                        if (!(event.getDamageSource().getDirectEntity() instanceof Projectile))
                            pushAway(player, livingEntity);
                        else
                            player.sendActionBar(MMUtil.formatColors("<f>Bu zindan sana ait değil!"));
                        return;
                    }
                    if (event.getFinalDamage() >= livingEntity.getHealth()) {
                        event.setCancelled(true);

                        Location loc = livingEntity.getLocation().add(0, livingEntity.getHeight() / 2, 0);
                        loc.getWorld().spawnParticle(Particle.SCULK_SOUL, loc, 75, 0.1, 0.4, 0.1, 0.8);

                        livingEntity.getPassengers().forEach(Entity::remove);
                        livingEntity.remove();
                        this.handleOnDungeonMobDeath(event.getEntity(), attackerEntity);
                    } else Bukkit.getScheduler().runTask(PDungeon.getInstance(), () -> MobUtil.updateLabel(livingEntity));
                }
            }
        }
    }

    @EventHandler
    public void onDungeonPlayerDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player damagedPlayer &&
        event.getDamageSource().getCausingEntity() instanceof Player attackerPlayer) {
            Party damagedPlayerParty = PartyManager.getByMember(damagedPlayer.getUniqueId());
            Party attackerPlayerParty = PartyManager.getByMember(attackerPlayer.getUniqueId());

            boolean atLeastOneAtDungeon = (DungeonManager.get(damagedPlayer.getUniqueId()) != null)
                                            || (DungeonManager.get(attackerPlayer.getUniqueId()) != null);

            if (damagedPlayerParty == null || attackerPlayerParty == null)
                return;

            if (damagedPlayerParty.equals(attackerPlayerParty) && atLeastOneAtDungeon) {
                event.setCancelled(true);
                attackerPlayer.playSound(attackerPlayer, Sound.ENTITY_VILLAGER_NO, 10, 2);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDungeonPlayerTeleport(PlayerTeleportEvent event) {
        for (DungeonInstance instance : DungeonManager.getAll().values()) {
            if (instance.getPlayers().contains(event.getPlayer().getUniqueId())) {
                if (instance.getStatus().isFinished()) return;
                
                Location spawn = instance.getArenaConfig().playerSpawn();
                Location to = event.getTo();

                boolean leftArena = !to.getWorld().equals(spawn.getWorld())
                        || to.distanceSquared(spawn) > 50 * 50;

                if (leftArena) {
                    event.getPlayer().sendMessage(MMUtil.formatColorsPrefixed("<f>Zindandan ayrıldığın için elendin."));
                    this.removePlayerIfExists(event.getPlayer().getUniqueId()); // kalanlara "düştü" mesajı burada gider

                    if (!instance.getStatus().isFinished())
                        spawn.getWorld().playSound(spawn, Sound.BLOCK_ANVIL_PLACE, 15, .6f);
                }
            }
        }
    }

    @EventHandler
    public void onDungeonMobDeath(EntityDeathEvent event) {
        Entity killerEntity = event.getDamageSource().getCausingEntity();

        this.handleOnDungeonMobDeath(event.getEntity(), killerEntity);
    }

    @EventHandler
    public void onDungeonPlayerLeave(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        removePlayerIfExists(playerId);
    }

    @EventHandler
    public void onDungeonPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getPlayer();
        DungeonInstance instance = DungeonManager.get(player.getUniqueId());
        if (instance == null) return;

        int remaining = instance.getPlayers().size() - 1;

        if (remaining == 0) {
            player.sendMessage(MMUtil.formatColorsPrefixed(
                    "<f>Öldünüz! <n>Takımınızda kimse kalmadı, zindan <f>başarısız <n>oldu. <n>Ulaşılan dalga: <v>" + instance.getCurrentWave()
            ));
            player.showTitle(Title.title(
                    MMUtil.formatColorsPrefixed("<f>Öldünüz"),
                    MMUtil.formatColorsPrefixed("<n>Zindan <f>başarısız")
            ));
        } else {
            player.sendMessage(MMUtil.formatColorsPrefixed(
                    "<f>Öldünüz! <n>Zindandan elendiniz. Takımınız <v>" + remaining + " <n>kişi olarak devam ediyor."
            ));
            player.showTitle(Title.title(
                    MMUtil.formatColorsPrefixed("<f>Öldünüz"),
                    MMUtil.formatColorsPrefixed("<n>Zindandan <v>elendiniz")
            ));
        }

        pendingRespawns.put(player.getUniqueId(), instance.getArenaConfig().respawnLocation());
        removePlayerIfExists(player.getUniqueId());
    }

    // zindanda ölen oyuncu, arenanın respawn-location noktasında doğar
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDungeonPlayerRespawn(PlayerRespawnEvent event) {
        Location location = pendingRespawns.remove(event.getPlayer().getUniqueId());
        if (location != null)
            event.setRespawnLocation(location);
    }

    private void removePlayerIfExists(UUID playerId) {
        if (DungeonManager.get(playerId) != null)
            this.removePlayerFromInstanceIfExists(DungeonManager.get(playerId), playerId);
    }

    private void removePlayerFromInstanceIfExists(DungeonInstance instance, UUID playerId) {
        if (!instance.getPlayers().contains(playerId))
            return;

        instance.getPlayers().remove(playerId);

        if (instance.getPlayers().isEmpty()) {
            instance.end(DungeonStatus.ENDED_FAIL);
            return;
        }

        for (UUID id : instance.getPlayers()) {
            Player player = Bukkit.getPlayer(id);

            player.sendMessage(MMUtil.formatColorsPrefixed(
                    "<v>" + Bukkit.getOfflinePlayer(playerId).getName() + " <n>düştü. Artık <v>" + instance.getPlayers().size() + " <n>kişisiniz."
            ));
            player.playSound(player.getLocation(), Sound.ENTITY_ALLAY_HURT, 1f, 0f);
        }
    }
}
