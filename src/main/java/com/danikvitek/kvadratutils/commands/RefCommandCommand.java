package com.danikvitek.kvadratutils.commands;

import com.danikvitek.kvadratutils.Main;
import com.danikvitek.kvadratutils.utils.QueryBuilder;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashMap;
import java.util.stream.Collectors;

public class RefCommandCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (sender.hasPermission("kvadratutils.command.refcommand")) {
            if (args.length >= 2) {
                String alias = args[0];
                String commandReference = Arrays.stream(args).skip(1L).collect(Collectors.joining(" "));

                if (Main.makeExecuteUpdate(new QueryBuilder().insert(Main.refCommandsTableName)
                        .setValues("'" + alias + "'", "'" + commandReference + "'")
                        .build(),
                        new HashMap<>()))
                    sender.sendMessage(ChatColor.YELLOW + "Сокращение сохранено");
                else
                    sender.sendMessage(ChatColor.RED + "Такое сокращение уже есть");
            }
            else
                return false;
        }
        else
            sender.sendMessage(ChatColor.RED + "Нет прав на использование команды");
        return true;
    }
}
