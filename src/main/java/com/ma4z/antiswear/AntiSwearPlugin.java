package com.ma4z.antiswear;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.text.Normalizer;
import java.util.*;
import java.util.stream.Collectors;

public class AntiSwearPlugin extends JavaPlugin implements Listener {

    private List<String> blockedWords;
    private String blockedMessage;
    private boolean notifyStaff;
    private String staffPerm;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfigValues();
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("AntiSwear enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("AntiSwear disabled.");
    }

    private void loadConfigValues() {
        FileConfiguration cfg = getConfig();
        this.blockedWords = cfg.getStringList("Blocked_Words").stream()
                .filter(s -> s != null && !s.trim().isEmpty())
                .map(s -> s.toLowerCase(Locale.ROOT))
                .collect(Collectors.toList());
        this.blockedMessage = ChatColor.translateAlternateColorCodes('&',
                cfg.getString("Blocked_Message", "&cWatch your language! Your message was blocked."));
        this.notifyStaff = cfg.getBoolean("Notify_Staff", true);
        this.staffPerm = cfg.getString("Staff_Notify_Permission", "antiswear.admin");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("antiswear.bypass")) return;

        String message = event.getMessage();
        if (containsBlockedWord(message)) {
            event.setCancelled(true);
            player.sendMessage(blockedMessage);

            if (notifyStaff && staffPerm != null && !staffPerm.isEmpty()) {
                String alert = ChatColor.RED + "[AntiSwear] " + ChatColor.GRAY + player.getName()
                        + " tried to send: " + ChatColor.WHITE + message;
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.hasPermission(staffPerm)) p.sendMessage(alert);
                }
            }
        }
    }

    private boolean containsBlockedWord(String originalMessage) {
        if (blockedWords.isEmpty()) return false;
        String normalized = normalize(originalMessage);
        for (String banned : blockedWords) {
            if (banned.isEmpty()) continue;
            if (normalized.contains(simpleNormalize(banned))) {
                return true;
            }
        }
        return false;
    }
    private String normalize(String msg) {
        String s = msg;
        s = Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");

        s = s.toLowerCase(Locale.ROOT);
        s = s
            .replace('0', 'o')
            .replace('1', 'i')
            .replace('3', 'e')
            .replace('4', 'a')
            .replace('5', 's')
            .replace('7', 't')
            .replace('@', 'a')
            .replace('$', 's')
            .replace('!', 'i')
            .replace('|', 'i');

        s = s.replaceAll("[^a-z]", "");

        s = s.replaceAll("(.)\\1{2,}", "$1$1"); 

        return s;
    }

    private String simpleNormalize(String s) {
        return s == null ? "" : s.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z]", "");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!"antiswear".equalsIgnoreCase(command.getName())) return false;

        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("antiswear.admin")) {
                sender.sendMessage(ChatColor.RED + "You don't have permission.");
                return true;
            }
            reloadConfig();
            loadConfigValues();
            sender.sendMessage(ChatColor.GREEN + "AntiSwear config reloaded.");
            return true;
        }

        sender.sendMessage(ChatColor.YELLOW + "Usage: /antiswear reload");
        return true;
    }
}
