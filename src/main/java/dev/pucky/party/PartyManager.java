package dev.pucky.party;

import dev.pucky.PDungeon;

import java.util.*;

public class PartyManager {
    public static final Map<UUID, Party> activeParties = new HashMap<>();
    public static final Map<UUID, PartyInvite> activePartyInvites = new HashMap<>();

    public static PartyActionStatus create(String partyName, UUID leaderID, UUID... members) {
        if (activeParties.containsKey(leaderID))
            return PartyActionStatus.PLAYER_ALREADY_IN_A_PARTY;

        for (Party party : activeParties.values()) {
            if (party.getMembers().contains(leaderID))
                return PartyActionStatus.PLAYER_ALREADY_IN_A_PARTY;

            for (UUID member : members) {
                if (party.getMembers().contains(member))
                    return PartyActionStatus.ONE_OR_MORE_PLAYERS_ALREADY_IN_A_PARTY;
            }
        }

        Set<UUID> membersList = new HashSet<>(Arrays.asList(members));
        membersList.add(leaderID);

        activeParties.put(
                leaderID,
                Party.builder()
                .leaderID(leaderID)
                .members(membersList)
                .partyName(partyName)
                .build()
        );

        return PartyActionStatus.PARTY_CREATED_SUCCESSFULLY;
    }

    public static PartyActionStatus disband(UUID playerID) {

        boolean inParty = false;
        for (Party party : activeParties.values()) {
            if (party.getMembers().contains(playerID)) {
                inParty = true;
                break;
            }
        }

        if (!inParty)
            return PartyActionStatus.PLAYER_NOT_IN_A_PARTY;

        if (!activeParties.containsKey(playerID))
            return PartyActionStatus.PLAYER_NOT_LEADER_TO_DISBAND;

        activeParties.remove(playerID);
        return PartyActionStatus.PARTY_DISBANDED_SUCCESSFULLY;
    }

    public static PartyActionStatus addMember(UUID leaderID, UUID playerID) {
        if (activeParties.get(leaderID) == null)
            return PartyActionStatus.PARTY_LEADER_ID_INVALID;

        for (Party party : activeParties.values()) {
            if (party.getMembers().contains(playerID))
                return PartyActionStatus.PLAYER_ALREADY_ADDED;
        }

        if (get(leaderID).getMembers().size() >=
                PDungeon.getInstance().getConfigManager().getMaxPlayers())
            return PartyActionStatus.PARTY_LIMIT_EXCEEDED;

        activeParties.get(leaderID).getMembers().add(playerID);
        return PartyActionStatus.PARTY_MEMBER_ADDED_SUCCESSFULLY;
    }

    public static PartyActionStatus removeMember(UUID leaderID, UUID playerID) {
        if (activeParties.get(leaderID) == null)
            return PartyActionStatus.PARTY_LEADER_ID_INVALID;

        if (!get(leaderID).getMembers().contains(playerID))
            return PartyActionStatus.PLAYER_ALREADY_REMOVED;

        activeParties.get(leaderID).getMembers().remove(playerID);
        return PartyActionStatus.PARTY_MEMBER_REMOVED_SUCCESSFULLY;
    }

    public static Party get(UUID leaderID) {
        return activeParties.get(leaderID);
    }

    public static Party getByMember(UUID playerID) {
        for (Party party : activeParties.values()) {
            if (party.getMembers().contains(playerID))
                return party;
        }

        return null;
    }

    public static Party getByName(String name) {
        for (Party party : activeParties.values()) {
            if (Objects.equals(party.getPartyName(), name))
                return party;
        }

        return null;
    }

    public static PartyActionStatus rename(UUID leaderID, String newName) {
        if (activeParties.get(leaderID) == null)
            return PartyActionStatus.PLAYER_NOT_LEADER_TO_RENAME;

        Party party = get(leaderID);
        party.setPartyName(newName);

        return PartyActionStatus.PARTY_RENAMED_SUCCESSFULLY;
    }

    public static PartyActionStatus invite(UUID leaderID, UUID invitedPlayerID) {
        if (activeParties.get(leaderID) == null)
            return PartyActionStatus.PLAYER_NOT_LEADER_TO_INVITE;

        if (getInvite(invitedPlayerID) != null
            && getInvite(invitedPlayerID).getInviterPartyLeaderID().equals(leaderID))
            return PartyActionStatus.PLAYER_ALREADY_INVITED;

        for (Party party : activeParties.values()) {
            if (party.getMembers().contains(invitedPlayerID))
                return PartyActionStatus.PLAYER_ALREADY_IN_A_PARTY;
        }

        if (get(leaderID).getMembers().size() >=
                PDungeon.getInstance().getConfigManager().getMaxPlayers())
            return PartyActionStatus.PARTY_LIMIT_EXCEEDED;

        activePartyInvites.put(invitedPlayerID, new PartyInvite(leaderID, System.currentTimeMillis() + 20 * 1000));

        return PartyActionStatus.PLAYER_INVITED_SUCCESSFULLY;
    }

    public static PartyInvite getInvite(UUID invitedID) {
        if (activePartyInvites.get(invitedID) == null)
            return null;

        if (System.currentTimeMillis() > activePartyInvites.get(invitedID).getExpiresAt()) {
            activePartyInvites.remove(invitedID);
            removeInvite(invitedID);
            return null;
        }

        return activePartyInvites.get(invitedID);
    }

    public static PartyActionStatus removeInvite(UUID invitedID) {
        if (!activePartyInvites.containsKey(invitedID))
            return PartyActionStatus.NO_INVITE_FOUND;

        activePartyInvites.remove(invitedID);
        return PartyActionStatus.INVITE_REMOVED_SUCCESSFULLY;
    }
}
