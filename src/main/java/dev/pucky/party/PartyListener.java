package dev.pucky.party;

import dev.pucky.util.MMUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

public class PartyListener implements Listener {
    @EventHandler
    public void onPartyMemberDisconnect(PlayerQuitEvent event) {
        Party leaderParty = PartyManager.get(event.getPlayer().getUniqueId());
        Party memberParty = PartyManager.getByMember(event.getPlayer().getUniqueId());

        if (leaderParty != null) {
            // Player is a party leader
            for (UUID memberID : leaderParty.getMembers()) {
                Player partyMember = Bukkit.getPlayer(memberID);

                if (partyMember != null && partyMember.isOnline())
                    partyMember.sendMessage(MMUtil.formatColorsPrefixed(
                            "<n>Parti lideriniz <v>oyundan düştü ve parti dağıldı. <n>Artık bir partiniz yok."
                    ));
            }

            PartyManager.disband(event.getPlayer().getUniqueId());
        } else if (memberParty != null) {
            // Player is a member
            for (UUID memberID : memberParty.getMembers()) {
                Player partyMember = Bukkit.getPlayer(memberID);

                if (partyMember != null && partyMember.isOnline())
                    partyMember.sendMessage(MMUtil.formatColorsPrefixed(
                            "<n>Parti üyelerinden <v>" + event.getPlayer().getName() + " <n>oyundan düştü ve partiden <v>çıkarıldı."
                    ));
            }

            PartyManager.removeMember(memberParty.getLeaderID(), event.getPlayer().getUniqueId());
        }

    }
}
