package dev.pucky.dungeon;

import lombok.Getter;

@Getter
public enum DungeonStatus {
    RUNNING("Bu zindan turu hala devam ediyor.", false),
    KILLED_FORCE("Bu zindan turu bir moderatör tarafından zorla kapatıldı.", true),
    KILLED_EXCEPTION("Bu zindan turu bir hata sonucu kapatıldı.", true),
    WAITING("Bu zindan turu beklemede, wave'ler arası bekleme veya oyuncu bekleme olasıdır.", false),
    ENDED("Bu zindan turu sona erdi. (zindanı bitirdiniz)", true),
    ENDED_FAIL("Bu zindan turu sona erdi. (herkes öldü veya oyundan çıktı)" , true);

    public final String message;
    public final boolean finished;
    DungeonStatus(String message, boolean finished) {
        this.message = message;
        this.finished = finished;
    }

}
