package com.danikvitek.kvadratutils.commands;

import com.danikvitek.kvadratutils.Main;
import com.danikvitek.kvadratutils.utils.gui.ControlButtons;
import com.danikvitek.kvadratutils.utils.gui.Button;
import com.danikvitek.kvadratutils.utils.gui.Menu;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;

public class CommandBlocksCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (player.hasPermission("kvadratutils.moderator")) {
                Inventory cbInventory = Bukkit.createInventory(null, 54, "Командные блоки");
                Menu cbMenu = new Menu(cbInventory);
                cbMenu.setButton(49, new Button(ControlButtons.QUIT.getItemStack()) {
                    @Override
                    public void onClick(Menu menu, InventoryClickEvent event) {
                        event.setCancelled(true);
                        Main.getMenuHandler().closeMenu(player);
                    }
                });
                Main.getMenuHandler().openMenu(player, cbMenu);
            }
            else
                player.sendMessage(ChatColor.RED + "Нет прав на использование команды");
        }
        else
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "Команду может использовать только игрок");
        return true;
    }
}
