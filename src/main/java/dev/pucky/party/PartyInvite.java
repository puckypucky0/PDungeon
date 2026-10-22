package dev.pucky.party;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter @Setter @Builder
public class PartyInvite {
    private UUID inviterPartyLeaderID;
    private long expiresAt;
}
