package dev.pucky.commands;

import dev.pucky.commands.util.MinecraftCommand;
import dev.pucky.commands.util.MinecraftCommandMetadata;
import dev.pucky.commands.util.tab.TabCompleteEntry;
import dev.pucky.party.Party;
import dev.pucky.party.PartyActionStatus;
import dev.pucky.party.PartyInvite;
import dev.pucky.party.PartyManager;
import dev.pucky.util.MMUtil;
import dev.pucky.util.logs.Logger;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public class PartyCommand implements MinecraftCommand {
    @Override
    public void execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;

        if (args.length == 0) {
            player.sendMessage(MMUtil.formatColorsPrefixed(
                    "<n>Doğru kullanım: <v>/pdungeon parti <white><davet / <n>dağıt <white>/ kur <white>/ <v>at<white>> <n><...>"
            ));
            return;
        }

        switch (args[0]) {
            case "davet": {
                if (args.length == 1) {
                    player.sendMessage(MMUtil.formatColorsPrefixed(
                            "<n>Doğru kullanım: <v>/pdungeon parti <n><davet> <v><oyuncu ismi>"
                    ));
                    return;
                }
                Player invitedPlayer = Bukkit.getPlayer(args[1]);

                if (invitedPlayer == null || !invitedPlayer.isOnline()) {
                    player.sendMessage(MMUtil.formatColorsPrefixed(
                            "<n>Bu oyuncu <v>aktif değil."
                    ));
                    return;
                }

                PartyActionStatus status = PartyManager.invite(player.getUniqueId(), invitedPlayer.getUniqueId());
                if (status.isSuccess()) {
                    player.sendMessage(MMUtil.formatColorsPrefixed(
                            "<s>Başarıyla <n>davet gönderildi: " + status.getMessage()
                    ));

                    invitedPlayer.sendMessage(MMUtil.formatColorsPrefixed(
                            "<v>[<n>Parti Daveti<v>] " + player.getName() + " <n>tarafından bir parti daveti aldınız. Kabul etmek için"
                    )
                            .append(MMUtil.formatColors("<v>/pdungeon parti kabul-et <s>[TIKLA]"))
                            .clickEvent(ClickEvent.runCommand("/pdungeon parti kabul-et")));
                    invitedPlayer.playSound(invitedPlayer.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 1);
                } else {
                    player.sendMessage(MMUtil.formatColorsPrefixed(
                            "<n>Davet <f>gönderilemedi: " + status.getMessage()
                    ));
                }
                break;
            }

            case "kabul-et": {
                if (args.length != 1) {
                    player.sendMessage(MMUtil.formatColorsPrefixed(
                            "<n>Doğru kullanım: <v>/pdungeon parti kabul-et"
                    ));
                    return;
                }

                PartyInvite invite = PartyManager.getInvite(player.getUniqueId());
                if (invite == null) {
                    player.sendMessage(MMUtil.formatColorsPrefixed(
                            "<n>Kabul edecek bir davetiniz <v>yok."
                    ));
                    return;
                }

                PartyActionStatus status = PartyManager.addMember(invite.getInviterPartyLeaderID(), player.getUniqueId());

                if (status.isSuccess()) {
                    player.sendMessage(MMUtil.formatColorsPrefixed(
                            "<s>Başarıyla <n>daveti kabul ettiniz: " + status.getMessage()
                    ));
                    for (UUID member : PartyManager.get(invite.getInviterPartyLeaderID()).getMembers()) {
                        Player partyMember = Bukkit.getPlayer(member);

                        if (partyMember != null) {
                            partyMember.sendMessage(MMUtil.formatColorsPrefixed(
                                    "<v>" + player.getName() + " <n>parti davetini <s>kabul etti."
                            ));
                            partyMember.playSound(partyMember.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 1);
                        }
                    }
                    PartyManager.removeInvite(player.getUniqueId());
                } else {
                    player.sendMessage(MMUtil.formatColorsPrefixed(
                            "<n>Davet bir <f>hatadan dolayı kabul edilemedi: " + status.getMessage()
                    ));
                }

                break;
            }

            case "kur": {
                if (PartyManager.get(player.getUniqueId()) != null) {
                    player.sendMessage(MMUtil.formatColorsPrefixed(
                            "<n>Zaten bir partiniz var. Bu partiden <v>ayrılmadan <n>yeni bir parti <f>oluşturamazsınız."
                    ));
                    return;
                }

                if (args.length == 1) {
                    player.sendMessage(MMUtil.formatColorsPrefixed(
                            "<n>Doğru kullanım: <v>/pdungeon kur <n><parti ismi>"
                    ));
                    return;
                }

                if (PartyManager.getByName(args[1]) != null) {
                    player.sendMessage(MMUtil.formatColorsPrefixed(
                            "<n>Bu parti ismi <v>zaten kullanılıyor."
                    ));
                    return;
                }

                String cleanName = MiniMessage.miniMessage().escapeTags(args[1]);
                PartyActionStatus status = PartyManager.create(cleanName, player.getUniqueId());

                if (status.isSuccess()) {
                    player.sendMessage(MMUtil.formatColorsPrefixed(
                            "<s>Başarıyla <v>" + cleanName + " <n>ismi ile yeni bir parti oluşturdunuz."
                    ));
                    Logger.inform("<gray>New party created: <green>" + player.getName() + " <gray>, <aqua>" + cleanName);
                } else {
                    player.sendMessage(MMUtil.formatColorsPrefixed(
                            "<n>Parti <f>oluşturulamadı: " + status.getMessage()
                    ));
                    Logger.warn("<gray>Party couldn't <red>create:" + status.getMessage());
                }
                break;
            }

            case "dağıt": {
                if (party(player) == null) {
                    player.sendMessage(MMUtil.formatColorsPrefixed(
                            "<n>Bir partiniz <v>yok."
                    ));
                    return;
                }

                if (!party(player).getLeaderID().equals(player.getUniqueId())) {
                    player.sendMessage(MMUtil.formatColorsPrefixed(
                            "<n>Yalnızca <v>liderler <n>parti dağıtabilir."
                    ));
                    return;
                }

                Set<UUID> members = party(player).getMembers();
                PartyActionStatus status = PartyManager.disband(player.getUniqueId());

                if (status.isSuccess()) {
                    player.sendMessage(MMUtil.formatColorsPrefixed(
                            "<n>Parti <s>başarıyla <n>dağıtıldı."
                    ));

                    for (UUID member : members) {
                        Player partyMember = Bukkit.getPlayer(member);
                        if (partyMember != null)
                            partyMember.sendMessage(MMUtil.formatColorsPrefixed(
                                    "<n>Partiniz <v>parti lideri tarafından dağıtıldı. <n>Artık bir partide değilsiniz."
                            ));
                    }
                }
                break;
            }

            case "at": {
                if (PartyManager.getByMember(player.getUniqueId()) == null) {
                    player.sendMessage(MMUtil.formatColorsPrefixed(
                            "<n>Bir partiniz <v>yok."
                    ));
                    return;
                }

                if (args.length == 1) {
                    player.sendMessage(MMUtil.formatColorsPrefixed(
                            "<n>Doğru kullanım: <v>/pdungeon parti <n>at <v><oyuncu_ismi>"
                    ));
                    return;
                }

                if (!PartyManager.getByMember(player.getUniqueId()).getLeaderID().equals(player.getUniqueId())) {
                    player.sendMessage(MMUtil.formatColorsPrefixed(
                            "<n>Yalnızca <v>liderler <n>partiden oyuncu atabilir."
                    ));
                    return;
                }

                if (player.getName().equalsIgnoreCase(args[1])) {
                    player.sendMessage(MMUtil.formatColorsPrefixed(
                            "<v>Kendinizi atamazsınız."
                    ));
                    return;
                }

                if (!party(player).getMembers().contains(Bukkit.getOfflinePlayer(args[1]).getUniqueId())) {
                    player.sendMessage(MMUtil.formatColorsPrefixed(
                            "<n>Partinizde böyle bir oyuncu <v>yok."
                    ));
                    return;
                }

                OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
                PartyActionStatus status = PartyManager.removeMember(player.getUniqueId(), target.getUniqueId());

                if (status.isSuccess()) {
                    player.sendMessage(MMUtil.formatColorsPrefixed(
                            "<n>Oyuncu partiden <s>başarıyla <n>atıldı."
                    ));

                    for (UUID member : party(player).getMembers()) {
                        Player partyMember = Bukkit.getPlayer(member);
                        if (partyMember != null)
                            partyMember.sendMessage(MMUtil.formatColorsPrefixed(
                                    "<v>" + target.getName() + " <n>partiden <v>atıldı."
                            ));
                    }

                    if (target.isOnline())
                        Bukkit.getPlayer(target.getUniqueId()).sendMessage(MMUtil.formatColorsPrefixed(
                                "<v>Parti lideri <n>sizi partiden attı. Artık bir partiniz <v>yok."
                        ));
                }
                break;
            }

            case "ayrıl": {
                Party party = PartyManager.getByMember(player.getUniqueId());

                if (party == null) {
                    player.sendMessage(MMUtil.formatColorsPrefixed(
                            "<n>Bir partiniz <v>yok."
                    ));
                    return;
                }

                Set<UUID> members = party.getMembers();

                if (party.getLeaderID().equals(player.getUniqueId())) {
                    PartyActionStatus status = PartyManager.disband(player.getUniqueId());

                    if (!status.isSuccess()) {
                        player.sendMessage(MMUtil.formatColorsPrefixed(
                                "<n>Partiden <f>ayrılamadınız: " + status.getMessage()
                        ));
                        return;
                    }

                    player.sendMessage(MMUtil.formatColorsPrefixed(
                            "<n>Lider olduğunuz için partiden ayrılınca parti <v>dağıtıldı."
                    ));

                    for (UUID member : members) {
                        Player partyMember = Bukkit.getPlayer(member);
                        if (partyMember != null && !member.equals(player.getUniqueId()))
                            partyMember.sendMessage(MMUtil.formatColorsPrefixed(
                                    "<v>Parti lideri <n>partiden ayrıldığı için parti dağıtıldı. Artık bir partide <v>değilsiniz."
                            ));
                    }
                    return;
                }

                PartyActionStatus status = PartyManager.removeMember(party.getLeaderID(), player.getUniqueId());

                if (!status.isSuccess()) {
                    player.sendMessage(MMUtil.formatColorsPrefixed(
                            "<n>Partiden <f>ayrılamadınız: " + status.getMessage()
                    ));
                    return;
                }

                player.sendMessage(MMUtil.formatColorsPrefixed(
                        "<n>Partiden <s>başarıyla <n>ayrıldınız."
                ));

                for (UUID member : members) {
                    Player partyMember = Bukkit.getPlayer(member);
                    if (partyMember != null)
                        partyMember.sendMessage(MMUtil.formatColorsPrefixed(
                                "<v>" + player.getName() + " <n>partiden <v>ayrıldı."
                        ));
                }
                break;
            }

            case "yeniden-adlandır": {
                if (args.length == 1) {
                    player.sendMessage(MMUtil.formatColorsPrefixed(
                            "<n>Doğru kullanım: <v>/pdungeon yeniden-adlandır <n><yeni isim>"
                    ));
                    return;
                }

                if (party(player) == null) {
                    player.sendMessage(MMUtil.formatColorsPrefixed(
                            "<n>Partinin adını yalnızca <v>liderler <n>değiştirebilir."
                    ));
                    return;
                }

                if (party(player).getPartyName().equals(args[1])) {
                    player.sendMessage(MMUtil.formatColorsPrefixed(
                            "<f>Partinin eski ismi ile yeni ismi aynı olamaz."
                    ));
                    return;
                }

                String cleanName = MiniMessage.miniMessage().escapeTags(args[1]);
                if (PartyManager.getByName(cleanName) != null) {
                    player.sendMessage(MMUtil.formatColorsPrefixed(
                            "<n>Bu parti ismi <v>zaten kullanılıyor."
                    ));
                    return;
                }
                PartyActionStatus status = PartyManager.rename(player.getUniqueId(), cleanName);

                if (status.isSuccess()) {
                    Party party = party(player);
                    player.sendMessage(MMUtil.formatColorsPrefixed(
                            "<n>Partinin adı <s>başarıyla <v>" + cleanName + " <n>olarak değiştirildi."
                    ));

                    for (UUID memberID : party.getMembers()) {
                        Player member = Bukkit.getPlayer(memberID);

                        if (member != null)
                            member.sendMessage(MMUtil.formatColorsPrefixed(
                                    "<n>Partinizin adı <v>" + cleanName + " <n>olarak değiştirildi."
                            ));
                    }
                } else {
                    player.sendMessage(MMUtil.formatColorsPrefixed(
                            "<f>Parti ismi değiştirilemedi: " + status.getMessage()
                    ));
                }
                break;
            }

        }
    }

    @Override
    public MinecraftCommandMetadata metadata() {
        return MinecraftCommandMetadata.of("parti", "Partinizi yönetin")
                .playerOnly()
                .permission("pdungeon.parti")
                .tabCompletes(
                        TabCompleteEntry.of(0, "davet", "dağıt", "kur", "at", "kabul-et", "ayrıl", "yeniden-adlandır"),
                        TabCompleteEntry.of(1, (ctx) -> {
                            if (ctx.sender() instanceof Player player) {
                                if (ctx.argAt(0).equals("davet"))
                                    return Bukkit.getOnlinePlayers().stream()
                                            .map(Player::getName)
                                            .toList();

                                if (ctx.argAt(0).equals("at"))
                                    return party(player) == null
                                    ? List.of() : party(player).getMembers().stream()
                                                  .map(uuid -> Bukkit.getOfflinePlayer(uuid).getName())
                                                  .toList();
                            }
                            return List.of();
                        })
                );

    }

    private Party party(Player player) {
        return PartyManager.get(player.getUniqueId());
    }
}
