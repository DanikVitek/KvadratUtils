package com.danikvitek.kvadratutils.commands;

import com.danikvitek.kvadratutils.Main;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Base64;

public class SkinSelectCommand implements CommandExecutor, Listener {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (player.hasPermission("kvadratutils.command.skin_select")) {

            }
            else
                player.sendMessage(ChatColor.RED + "Нет прав на использование команды");
        }
        else
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "Команду может использовать только игрок");
        return true;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {

    }

    private void setSkin(@NotNull Player player, @NotNull String value, @NotNull String signature) {
        Main.getReflector().setSkin(player, value, signature);
    }

    private String toBase64(String s) {
        return Base64.getEncoder().encodeToString(s.getBytes());
    }

    private String fromBase64(String s) {
        return new String(Base64.getDecoder().decode(s));
    }
}
