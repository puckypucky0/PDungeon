package dev.pucky.party;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;
import java.util.UUID;

@Getter @Setter @Builder
public class Party {
    private String partyName;
    private UUID leaderID;
    private Set<UUID> members;
}
