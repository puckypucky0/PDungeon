package dev.pucky.gui;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

// menüyü kapatır, oyuncunun sohbete yazdığı ilk mesajı koda verir
public class ChatInput implements Listener {
    // sohbet async thread'de geldiği için thread-safe map
    private static final Map<UUID, Request> requests = new ConcurrentHashMap<>();
    private static String cancelWord = "iptal";

    private record Request(Consumer<String> onInput, Runnable onCancel) {}

    public static void ask(Player player, String prompt, Consumer<String> onInput) {
        ask(player, prompt, onInput, null);
    }

    // prompt MiniMessage formatında, onCancel null olabilir
    public static void ask(Player player, String prompt, Consumer<String> onInput, Runnable onCancel) {
        requests.put(player.getUniqueId(), new Request(onInput, onCancel));

        // tıklama olayının içinden çağrılabilir, kapatmayı bir tick sonraya bırak
        Bukkit.getScheduler().runTask(PuckyGui.getPlugin(), () -> {
            player.closeInventory();
            player.sendMessage(MiniMessage.miniMessage().deserialize(prompt));
        });
    }

    public static boolean isWaiting(Player player) {
        return requests.containsKey(player.getUniqueId());
    }

    public static void setCancelWord(String word) {
        cancelWord = word;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent event) {
        Request request = requests.remove(event.getPlayer().getUniqueId());
        if (request == null) return;

        // mesaj sohbete düşmesin
        event.setCancelled(true);
        String message = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();

        // Bukkit API'si ana thread'de kullanılmalı
        Bukkit.getScheduler().runTask(PuckyGui.getPlugin(), () -> {
            if (message.equalsIgnoreCase(cancelWord)) {
                if (request.onCancel() != null) request.onCancel().run();
            } else {
                request.onInput().accept(message);
            }
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        requests.remove(event.getPlayer().getUniqueId());
    }
}
