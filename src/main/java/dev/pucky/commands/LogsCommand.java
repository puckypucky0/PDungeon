package dev.pucky.commands;

import dev.pucky.commands.util.MinecraftCommand;
import dev.pucky.commands.util.MinecraftCommandMetadata;
import dev.pucky.commands.util.tab.TabCompleteEntry;
import dev.pucky.config.ConfigManager;
import dev.pucky.util.CommandUtils;
import dev.pucky.util.MMUtil;
import dev.pucky.util.logs.LogEntry;
import dev.pucky.util.logs.Logger;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class LogsCommand implements MinecraftCommand {

    public ConfigManager configManager;

    public LogsCommand(ConfigManager configManager) {
        this.configManager = configManager;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(MMUtil.formatColorsPrefixed(
                    "<n>Komutun doğru kullanımı: /pdungeon <v>logs <n><sayfa> <v><log derecesi>"
            ));
            return;
        }

        if (CommandUtils.parseIntOrNull(args[0]) == null) {
            sender.sendMessage(MMUtil.formatColorsPrefixed(
                    "<n>Sayfa değeri <v>sayı olmalıdır."
            ));
            return;
        }

        if (!List.of("ALL", "SUCCESS", "FAIL", "FATAL", "WARN", "INFO").contains(args[1])) {
            sender.sendMessage(MMUtil.formatColorsPrefixed(
                    "<v>Geçersiz log derecesi değeri."
            ));
            return;
        }

        int page = CommandUtils.parseIntOrNull(args[0]);
        Logger.Level level = Objects.equals(args[1], "ALL") ? null : Logger.Level.valueOf(args[1]);
        String levelInfo = (level == null) ? "Bütün log kayıtları" : level.getName() + " <n>seviyesindeki log kayıtları";
        Component logComponent = MMUtil.formatColorsPrefixed(levelInfo + " gösteriliyor.")
                .appendNewline();

        List<LogEntry> logs = new ArrayList<>();
        for (LogEntry logEntry : Logger.getLogHistory()) {
            if (level == null || logEntry.level() == level)
                logs.add(logEntry);
        }

        int subListStart = (page - 1) * 10;
        int subListEnd = Math.min(subListStart + 10, logs.size());

        if (page < 1 || subListStart >= logs.size()) {
            sender.sendMessage(MMUtil.formatColorsPrefixed("<f>Bu sayfada log yok."));
            return;
        }

        List<LogEntry> logEntrySublist = logs.subList(subListStart, subListEnd);

        for (LogEntry logEntry : logEntrySublist) {
            String message = logEntry.message();

            logComponent = logComponent.append(MMUtil.formatColorsPrefixed(
                    "<v>" + new SimpleDateFormat("HH:mm:ss").format(logEntry.time()) + " <yellow><st>   </st></yellow> " + message
            )).appendNewline();
        }

        sender.sendMessage(logComponent);
    }


    @Override
    public MinecraftCommandMetadata metadata() {
        return MinecraftCommandMetadata.of(
                        "logs",
                        "Look for logs."
                )
                .permission("op")
                .tabCompletes(
                        TabCompleteEntry.of(0, "1", "2", "3", "4", "5"),
                        TabCompleteEntry.of(1, "ALL", "SUCCESS", "FAIL", "FATAL", "WARN", "INFO")
                );
    }
}
