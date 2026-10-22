package dev.pucky.party;

import lombok.Getter;

@Getter
public enum PartyActionStatus {
    PARTY_CREATED_SUCCESSFULLY(true, "<n>Parti <v>başarıyla kuruldu."),
    ONE_OR_MORE_PLAYERS_ALREADY_IN_A_PARTY(false, "<n>Oyunculardan <v>bir veya birkaçı <n>zaten bir partide."),
    PLAYER_ALREADY_IN_A_PARTY(false, "<n>Bu oyuncu <v>zaten bir partide."),

    PARTY_DISBANDED_SUCCESSFULLY(true, "<n>Parti <v>dağıtıldı."),
    PLAYER_NOT_IN_A_PARTY(false, "<n>Herhangi bir partide <v>değilsiniz."),
    PLAYER_NOT_LEADER_TO_DISBAND(false, "<n>Partiyi yalnızca <v>parti lideri <n>dağıtabilir."),

    PARTY_MEMBER_ADDED_SUCCESSFULLY(true, "<n>Oyuncu partiye <v>katıldı."),
    PLAYER_ALREADY_ADDED(false, "<n>Bu oyuncu <v>zaten bir partide."),

    PARTY_MEMBER_REMOVED_SUCCESSFULLY(true, "<n>Oyuncu partiden <v>çıkarıldı."),
    PARTY_LEADER_ID_INVALID(false, "<n>Bu parti <v>artık mevcut değil."),
    PLAYER_ALREADY_REMOVED(false, "<n>Bu oyuncu <v>partide değil."),

    PLAYER_INVITED_SUCCESSFULLY(true, "<n>Oyuncu partiye <v>davet edildi."),
    PLAYER_NOT_LEADER_TO_INVITE(false, "<n>Partiye yalnızca <v>parti lideri <n>davet gönderebilir."),
    PARTY_LIMIT_EXCEEDED(false, "<n>Parti <v>dolu."),
    NO_INVITE_FOUND(false, "<n>Bekleyen bir davetiniz <v>yok <n>veya davetin <v>süresi dolmuş."),
    INVITE_REMOVED_SUCCESSFULLY(true, "<n>Davet <v>kaldırıldı."),
    PLAYER_ALREADY_INVITED(false, "<n>Bu oyuncu <v>zaten davet edilmiş."),

    PARTY_RENAMED_SUCCESSFULLY(true, "<n>Partinin adı <v>değiştirildi."),
    PLAYER_NOT_LEADER_TO_RENAME(false, "<n>Partinin adını yalnızca <v>parti lideri <n>değiştirebilir.");

    private final boolean success;
    private final String message;

    PartyActionStatus(boolean success, String message) {
        this.success = success;
        this.message = message;
    }
}
