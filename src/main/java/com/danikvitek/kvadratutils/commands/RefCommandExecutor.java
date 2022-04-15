package com.danikvitek.kvadratutils.commands;

import com.danikvitek.kvadratutils.Main;
import com.danikvitek.kvadratutils.utils.QueryBuilder;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class RefCommandExecutor implements TabExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (sender.hasPermission("kvadratutils.command.ref")) {
            if (args.length >= 1) {
                String commandReference = Main.makeExecuteQuery(
                        new QueryBuilder().select(Main.REF_COMMANDS_TABLE_NAME)
                                .what("Command")
                                .from()
                                .where("Alias = '" + args[0] + "'")
                                .build(),
                        new HashMap<>(),
                        commandsResultSet -> {
                            try {
                                if (commandsResultSet.next())
                                    return commandsResultSet.getString(1);
                            } catch (SQLException e) {
                                e.printStackTrace();
                            }
                            return null;
                        }
                );
                if (commandReference != null) {
                    Bukkit.dispatchCommand(sender, commandReference + " " + Arrays.stream(args).skip(1L).collect(Collectors.joining(" ")));
                }
            }
            else
                return false;
        }
        else
            sender.sendMessage(ChatColor.RED + "Нет прав на использование команды");
        return true;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (sender.hasPermission("kvadratutils.command.ref")) {
            return copyPartialInnerMatches(args[0], Objects.requireNonNull(Main.makeExecuteQuery(new QueryBuilder().select(Main.REF_COMMANDS_TABLE_NAME)
                            .what("Alias").from().build(),
                    new HashMap<>(),
                    aliasesResultSet -> {
                        ArrayList<String> tabCompleteResult = new ArrayList<>();
                        try {
                            while (aliasesResultSet.next()) {
                                tabCompleteResult.add(aliasesResultSet.getString(1));
                            }
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                        return tabCompleteResult;
                    })));
        }
        return null;
    }

    private static List<String> copyPartialInnerMatches(final String lookFor, final @NotNull Collection<String> lookIn) {
        return lookIn.stream().filter(s -> s.contains(lookFor)).collect(Collectors.toList());
    }
}
