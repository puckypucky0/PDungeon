package dev.pucky.commands;

import dev.pucky.commands.util.MinecraftCommand;
import dev.pucky.commands.util.MinecraftCommandMetadata;
import dev.pucky.editor.DifficultyEditor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class EditorCommand implements MinecraftCommand {
    @Override
    public void execute(CommandSender sender, String[] args) {
        DifficultyEditor.openDifficultyList((Player) sender);
    }

    @Override
    public MinecraftCommandMetadata metadata() {
        return MinecraftCommandMetadata.of("editor", "Opens the dungeon editor.")
                .permission("op")
                .playerOnly();
    }
}