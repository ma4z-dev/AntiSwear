package com.ma4z.antiswear;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class AntiSwearPlugin extends JavaPlugin implements Listener {

    private List<String> blockedWords;
    private Component blockedMessage;
    private boolean notifyStaff;
    private String staffPerm;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfigValues();
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("AntiSwear v" + getDescription().getVersion() + " enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("AntiSwear disabled.");
    }

    private void loadConfigValues() {
        FileConfiguration cfg = getConfig();

        blockedWords = cfg.getStringList("Blocked_Words").stream()
                .filter(s -> s != null && !s.trim().isEmpty())
                .map(s -> s.toLowerCase(Locale.ROOT))
                .collect(Collectors.toList());

        // Uses MiniMessage so users can use modern tags like <red> or legacy ones like &c automatically
        String rawMessage = cfg.getString("Blocked_Message", "<red>Watch your language! Your message was filtered.");
        blockedMessage = MiniMessage.miniMessage().deserialize(rawMessage);

        notifyStaff = cfg.getBoolean("Notify_Staff", true);
        staffPerm = cfg.getString("Staff_Notify_Permission", "antiswear.admin");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {

        Player player = event.getPlayer();

        if (player.hasPermission("antiswear.bypass")) {
            return;
        }

        String original = PlainTextComponentSerializer.plainText()
                .serialize(event.message());

        String filtered = filterMessage(original);

        if (!original.equals(filtered)) {

            event.message(Component.text(filtered));

            player.sendMessage(blockedMessage);

            if (notifyStaff) {
                Component alert = Component.text("[AntiSwear] ", NamedTextColor.RED)
                        .append(Component.text(player.getName(), NamedTextColor.GRAY))
                        .append(Component.text(" said: ", NamedTextColor.GRAY))
                        .append(Component.text(original, NamedTextColor.WHITE));

                for (Player online : Bukkit.getOnlinePlayers()) {
                    if (online.hasPermission(staffPerm)) {
                        online.sendMessage(alert);
                    }
                }
            }
        }
    }

    private String filterMessage(String message) {

        String result = message;

        for (String banned : blockedWords) {

            Pattern pattern = Pattern.compile(
                    "\\b" + Pattern.quote(banned) + "\\b",
                    Pattern.CASE_INSENSITIVE
            );

            Matcher matcher = pattern.matcher(result);

            StringBuilder sb = new StringBuilder();

            while (matcher.find()) {
                matcher.appendReplacement(sb, Matcher.quoteReplacement(
                        censorWord(matcher.group())
                ));
            }

            matcher.appendTail(sb);
            result = sb.toString();
        }

        return result;
    }

    private String censorWord(String word) {

        if (word.length() <= 2) {
            return "**";
        }

        StringBuilder sb = new StringBuilder();

        sb.append(word.charAt(0));

        for (int i = 1; i < word.length() - 1; i++) {
            sb.append('*');
        }

        sb.append(word.charAt(word.length() - 1));

        return sb.toString();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!command.getName().equalsIgnoreCase("antiswear")) {
            return false;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {

            if (!sender.hasPermission("antiswear.admin")) {
                sender.sendMessage(Component.text("You don't have permission.", NamedTextColor.RED));
                return true;
            }

            reloadConfig();
            loadConfigValues();

            sender.sendMessage(Component.text("AntiSwear config reloaded.", NamedTextColor.GREEN));
            return true;
        }

        sender.sendMessage(Component.text("Usage: /antiswear reload", NamedTextColor.YELLOW));
        return true;
    }
}
