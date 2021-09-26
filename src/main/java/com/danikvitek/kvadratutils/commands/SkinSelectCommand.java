package com.danikvitek.kvadratutils.commands;

import com.danikvitek.kvadratutils.Main;
import com.danikvitek.kvadratutils.utils.ItemBuilder;
import com.danikvitek.kvadratutils.utils.QueryBuilder;
import com.danikvitek.kvadratutils.utils.gui.Menu;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class SkinSelectCommand implements CommandExecutor {
    private static final HashMap<UUID, Integer> pages = new HashMap<>();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (player.hasPermission("kvadratutils.command.skin_select")) {
                Inventory skinInventory = Bukkit.createInventory(null, 54, "Выбор скина");
                Menu skinMenu = new Menu(skinInventory);
                pages.put(player.getUniqueId(), 0);

                redrawMenu(player, skinMenu);
            }
            else
                player.sendMessage(ChatColor.RED + "Нет прав на использование команды");
        }
        else
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "Команду может использовать только игрок");
        return true;
    }

    private static void redrawMenu(Player player, Menu skinMenu) {

    }

    private void setSkin(@NotNull Player player, @NotNull String title) {
        Main.getReflector().setSkin(player, title);
    }

    private static List<ItemStack> getSkinIcons() {
        return Main.makeExecuteQuery(
                new QueryBuilder().select(Main.skinsTableName).what("*").from().build(),
                new HashMap<>(),
                (args, skinsResultSet) -> {
                    List<ItemStack> result = new ArrayList<>();
                    try {
                        while (skinsResultSet.next()) {
                            ItemStack skinHead = new ItemBuilder(Material.PLAYER_HEAD).setDisplayName(skinsResultSet.getString(1)).build();
                            SkullMeta meta = (SkullMeta) skinHead.getItemMeta();
                            GameProfile gameProfile = new GameProfile(UUID.randomUUID(), null);
                            gameProfile.getProperties().put("textures", new Property("textures", skinsResultSet.getString(2)));

                            Field field;
                            assert meta != null;
                            field = meta.getClass().getDeclaredField("profile");
                            field.setAccessible(true);
                            field.set(meta, gameProfile);

                            skinHead.setItemMeta(meta);
                            result.add(skinHead);
                        }
                    } catch (SQLException | NoSuchFieldException | IllegalAccessException e) {
                        e.printStackTrace();
                    }
                    return result;
                },
                null
        );
    }
}