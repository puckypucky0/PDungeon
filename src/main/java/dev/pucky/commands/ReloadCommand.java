package dev.pucky.commands;

import dev.pucky.commands.util.MinecraftCommand;
import dev.pucky.commands.util.MinecraftCommandMetadata;
import dev.pucky.config.ConfigManager;
import dev.pucky.dungeon.DungeonManager;
import dev.pucky.util.MMUtil;
import dev.pucky.util.logs.Logger;
import org.bukkit.command.CommandSender;

public class ReloadCommand implements MinecraftCommand {

    public ConfigManager configManager;

    public ReloadCommand(ConfigManager configManager) {
        this.configManager = configManager;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        try {
            configManager.load();
        } catch (Exception e) {
            sender.sendMessage(MMUtil.formatColorsPrefixed(
                    "<f>Konfigürasyon yüklenemedi: <v>" + e.getMessage() + " <n>Ayrıntılar için konsola bakın."
            ));
            Logger.fail("<gray>Config reload <red>failed: <white>" + e);
            return;
        }

        sender.sendMessage(MMUtil.formatColorsPrefixed(
                "<s>Konfigürasyon yeniden yüklendi. <v>" + configManager.getArenas().size() + " <n>arena, <v>"
                        + configManager.getDifficulties().size() + " <n>zorluk yüklendi."
        ));

        int active = DungeonManager.getAll().size();
        if (active > 0)
            sender.sendMessage(MMUtil.formatColorsPrefixed(
                    "<v>" + active + " <n>aktif zindan var. Bu zindanlar <v>mevcut dalga listeleriyle <n>devam eder; yeni mob ayarları <v>bir sonraki dalgadan <n>itibaren geçerli olur."
            ));
    }

    @Override
    public MinecraftCommandMetadata metadata() {
        return MinecraftCommandMetadata.of(
                "reload",
                "Reloads the PDungeon configuration."
        )
                .permission("pdungeon.reload");
    }
}
