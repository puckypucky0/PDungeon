package dev.pucky.commands;

import dev.pucky.PDungeon;
import dev.pucky.commands.util.MinecraftCommand;
import dev.pucky.commands.util.MinecraftCommandMetadata;
import dev.pucky.commands.util.tab.TabCompleteEntry;
import dev.pucky.config.ConfigManager;
import dev.pucky.dungeon.DungeonInstance;
import dev.pucky.dungeon.DungeonManager;
import dev.pucky.party.Party;
import dev.pucky.party.PartyManager;
import dev.pucky.util.CommandUtils;
import dev.pucky.util.MMUtil;
import dev.pucky.util.RequirementUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.*;

public class StartDungeonInstanceCommand implements MinecraftCommand {

    public ConfigManager configManager;

    public StartDungeonInstanceCommand(ConfigManager configManager) {
        this.configManager = configManager;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        FileConfiguration config = PDungeon.getInstance().getConfig();

        // menü konsoldan çalıştırır: /pd start <true|false> <zorluk> <oyuncu>
        Player player;
        if (args.length == 3) {
            player = Bukkit.getPlayerExact(args[2]);
            if (player == null) {
                sender.sendMessage(MMUtil.formatColorsPrefixed("<f>Oyuncu bulunamadı: <v>" + args[2]));
                return;
            }
        } else if (args.length == 2 && sender instanceof Player self) {
            player = self;
        } else {
            sender.sendMessage(MMUtil.formatColorsPrefixed("<n>Doğru kullanım: <v>/pd start <true|false> <zorluk> [oyuncu]"));
            return;
        }

        if (!List.of("true", "false").contains(args[0])) {
            error(player, 2);
            return;
        }

        if (!configManager.getDifficulties().contains(args[1].toUpperCase(Locale.ROOT))) {
            error(player, 4);
            return;
        }

        boolean isMultiPlayer = Boolean.parseBoolean(args[0]);
        ConfigManager.Difficulty difficulty = configManager.getDifficulty(args[1]);

        Component requirementsMet = RequirementUtil.checkRequirements(difficulty, player);
        if (requirementsMet != null) {
            player.sendMessage(requirementsMet);
            return;
        }

        // zorluğun arena listesinden yüklü ve boş olan ilk arena
        Integer arena = null;
        for (int id : difficulty.arenas()) {
            if (configManager.getArena(id) != null && !DungeonManager.isActive(id)) {
                arena = id;
                break;
            }
        }

        if (arena == null) {
            player.sendMessage(MMUtil.formatColorsPrefixed(
                    "<n>Bu zorluktaki tüm arenalar şuan <v>dolu. <n>Biraz sonra tekrar deneyin."
            ));
            return;
        }

        Party party = PartyManager.getByMember(player.getUniqueId());

        if (isMultiPlayer) {
            if (party == null) {
                player.sendMessage(MMUtil.formatColorsPrefixed(
                        "<f>Önce bir parti oluştur veya bir partiye gir."
                ));
                return;
            }

            if (!party.getLeaderID().equals(player.getUniqueId())) {
                player.sendMessage(MMUtil.formatColorsPrefixed(
                        "<n>Bir partiniz varken arkadaşlarınızla birlikte zindan girmek için <v>parti lideri olmanız <n>gerekiyor."
                ));

            } else if (party.getMembers().stream().anyMatch(uuid -> DungeonManager.get(uuid) != null)) {
                player.sendMessage(MMUtil.formatColorsPrefixed(
                        "<f>Partinizden biri zaten bir zindanda!"
                ));
            } else { // Requirement check
                List<String> invalidMembers = new ArrayList<>();
                for (UUID member : party.getMembers()) {
                    Player memberPlayer = Bukkit.getPlayer(member);
                    if (memberPlayer != null && RequirementUtil.checkRequirements(difficulty, memberPlayer) != null)
                        invalidMembers.add(memberPlayer.getName());
                }
                if (!invalidMembers.isEmpty()) {
                    player.sendMessage(MMUtil.formatColorsPrefixed(
                            "<f>Şu parti üyeleri bu zindan için gereksinimleri karşılamıyor: <n>" +  String.join(", ", invalidMembers)
                    ));
                } else {
                    long cooldown = DungeonManager.checkCooldownMillis(player, difficulty);
                    if (cooldown != 0) {
                        player.sendMessage(MMUtil.formatColorsPrefixed(
                                "<n>Bu zindana girmek için <v>" + CommandUtils.formatDuration(cooldown) + " <n>kadar daha beklemen gerekiyor."
                        ));
                    } else {
                        List<String> cooldownMembers = new ArrayList<>();
                        for (UUID member : party.getMembers()) {
                            Player memberPlayer = Bukkit.getPlayer(member);
                            if (memberPlayer != null && DungeonManager.checkCooldownMillis(memberPlayer, difficulty) != 0)
                                cooldownMembers.add(memberPlayer.getName());
                        }
                        if (!cooldownMembers.isEmpty()) {
                            player.sendMessage(MMUtil.formatColorsPrefixed(
                                    "<f>Şu parti üyelerinin bu zindan için bekleme süresi var: " +  String.join(", ", cooldownMembers)
                            ));
                        } else {
                            DungeonInstance.builder()
                                    .arenaConfig(configManager.getArena(arena))
                                    .players(new HashSet<>(party.getMembers()))
                                    .difficulty(difficulty)
                                    .build()
                                    .start();
                        }
                    }
                }


            }
        } else {
            if (DungeonManager.get(player.getUniqueId()) != null) {
                player.sendMessage(MMUtil.formatColorsPrefixed(
                        "<f>Zaten bir zindandasınız!"
                ));
                return;
            }

            if (DungeonManager.checkCooldownMillis(player, difficulty) != 0) {
                player.sendMessage(MMUtil.formatColorsPrefixed(
                        "<n>Bu zindana girmek için <v>" + CommandUtils.formatDuration(DungeonManager.checkCooldownMillis(player, difficulty)) + " <n>kadar daha beklemen gerekiyor."
                ));
                return;
            }

            DungeonInstance.builder()
                    .arenaConfig(configManager.getArena(arena))
                    .players(new HashSet<>(Set.of(player.getUniqueId())))
                    .difficulty(difficulty)
                    .build()
                    .start();
        }
    }

    @Override
    public MinecraftCommandMetadata metadata() {
        return MinecraftCommandMetadata.of(
                        "start",
                        "Starts a dungeon instance."
                )
                .permission("pdungeon.start")
                .tabCompletes(
                        TabCompleteEntry.of(0, "true", "false"),
                        TabCompleteEntry.of(1, ctx -> configManager.getDifficulties())
                );
    }

    private void error(Player player, int i) {
        player.sendMessage(MMUtil.formatColorsPrefixed(
                "<f>Zindana giriş yapılamadı. Bir yetkiliye başvurun. <v>[" + i + "]"
        ));
    }
}
